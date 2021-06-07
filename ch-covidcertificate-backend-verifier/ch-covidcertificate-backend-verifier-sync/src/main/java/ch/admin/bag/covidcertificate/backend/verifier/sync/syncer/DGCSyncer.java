package ch.admin.bag.covidcertificate.backend.verifier.sync.syncer;

import ch.admin.bag.covidcertificate.backend.verifier.data.VerifierDataService;
import ch.admin.bag.covidcertificate.backend.verifier.model.sync.CertificateType;
import ch.admin.bag.covidcertificate.backend.verifier.model.sync.TrustList;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DGCSyncer {

    private static final Logger logger = LoggerFactory.getLogger(DGCSyncer.class);

    private final DGCClient dgcClient;
    private final VerifierDataService verifierDataService;

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
        final var cscaTrustLists = dgcClient.download(CertificateType.CSCA);
        final var dscTrustLists = dgcClient.download(CertificateType.DSC);
        // TODO: Map to DbCsca & DbDsc --> Insert into DB (verify signature?)
        var end = Instant.now();
        logger.info("Finished download in {} ms", end.toEpochMilli() - start.toEpochMilli());
    }

    private void upload() {
        // TODO: Implement
    }
}
