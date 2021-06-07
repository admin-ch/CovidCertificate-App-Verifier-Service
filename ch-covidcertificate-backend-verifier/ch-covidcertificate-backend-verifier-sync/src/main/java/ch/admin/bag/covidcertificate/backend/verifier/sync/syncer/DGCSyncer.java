package ch.admin.bag.covidcertificate.backend.verifier.sync.syncer;

import ch.admin.bag.covidcertificate.backend.verifier.data.VerifierDataService;
import ch.admin.bag.covidcertificate.backend.verifier.model.cert.db.DbCsca;
import ch.admin.bag.covidcertificate.backend.verifier.model.cert.db.DbDsc;
import ch.admin.bag.covidcertificate.backend.verifier.model.sync.CertificateType;
import ch.admin.bag.covidcertificate.backend.verifier.model.sync.TrustList;
import ch.admin.bag.covidcertificate.backend.verifier.sync.utils.TrustListMapper;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.time.Instant;
import java.util.ArrayList;
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

        // Download and insert CSCA certificates
        final var cscaTrustLists = dgcClient.download(CertificateType.CSCA);
        final var dbCscaList = new ArrayList<DbCsca>();
        for (TrustList cscaTrustList : cscaTrustLists) {
            try {
                final var dbCsca = trustListMapper.mapCsca(cscaTrustList);
                dbCscaList.add(dbCsca);
            } catch (CertificateException | NoSuchAlgorithmException e) {
                logger.error("Couldn't map csca trustlist to X509 certificate");
            }
        }
        verifierDataService.insertCscas(dbCscaList);

        // Download and insert DSC certificates
        final var dscTrustLists = dgcClient.download(CertificateType.DSC);
        final var dbDscList = new ArrayList<DbDsc>();
        for (TrustList dscTrustList: dscTrustLists) {
            try {
                final var dbDsc = trustListMapper.mapDsc(dscTrustList);
                // Verify signature for corresponding csca
                if(isValid(dbDsc, dscTrustList.getSignature())) {
                    dbDscList.add(dbDsc);
                } else {
                    logger.error("Signature for kid {} was invalid", dbDsc.getKeyId());
                }
            } catch(CertificateException | NoSuchAlgorithmException e) {
                logger.error("Couldn't map dsc trustlist to X509 certificate");
            }
        }
        verifierDataService.insertDsc(dbDscList);
        var end = Instant.now();
        logger.info("Finished download in {} ms", end.toEpochMilli() - start.toEpochMilli());
    }

    private boolean isValid(DbDsc dbDsc, String signature) {
        // TODO: Implement
        return true;
    }

    private void upload() {
        // TODO: Implement
    }
}
