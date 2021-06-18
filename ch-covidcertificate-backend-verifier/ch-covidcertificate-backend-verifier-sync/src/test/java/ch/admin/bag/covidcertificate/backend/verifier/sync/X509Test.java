package ch.admin.bag.covidcertificate.backend.verifier.sync;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ch.admin.bag.covidcertificate.backend.verifier.model.cert.Algorithm;
import ch.admin.bag.covidcertificate.backend.verifier.model.cert.ExtendedKeyUsage;
import ch.admin.bag.covidcertificate.backend.verifier.model.cert.db.DbCsca;
import ch.admin.bag.covidcertificate.backend.verifier.model.cert.db.DbDsc;
import ch.admin.bag.covidcertificate.backend.verifier.sync.utils.UnexpectedAlgorithmException;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
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
import org.junit.jupiter.api.Test;

class X509Test {

    private static final String RSA_DSC_PATH = "src/test/resources/rsa_dsc_ch_prod.pem";
    private static final String RSA_CSCA_PATH = "src/test/resources/rsa_csca_ch_prod.crt";

    private static final String EC_DSC_PATH = "src/test/resources/ec_dsc_li.pem";
    private static final String EC_CSCA_PATH = "src/test/resources/ec_csca_li.pem";

    private static final String DSC_USE_V_PATH = "src/test/resources/dsc_use_v.cert";

    private static final String P256 = "P-256";
    private static final String USE_SIG = "sig";

    @Test
    void testX509Rsa() throws Exception {
        X509Certificate dscX509 = getX509(RSA_DSC_PATH);
        X509Certificate cscaX509 = getX509(RSA_CSCA_PATH);

        assertIssuerPrincipalName(dscX509, cscaX509);
        verifyDscSignature(dscX509, cscaX509);

        DbDsc dsc = mapDsc(dscX509, "CH", "kid_0");
        DbCsca csca = mapCsca(cscaX509, "CH", "kid_1");
    }

    @Test
    void testX509Mapping() throws Exception {
        X509Certificate dscX509 = getX509(EC_DSC_PATH);
        X509Certificate cscaX509 = getX509(EC_CSCA_PATH);

        assertIssuerPrincipalName(dscX509, cscaX509);
        verifyDscSignature(dscX509, cscaX509);

        DbDsc dsc = mapDsc(dscX509, "LI", "kid_0");
        DbCsca csca = mapCsca(cscaX509, "LI", "kid_1");
    }

    @Test
    void testX509ToBase64() throws Exception {
        X509Certificate original = getX509(RSA_DSC_PATH);
        String base64EncodedX509 = getBase64EncodedStr(original);
        assertEquals(original, fromBase64EncodedStr(base64EncodedX509));
    }

    @Test
    void testExtendedKeyUsage() throws Exception {
        // contains vaccinations
        X509Certificate useVDsc = getX509(DSC_USE_V_PATH);
        assertEquals("v", getUse(useVDsc.getExtendedKeyUsage()));
    }

    private void assertIssuerPrincipalName(X509Certificate dsc, X509Certificate csca) {
        // check that issuer principal name of dsc matches subject principal name of csca
        assertEquals(
                csca.getSubjectX500Principal().getName(), dsc.getIssuerX500Principal().getName());
    }

    private void verifyDscSignature(X509Certificate dsc, X509Certificate csca)
            throws CertificateException, NoSuchAlgorithmException, SignatureException,
                    InvalidKeyException, NoSuchProviderException {
        // verify dsc with csca public key
        dsc.verify(csca.getPublicKey());
    }

    private DbCsca mapCsca(X509Certificate cscaX509, String origin, String kid)
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

    private DbDsc mapDsc(X509Certificate dscX509, String origin, String kid)
            throws CertificateEncodingException, CertificateParsingException,
                    UnexpectedAlgorithmException, CertificateNotYetValidException,
                    CertificateExpiredException {
        dscX509.checkValidity();
        var dsc = new DbDsc();
        dsc.setKeyId(kid);
        dsc.setCertificateRaw(getBase64EncodedStr(dscX509));
        dsc.setOrigin(origin);
        dsc.setUse(getUse(dscX509.getExtendedKeyUsage()));
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
        // convert them to uncompressed point form
        byte[] xArray = ((ECPublicKey) x509.getPublicKey()).getW().getAffineX().toByteArray();
        return normalizeEcCurvePoint(xArray);
    }

    private String getY(X509Certificate x509) {
        // convert them to uncompressed point form
        byte[] yArray = ((ECPublicKey) x509.getPublicKey()).getW().getAffineY().toByteArray();
        return normalizeEcCurvePoint(yArray);
    }

    private String normalizeEcCurvePoint(byte[] array) {
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

    private String getSubjectPublicKeyInfo(X509Certificate dscX509) {
        return Base64.getEncoder().encodeToString(dscX509.getPublicKey().getEncoded());
    }

    private X509Certificate getX509(String pathToFile) throws IOException, CertificateException {
        try (InputStream is = new FileInputStream(pathToFile)) {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            return (X509Certificate) cf.generateCertificate(is);
        }
    }
}
