package ch.admin.bag.covidcertificate.backend.verifier.sync.utils;

import ch.admin.bag.covidcertificate.backend.verifier.model.cert.Algorithm;
import ch.admin.bag.covidcertificate.backend.verifier.model.cert.ExtendedKeyUsage;
import ch.admin.bag.covidcertificate.backend.verifier.model.cert.db.DbCsca;
import ch.admin.bag.covidcertificate.backend.verifier.model.cert.db.DbDsc;
import ch.admin.bag.covidcertificate.backend.verifier.model.sync.TrustList;
import ch.admin.bag.covidcertificate.backend.verifier.sync.syncer.DGCSyncer;
import java.io.ByteArrayInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TrustListMapper {

    private static final Logger logger = LoggerFactory.getLogger(TrustListMapper.class);

    private static final String P256 = "P-256";
    private static final String USE_SIG = "sig";

    public DbCsca mapCsca(TrustList trustList)
            throws CertificateException, NoSuchAlgorithmException {
        return mapCsca(fromBase64EncodedStr(trustList.getRawData()), trustList.getCountry());
    }

    private DbCsca mapCsca(X509Certificate cscaX509, String origin)
            throws CertificateEncodingException, NoSuchAlgorithmException {
        DbCsca csca = new DbCsca();
        csca.setKeyId(createKeyId(cscaX509));
        csca.setCertificateRaw(getBase64EncodedStr(cscaX509));
        csca.setOrigin(origin);
        csca.setSubjectPrincipalName(cscaX509.getSubjectX500Principal().getName());
        return csca;
    }

    public DbDsc mapDsc(TrustList trustList) throws CertificateException, NoSuchAlgorithmException {
        return mapDsc(fromBase64EncodedStr(trustList.getRawData()), trustList.getCountry());
    }

    private DbDsc mapDsc(X509Certificate dscX509, String origin)
            throws CertificateEncodingException, CertificateParsingException,
                    NoSuchAlgorithmException {
        var dsc = new DbDsc();
        dsc.setKeyId(createKeyId(dscX509));
        dsc.setCertificateRaw(getBase64EncodedStr(dscX509));
        dsc.setOrigin(origin);
        dsc.setUse(getUse(dscX509.getExtendedKeyUsage()));

        final String sigAlgName = dscX509.getSigAlgName();
        logger.debug("Alg name: {}", sigAlgName);
        var algorithm = Algorithm.forSigAlgName(sigAlgName);
        dsc.setAlg(algorithm);

        switch (algorithm) {
            case ES256:
                dsc.setCrv(P256);
                dsc.setX(getX(dscX509));
                dsc.setY(getY(dscX509));
                break;
            case RS256:
            case PS256:
                // TODO: Does this work for PS256?
                dsc.setN(getN(dscX509));
                dsc.setE(getE(dscX509));
                dsc.setSubjectPublicKeyInfo(getSubjectPublicKeyInfo(dscX509));
                break;
            default:
                throw new RuntimeException("unexpected algorithm: " + algorithm);
        }
        return dsc;
    }

    private String createKeyId(X509Certificate x509)
            throws NoSuchAlgorithmException, CertificateEncodingException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(x509.getEncoded());
        return Base64.getEncoder().encodeToString(Arrays.copyOfRange(hash, 0, 8));
    }

    private String getBase64EncodedStr(X509Certificate x509) throws CertificateEncodingException {
        return Base64.getEncoder().encodeToString(x509.getEncoded());
    }

    public X509Certificate fromBase64EncodedStr(String base64) throws CertificateException {
        var cf = CertificateFactory.getInstance("X.509");
        return (X509Certificate)
                cf.generateCertificate(
                        new ByteArrayInputStream(Base64.getDecoder().decode(base64)));
    }

    private String getUse(List<String> extendedKeyUsage) {
        String use = "";
        if (extendedKeyUsage != null) {
            for (String oid : extendedKeyUsage) {
                ExtendedKeyUsage usage = ExtendedKeyUsage.forOid(oid);
                if (usage != null) {
                    use += usage.getCode();
                }
            }
        }
        return !use.isBlank() ? use : USE_SIG;
    }

    private String getN(X509Certificate x509) {
        return Base64.getEncoder()
                .encodeToString(((RSAPublicKey) x509.getPublicKey()).getModulus().toByteArray());
    }

    private String getE(X509Certificate x509) {
        return Base64.getEncoder()
                .encodeToString(
                        ((RSAPublicKey) x509.getPublicKey()).getPublicExponent().toByteArray());
    }

    private String getX(X509Certificate x509) {
        return Base64.getEncoder()
                .encodeToString(
                        ((ECPublicKey) x509.getPublicKey()).getW().getAffineX().toByteArray());
    }

    private String getY(X509Certificate x509) {
        return Base64.getEncoder()
                .encodeToString(
                        ((ECPublicKey) x509.getPublicKey()).getW().getAffineY().toByteArray());
    }

    private String getSubjectPublicKeyInfo(X509Certificate dscX509) {
        return Base64.getEncoder().encodeToString(dscX509.getPublicKey().getEncoded());
    }
}
