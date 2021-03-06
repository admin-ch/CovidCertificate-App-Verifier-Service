/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.backend.verifier.sync.syncer;

import ch.admin.bag.covidcertificate.backend.verifier.data.VerifierDataService;
import ch.admin.bag.covidcertificate.backend.verifier.model.cert.db.DbCsca;
import ch.admin.bag.covidcertificate.backend.verifier.model.cert.db.DbDsc;
import ch.admin.bag.covidcertificate.backend.verifier.model.sync.CertificateType;
import ch.admin.bag.covidcertificate.backend.verifier.model.sync.TrustList;
import ch.admin.bag.covidcertificate.backend.verifier.model.exception.DgcSyncException;
import ch.admin.bag.covidcertificate.backend.verifier.sync.utils.TrustListMapper;
import ch.admin.bag.covidcertificate.backend.verifier.sync.utils.UnexpectedAlgorithmException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

public class DgcCertSyncer {

    private static final Logger logger = LoggerFactory.getLogger(DgcCertSyncer.class);

    private final DgcCertClient dgcClient;
    private final VerifierDataService verifierDataService;

    public DgcCertSyncer(DgcCertClient dgcClient, VerifierDataService verifierDataService) {
        this.dgcClient = dgcClient;
        this.verifierDataService = verifierDataService;
    }

    @Transactional(rollbackFor = {DgcSyncException.class, Throwable.class})
    public void sync() throws DgcSyncException {
        logger.info("Start sync with DGC Gateway");
        var start = Instant.now();
        download();
        var end = Instant.now();
        logger.info("Finished sync in {} ms", end.toEpochMilli() - start.toEpochMilli());
    }

    private void download() throws DgcSyncException {
        logger.info("Downloading certificates from DGC Gateway");
        var start = Instant.now();
        downloadCscas();
        downloadDscs();
        var end = Instant.now();
        logger.info("Finished download in {} ms", end.toEpochMilli() - start.toEpochMilli());
    }

    private void downloadCscas() throws DgcSyncException {
        // Check which CSCAs are currently stored in the db
        final var activeCscaKeyIds = verifierDataService.findActiveCscaKeyIds();
        // Download CSCAs and check validity
        final var cscaTrustLists = dgcClient.download(CertificateType.CSCA);
        if (cscaTrustLists.length == 0) {
            throw new DgcSyncException(new Exception("CSCA List is Empty"));
        }
        final var dbCscaList = new ArrayList<DbCsca>();
        final var cscaListToInsert = new ArrayList<DbCsca>();
        for (TrustList cscaTrustList : cscaTrustLists) {
            try {
                final var dbCsca = TrustListMapper.mapCsca(cscaTrustList);
                dbCscaList.add(dbCsca);
                // Only insert CSCA if it isn't already in the db
                if (!activeCscaKeyIds.contains(dbCsca.getKeyId())) {
                    cscaListToInsert.add(dbCsca);
                }
            } catch (CertificateNotYetValidException e) {
                logger.info(
                        "Dropping CSCA trustlist {} of origin {}: Certificate not yet valid",
                        cscaTrustList.getKid(),
                        cscaTrustList.getCountry());
            } catch (CertificateExpiredException e) {
                logger.info(
                        "Dropping CSCA trustlist {} of origin {}: Certificate expired",
                        cscaTrustList.getKid(),
                        cscaTrustList.getCountry());
            } catch (CertificateException e) {
                logger.info(
                        "Dropping CSCA trustlist {} of origin {}: Couldn't map to X509 certificate",
                        cscaTrustList.getKid(),
                        cscaTrustList.getCountry());
                // we abort here, since something is off. The Certificate could not be matched
                throw new DgcSyncException(e);
            }
        }
        // Remove DSCs whose CSCA is about to be removed
        List<String> cscaKeyIdsToKeep =
                dbCscaList.stream().map(DbCsca::getKeyId).collect(Collectors.toList());
        final var removedDscCount = verifierDataService.removeDscsWithCscaNotIn(cscaKeyIdsToKeep);
        // Remove CSCAs that weren't returned by the download
        final var removedCscaCount = verifierDataService.removeCscasNotIn(cscaKeyIdsToKeep);
        // Insert CSCAs
        verifierDataService.insertCscas(cscaListToInsert);
        logger.info(
                "Downloaded {} CSCA certificates: Dropped {}, Inserted {}, Removed {} CSCAs and {} DSCs, Left {} in DB",
                cscaTrustLists.length,
                cscaTrustLists.length - dbCscaList.size(),
                cscaListToInsert.size(),
                removedCscaCount,
                removedDscCount,
                activeCscaKeyIds.size() - removedCscaCount + cscaListToInsert.size());
    }

    private void downloadDscs() throws DgcSyncException {
        // Check which DSCs are currently stored in the db
        final var activeDscKeyIds = verifierDataService.findActiveDscKeyIds();
        // Download and insert DSC certificates
        final var dscTrustLists = dgcClient.download(CertificateType.DSC);
        if (dscTrustLists.length == 0) {
            throw new DgcSyncException(new Exception("DSC List was empty"));
        }
        final var dbDscList = new ArrayList<DbDsc>();
        final var dscListToInsert = new ArrayList<DbDsc>();
        for (TrustList dscTrustList : dscTrustLists) {
            try {
                final var dbDsc = TrustListMapper.mapDsc(dscTrustList);
                // Verify signature
                if (verify(dbDsc)) {
                    dbDscList.add(dbDsc);
                    // Only insert DSC if it isn't already in the db
                    if (!activeDscKeyIds.contains(dbDsc.getKeyId())) {
                        dscListToInsert.add(dbDsc);
                    }
                } else {
                    logger.info(
                            "Dropping DSC trustlist {} of origin {}: Couldn't verify signature",
                            dscTrustList.getKid(),
                            dscTrustList.getCountry());
                }
            } catch (CertificateNotYetValidException e) {
                logger.info(
                        "Dropping DSC trustlist {} of origin {}: Certificate not yet valid",
                        dscTrustList.getKid(),
                        dscTrustList.getCountry());
            } catch (CertificateExpiredException e) {
                logger.info(
                        "Dropping DSC trustlist {} of origin {}: Certificate expired",
                        dscTrustList.getKid(),
                        dscTrustList.getCountry());
            } catch (CertificateException e) {
                logger.info(
                        "Dropping DSC trustlist {} of origin {}: Couldn't map to X509 certificate",
                        dscTrustList.getKid(),
                        dscTrustList.getCountry());
                // The certificate could not be mapped, let's bail
                throw new DgcSyncException(e);
            } catch (UnexpectedAlgorithmException e) {
                logger.info(
                        "Dropping DSC trustlist {} of origin {}",
                        dscTrustList.getKid(),
                        dscTrustList.getCountry(),
                        e);
                // If the algorithm is not known, we should bail!
                throw new DgcSyncException(e);
            }
        }
        // Remove DSCs that weren't returned by the download
        final var removedDscCount =
                verifierDataService.removeDscsNotIn(
                        dbDscList.stream().map(DbDsc::getKeyId).collect(Collectors.toList()));
        // Insert DSCs
        verifierDataService.insertDscs(dscListToInsert);
        logger.info(
                "Downloaded {} DSC certificates: Dropped {}, Inserted {}, Removed {}, Left {} in DB",
                dscTrustLists.length,
                dscTrustLists.length - dbDscList.size(),
                dscListToInsert.size(),
                removedDscCount,
                activeDscKeyIds.size() - removedDscCount + dscListToInsert.size());
    }

    private boolean verify(DbDsc dbDsc) {
        logger.debug("Verifying signature of DSC with kid {}", dbDsc.getKeyId());
        final var cscas = verifierDataService.findCscas(dbDsc.getOrigin());
        for (DbCsca dbCsca : cscas) {
            try {
                final var dscX509 = TrustListMapper.fromBase64EncodedStr(dbDsc.getCertificateRaw());
                final var cscaX509 =
                        TrustListMapper.fromBase64EncodedStr(dbCsca.getCertificateRaw());
                dscX509.verify(cscaX509.getPublicKey());
                logger.debug(
                        "Successfully verified DSC {} with CSCA {}",
                        dbDsc.getKeyId(),
                        dbCsca.getKeyId());
                dbDsc.setFkCsca(dbCsca.getId());
                return true;
            } catch (CertificateException
                    | NoSuchAlgorithmException
                    | NoSuchProviderException
                    | InvalidKeyException
                    | SignatureException e) {
                logger.debug(
                        "Couldn't verify DSC {} signature with CSCA {}",
                        dbDsc.getKeyId(),
                        dbCsca.getKeyId());
            }
        }
        return false;
    }
}
