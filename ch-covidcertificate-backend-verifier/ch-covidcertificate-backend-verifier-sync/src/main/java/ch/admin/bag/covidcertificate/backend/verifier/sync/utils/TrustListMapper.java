package ch.admin.bag.covidcertificate.backend.verifier.sync.utils;

import ch.admin.bag.covidcertificate.backend.verifier.model.cert.Algorithm;
import ch.admin.bag.covidcertificate.backend.verifier.model.cert.ExtendedKeyUsage;
import ch.admin.bag.covidcertificate.backend.verifier.model.cert.db.DbCsca;
import ch.admin.bag.covidcertificate.backend.verifier.model.cert.db.DbDsc;
import ch.admin.bag.covidcertificate.backend.verifier.model.sync.TrustList;
import java.io.ByteArrayInputStream;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TrustListMapper {

    private static final Logger logger = LoggerFactory.getLogger(TrustListMapper.class);

    private static final String P256 = "P-256";
    private static final String USE_SIG = "sig";

    /**
     * Map a TrustList as returned by the DGC gateway to a DbCsca object. Note that an exception is
     * thrown if the certificate is no longer valid.
     *
     * @throws CertificateException if the certificate's encoding was invalid or its validity has
     *     expired
     */
    public static DbCsca mapCsca(TrustList trustList) throws CertificateException {
        return mapCsca(
                fromBase64EncodedStr(trustList.getRawData()),
                trustList.getCountry(),
                trustList.getKid());
    }

    private static DbCsca mapCsca(X509Certificate cscaX509, String origin, String kid)
            throws CertificateEncodingException, CertificateNotYetValidException,
                    CertificateExpiredException {
        cscaX509.checkValidity();
        var csca = new DbCsca();
        csca.setKeyId(kid);
        csca.setCertificateRaw(getBase64EncodedStr(cscaX509));
        csca.setOrigin(origin);
        csca.setSubjectPrincipalName(cscaX509.getSubjectX500Principal().getName());
        return csca;
    }

    /**
     * Map a TrustList as returned by the DGC gateway to a DbDsc object. Note that an exception is
     * thrown if the certificate is no longer valid.
     *
     * @throws CertificateException if the certificate's encoding was invalid or its validity has
     *     expired
     * @throws UnexpectedAlgorithmException if the public key's signing algorithm isn't EC or RSA
     */
    public static DbDsc mapDsc(TrustList trustList)
            throws CertificateException, UnexpectedAlgorithmException {
        return mapDsc(
                fromBase64EncodedStr(trustList.getRawData()),
                trustList.getCountry(),
                trustList.getKid());
    }

    private static DbDsc mapDsc(X509Certificate dscX509, String origin, String kid)
            throws CertificateEncodingException, CertificateParsingException,
                    UnexpectedAlgorithmException, CertificateNotYetValidException,
                    CertificateExpiredException {
        dscX509.checkValidity();
        var dsc = new DbDsc();
        dsc.setKeyId(kid);
        dsc.setCertificateRaw(getBase64EncodedStr(dscX509));
        dsc.setOrigin(origin);
        dsc.setUse(getUse(dscX509.getExtendedKeyUsage()));

        logger.debug(
                "Reading parameters for DSC {} of origin {} with public key algorithm {}",
                kid,
                origin,
                dscX509.getPublicKey().getAlgorithm());
        var keyType = dscX509.getPublicKey().getAlgorithm();
        var algorithm = Algorithm.forPubKeyType(keyType);
        dsc.setAlg(algorithm);

        switch (algorithm) {
            case ES256:
                dsc.setCrv(P256);
                dsc.setX(getX(dscX509));
                dsc.setY(getY(dscX509));
                break;
            case RS256:
                dsc.setN(getN(dscX509));
                dsc.setE(getE(dscX509));
                dsc.setSubjectPublicKeyInfo(getSubjectPublicKeyInfo(dscX509));
                break;
            case UNSUPPORTED:
            default:
                throw new UnexpectedAlgorithmException(keyType);
        }
        return dsc;
    }

    private static String getBase64EncodedStr(X509Certificate x509) throws CertificateEncodingException {
        return Base64.getEncoder().encodeToString(x509.getEncoded());
    }

    public static X509Certificate fromBase64EncodedStr(String base64) throws CertificateException {
        var cf = CertificateFactory.getInstance("X.509");
        return (X509Certificate)
                cf.generateCertificate(
                        new ByteArrayInputStream(Base64.getDecoder().decode(base64)));
    }

    private static String getUse(List<String> extendedKeyUsage) {
        var strBldr = new StringBuilder();
        if (extendedKeyUsage != null) {
            for (String oid : extendedKeyUsage) {
                var usage = ExtendedKeyUsage.forOid(oid);
                if (usage != null) {
                    strBldr.append(usage.getCode());
                }
            }
        }
        var use = strBldr.toString();
        return !use.isBlank() ? use : USE_SIG;
    }

    private static String getN(X509Certificate x509) {
        return Base64.getEncoder()
                .encodeToString(((RSAPublicKey) x509.getPublicKey()).getModulus().toByteArray());
    }

    private static String getE(X509Certificate x509) {
        return Base64.getEncoder()
                .encodeToString(
                        ((RSAPublicKey) x509.getPublicKey()).getPublicExponent().toByteArray());
    }

    private static String getX(X509Certificate x509) {
        // convert them to uncompressed point form
        byte[] xArray = ((ECPublicKey) x509.getPublicKey()).getW().getAffineX().toByteArray();
        return normalizeEcCurvePoint(xArray);
    }

    private static String getY(X509Certificate x509) {
        // convert them to uncompressed point form
        byte[] yArray = ((ECPublicKey) x509.getPublicKey()).getW().getAffineY().toByteArray();
        return normalizeEcCurvePoint(yArray);
    }

    private static String normalizeEcCurvePoint(byte[] array) {
        // normalize ec curve point to always be 32 bytes (we always have positive sign, so the
        // leading 00 can be omitted)
        int byteArrayLength = 32;
        byte[] unsignedArr = new byte[byteArrayLength];
        if (array.length == 33) {
            System.arraycopy(array, 1, unsignedArr, 0, byteArrayLength);
        } else {
            System.arraycopy(array, 0, unsignedArr, 0, byteArrayLength);
        }

        return Base64.getEncoder().encodeToString(unsignedArr);
    }

    private static String getSubjectPublicKeyInfo(X509Certificate dscX509) {
        return Base64.getEncoder().encodeToString(dscX509.getPublicKey().getEncoded());
    }
}
