package ch.admin.bag.covidcertificate.backend.verifier.sync.syncer;

import ch.admin.bag.covidcertificate.backend.verifier.data.VerifierDataService;
import ch.admin.bag.covidcertificate.backend.verifier.model.cert.db.DbCsca;
import ch.admin.bag.covidcertificate.backend.verifier.model.cert.db.DbDsc;
import ch.admin.bag.covidcertificate.backend.verifier.model.sync.CertificateType;
import ch.admin.bag.covidcertificate.backend.verifier.model.sync.TrustList;
import ch.admin.bag.covidcertificate.backend.verifier.sync.utils.TrustListMapper;
import ch.admin.bag.covidcertificate.backend.verifier.sync.utils.UnexpectedAlgorithmException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DGCSyncer {

    private static final Logger logger = LoggerFactory.getLogger(DGCSyncer.class);

    private final DGCClient dgcClient;
    private final VerifierDataService verifierDataService;
    private final TrustListMapper trustListMapper = new TrustListMapper();

    public DGCSyncer(DGCClient dgcClient, VerifierDataService verifierDataService) {
        this.dgcClient = dgcClient;
        this.verifierDataService = verifierDataService;
    }

    public void sync() {
        logger.info("Start sync with DGC Gateway");
        var start = Instant.now();
        download();
        upload();
        var end = Instant.now();
        logger.info("Finished sync in {} ms", end.toEpochMilli() - start.toEpochMilli());
    }

    private void download() {
        logger.info("Downloading certificates from DGC Gateway");
        var start = Instant.now();
        downloadCSCAs();
        downloadDSCs();
        var end = Instant.now();
        logger.info("Finished download in {} ms", end.toEpochMilli() - start.toEpochMilli());
    }

    private void downloadCSCAs() {
        // Check which CSCAs are currently stored in the db
        final var activeCscaKeyIds = verifierDataService.findActiveCscaKeyIds();
        // Download CSCAs and check validity
        final var cscaTrustLists = dgcClient.download(CertificateType.CSCA);
        final var dbCscaList = new ArrayList<DbCsca>();
        final var cscaListToInsert = new ArrayList<DbCsca>();
        for (TrustList cscaTrustList : cscaTrustLists) {
            try {
                final var dbCsca = trustListMapper.mapCsca(cscaTrustList);
                dbCscaList.add(dbCsca);
                // Only insert CSCA if it isn't already in the db
                if (!activeCscaKeyIds.contains(dbCsca.getKeyId())) {
                    cscaListToInsert.add(dbCsca);
                }
            } catch (CertificateException e) {
                logger.debug(
                        "Couldn't map CSCA trustlist {} of origin {} to X509 certificate",
                        cscaTrustList.getKid(),
                        cscaTrustList.getCountry());
            }
        }
        // Remove DSCs whose CSCA is about to be removed
        final var removedCscaList = new ArrayList<>(activeCscaKeyIds);
        dbCscaList.stream().map(DbCsca::getKeyId).forEach(removedCscaList::remove);
        final var removedDSCs = verifierDataService.removeDscsWithCSCAIn(removedCscaList);
        // Remove CSCAs that weren't returned by the download
        final var removedCSCAs = verifierDataService.removeCSCAs(removedCscaList);
        // Insert CSCAs
        verifierDataService.insertCscas(cscaListToInsert);
        logger.info(
                "Downloaded {} CSCA certificates: Dropped {}, Inserted {}, Removed {} CSCAs and {} DSCs, Left {} in DB",
                cscaTrustLists.length,
                cscaTrustLists.length - dbCscaList.size(),
                cscaListToInsert.size(),
                removedCSCAs,
                removedDSCs,
                dbCscaList.size() - removedCscaList.size() - cscaListToInsert.size());
    }

    private void downloadDSCs() {
        // Check which DSCs are currently stored in the db
        final var activeDscKeyIds = verifierDataService.findActiveDscKeyIds();
        // Download and insert DSC certificates
        final var dscTrustLists = dgcClient.download(CertificateType.DSC);
        final var dbDscList = new ArrayList<DbDsc>();
        final var dscListToInsert = new ArrayList<DbDsc>();
        for (TrustList dscTrustList : dscTrustLists) {
            try {
                final var dbDsc = trustListMapper.mapDsc(dscTrustList);
                // Verify signature
                if (verify(dbDsc)) {
                    dbDscList.add(dbDsc);
                    // Only insert DSC if it isn't already in the db
                    if (!activeDscKeyIds.contains(dbDsc.getKeyId())) {
                        dscListToInsert.add(dbDsc);
                    }
                }
            } catch (CertificateException e) {
                logger.debug(
                        "Couldn't map DSC trustlist {} of origin {} to X509 certificate",
                        dscTrustList.getKid(),
                        dscTrustList.getCountry());
            } catch (UnexpectedAlgorithmException e) {
                logger.error(e.getMessage());
            }
        }
        // Remove DSCs that weren't returned by the download
        final var removedDSCs =
                verifierDataService.removeDscsNotIn(
                        dbDscList.stream().map(DbDsc::getKeyId).collect(Collectors.toList()));
        // Insert DSCs
        verifierDataService.insertDsc(dscListToInsert);
        logger.info(
                "Downloaded {} DSC certificates: Dropped {}, Inserted {}, Removed {}, Left {} in DB",
                dscTrustLists.length,
                dscTrustLists.length - dbDscList.size(),
                dscListToInsert.size(),
                removedDSCs,
                dbDscList.size() - removedDSCs - dscListToInsert.size());
    }

    private boolean verify(DbDsc dbDsc) {
        logger.debug("Verifying signature of DSC with kid {}", dbDsc.getKeyId());
        final var cscas = verifierDataService.findCscas(dbDsc.getOrigin());
        for (DbCsca dbCsca : cscas) {
            try {
                final var dscX509 = trustListMapper.fromBase64EncodedStr(dbDsc.getCertificateRaw());
                final var cscaX509 =
                        trustListMapper.fromBase64EncodedStr(dbCsca.getCertificateRaw());
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

    private void upload() {
        // TODO: Implement
    }
}
