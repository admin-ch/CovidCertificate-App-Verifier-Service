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

import ch.admin.bag.covidcertificate.backend.verifier.data.CertUploadDataService;
import ch.admin.bag.covidcertificate.backend.verifier.data.VerifierDataService;
import ch.admin.bag.covidcertificate.backend.verifier.model.cert.CertToUpload;
import ch.admin.bag.covidcertificate.backend.verifier.model.cert.db.DbDsc;
import ch.admin.bag.covidcertificate.backend.verifier.model.exception.AlreadyUploadedException;
import ch.admin.bag.covidcertificate.backend.verifier.sync.utils.CmsUtil;
import ch.admin.bag.covidcertificate.backend.verifier.sync.utils.UnexpectedAlgorithmException;
import ch.admin.bag.covidcertificate.backend.verifier.sync.ws.model.DscUploadResponse;
import ch.admin.bag.covidcertificate.backend.verifier.sync.ws.model.DscUploadResult;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.operator.OperatorCreationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DscUploadClient {

    private static final Logger logger = LoggerFactory.getLogger(DscUploadClient.class);

    private final SigningClient signingClient;
    private final DgcCertClient dgcCertClient;
    private final CertUploadDataService certUploadDataService;
    private final VerifierDataService verifierDataService;

    public DscUploadClient(
            SigningClient signingClient,
            DgcCertClient dgcCertClient,
            CertUploadDataService certUploadDataService,
            VerifierDataService verifierDataService) {
        this.signingClient = signingClient;
        this.dgcCertClient = dgcCertClient;
        this.certUploadDataService = certUploadDataService;
        this.verifierDataService = verifierDataService;
    }

    public DscUploadResponse uploadDscs()
            throws CMSException, CertificateException, IOException, OperatorCreationException,
                    UnexpectedAlgorithmException, NoSuchAlgorithmException {
        Instant start = Instant.now();
        DscUploadResponse response = new DscUploadResponse();

        List<CertToUpload> certsToUpload = certUploadDataService.findCertsToUpload();
        logger.info(
                "found {} dscs to upload. aliases: {}",
                certsToUpload.size(),
                certsToUpload.stream().map(CertToUpload::getAlias).collect(Collectors.toList()));
        for (CertToUpload certToUpload : certsToUpload) {
            String alias = certToUpload.getAlias();
            DscUploadResult uploadResult = new DscUploadResult(alias);
            response.addResult(uploadResult);

            String cms = null;
            try {
                logger.info("downloading cms for alias {} from signing service", alias);
                cms = signingClient.getCmsForAlias(alias);
            } catch (Exception e) {
                String error =
                        String.format(
                                "failed to download cms for alias %s from signing service", alias);
                uploadResult.addError(error);
                logger.error(error, e);
                continue;
            }

            DbDsc dsc = CmsUtil.decodeDscCms(cms);
            dsc.setFkCsca(verifierDataService.findChCscaPkId());
            String kid = dsc.getKeyId();
            uploadResult.setKid(kid);
            certToUpload.setKeyId(kid);

            if (certToUpload.doUpload()) {
                if (!certToUpload.wasUploaded()) {
                    try {
                        logger.info("uploading cms for alias {} to dgc hub", alias);
                        dgcCertClient.upload(cms, kid);
                        certToUpload.setUploadedAt(Instant.now());
                    } catch (AlreadyUploadedException e) {
                        uploadResult.addError(
                                String.format(
                                        "dgc reported that dsc with kid %s has already been uploaded",
                                        kid));
                    } catch (Exception e) {
                        String error =
                                String.format(
                                        "failed to upload cms for alias %s to dgc hub", alias);
                        uploadResult.addError(error);
                        logger.error(error, e);
                    }
                } else {
                    String info = String.format("cms for alias %s already uploaded", alias);
                    uploadResult.addInfo(info);
                    logger.info(info);
                }
            } else {
                String info = String.format("cms for alias %s not marked for dgc upload", alias);
                uploadResult.addInfo(info);
                logger.info(info);
            }

            if (certToUpload.doInsert()) {
                if (!certToUpload.wasInserted()) {
                    try {
                        logger.info("inserting dsc with kid {} into db", kid);
                        verifierDataService.insertManualDsc(dsc);
                        certToUpload.setInsertedAt(Instant.now());
                    } catch (Exception e) {
                        String error = String.format("failed to insert dsc with kid %s to db", kid);
                        uploadResult.addError(error);
                        logger.error(error, e);
                    }
                } else {
                    String info = String.format("cms for alias %s already in db", alias);
                    uploadResult.addInfo(info);
                    logger.info(info);
                }
            } else {
                String info = String.format("cms for alias %s not marked for insertion", alias);
                uploadResult.addInfo(info);
                logger.info(info);
            }

            // update keyId, uploadedAt and insertedAt
            certUploadDataService.updateCertToUpload(certToUpload);
        }
        logger.info(
                "uploaded {} dscs in {}ms",
                certsToUpload.size(),
                Instant.now().toEpochMilli() - start.toEpochMilli());
        return response;
    }
}
