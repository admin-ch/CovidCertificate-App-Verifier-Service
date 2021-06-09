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
                logger.error(
                        "Couldn't map CSCA trustlist {} to X509 certificate",
                        cscaTrustList.getKid());
            }
        }
        verifierDataService.insertCscas(dbCscaList);

        // Download and insert DSC certificates
        final var dscTrustLists = dgcClient.download(CertificateType.DSC);
        final var dbDscList = new ArrayList<DbDsc>();
        for (TrustList dscTrustList : dscTrustLists) {
            try {
                final var dbDsc = trustListMapper.mapDsc(dscTrustList);
                // Verify signature for corresponding csca
                if (isValid(dbDsc)) {
                    dbDscList.add(dbDsc);
                }
            } catch (CertificateException e) {
                logger.error("Couldn't map DSC trustlist to X509 certificate");
            } catch (UnexpectedAlgorithmException e) {
                logger.error(e.getMessage());
            }
        }
        verifierDataService.insertDsc(dbDscList);
        var end = Instant.now();
        logger.info("Finished download in {} ms", end.toEpochMilli() - start.toEpochMilli());
    }

    private boolean isValid(DbDsc dbDsc) {
        logger.debug("Verifying signature of DSC with kid {}", dbDsc.getKeyId());
        final var dbCscaList = verifierDataService.findCscas(dbDsc.getOrigin());
        for (DbCsca dbCsca : dbCscaList) {
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
                logger.error("Couldn't verify DSC {} signature", dbDsc.getKeyId());
            }
        }
        return false;
    }

    private void upload() {
        // TODO: Implement
    }
}
