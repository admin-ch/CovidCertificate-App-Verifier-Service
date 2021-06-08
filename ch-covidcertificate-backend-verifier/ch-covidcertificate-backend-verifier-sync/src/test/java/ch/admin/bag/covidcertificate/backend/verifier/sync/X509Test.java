package ch.admin.bag.covidcertificate.backend.verifier.sync;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ch.admin.bag.covidcertificate.backend.verifier.model.cert.Algorithm;
import ch.admin.bag.covidcertificate.backend.verifier.model.cert.ExtendedKeyUsage;
import ch.admin.bag.covidcertificate.backend.verifier.model.cert.db.DbCsca;
import ch.admin.bag.covidcertificate.backend.verifier.model.cert.db.DbDsc;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.AlgorithmParameters;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.InvalidParameterSpecException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.Test;

public class X509Test {

    private static final String RSA_DSC_PATH = "src/test/resources/rsa_dsc_ch_prod.pem";
    private static final String RSA_CSCA_PATH = "src/test/resources/rsa_csca_ch_prod.crt";

    private static final String EC_DSC_PATH = "src/test/resources/ec_dsc_li.pem";
    private static final String EC_CSCA_PATH = "src/test/resources/ec_csca_li.pem";

    private static final String DSC_USE_V_PATH = "src/test/resources/dsc_use_v.cert";

    @Test
    public void testX509Rsa() throws Exception {
        X509Certificate dscX509 = getX509(RSA_DSC_PATH);
        X509Certificate cscaX509 = getX509(RSA_CSCA_PATH);

        assertIssuerPrincipalName(dscX509, cscaX509);
        verifyDscSignature(dscX509, cscaX509);

        DbDsc dsc = mapDsc(dscX509, "CH");
        DbCsca csca = mapCsca(cscaX509, "CH");
    }

    @Test
    public void testX509Mapping() throws Exception {
        X509Certificate dscX509 = getX509(EC_DSC_PATH);
        X509Certificate cscaX509 = getX509(EC_CSCA_PATH);

        assertIssuerPrincipalName(dscX509, cscaX509);
        verifyDscSignature(dscX509, cscaX509);

        DbDsc dsc = mapDsc(dscX509, "LI");
        DbCsca csca = mapCsca(cscaX509, "LI");
    }

    private DbCsca mapCsca(X509Certificate cscaX509, String origin)
            throws CertificateEncodingException, NoSuchAlgorithmException {
        DbCsca csca = new DbCsca();
        csca.setKeyId(createKeyId(cscaX509));
        csca.setCertificateRaw(getBase64EncodedStr(cscaX509));
        csca.setImportedAt(Instant.now());
        csca.setOrigin(origin);
        csca.setSubjectPrincipalName(cscaX509.getSubjectX500Principal().getName());
        return csca;
    }

    private static final String SECP256R1 = "secp256r1";
    private static final String P256 = "P-256";

    private DbDsc mapDsc(X509Certificate dscX509, String origin)
            throws CertificateEncodingException, CertificateParsingException,
                    NoSuchAlgorithmException, InvalidParameterSpecException {
        DbDsc dsc = new DbDsc();
        dsc.setKeyId(createKeyId(dscX509));
        dsc.setCertificateRaw(getBase64EncodedStr(dscX509));
        dsc.setImportedAt(Instant.now());
        dsc.setOrigin(origin);
        dsc.setUse(getUse(dscX509.getExtendedKeyUsage()));

        Algorithm algorithm = Algorithm.forSigAlgName(dscX509.getSigAlgName());
        dsc.setAlg(algorithm);

        switch (algorithm) {
            case ES256:
                ECParameterSpec expectedEcParameterSpec = ecParameterSpecForCurve(SECP256R1);
                assertEquals(
                        expectedEcParameterSpec,
                        ((ECPublicKey) dscX509.getPublicKey()).getParams());
                dsc.setCrv(P256);
                dsc.setX(getX(dscX509));
                dsc.setY(getY(dscX509));
                break;
            case RS256:
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

    private String getSubjectPublicKeyInfo(X509Certificate dscX509) {
        return Base64.getEncoder().encodeToString(dscX509.getPublicKey().getEncoded());
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

    @Test
    public void testX509ToBase64() throws Exception {
        X509Certificate original = getX509(RSA_DSC_PATH);
        String base64EncodedX509 = getBase64EncodedStr(original);
        assertEquals(original, base64EncodedStrToX509(base64EncodedX509));
    }

    @Test
    public void testExtendedKeyUsage() throws Exception {
        // contains vaccinations
        X509Certificate useVDsc = getX509(DSC_USE_V_PATH);
        assertEquals("v", getUse(useVDsc.getExtendedKeyUsage()));
    }

    private static final String USE_SIG = "sig";

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

    private String getBase64EncodedStr(X509Certificate x509) throws CertificateEncodingException {
        return Base64.getEncoder().encodeToString(x509.getEncoded());
    }

    private X509Certificate base64EncodedStrToX509(String base64) throws CertificateException {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        return (X509Certificate)
                cf.generateCertificate(
                        new ByteArrayInputStream(Base64.getDecoder().decode(base64)));
    }

    private X509Certificate getX509(String pathToFile) throws IOException, CertificateException {
        try (InputStream is = new FileInputStream(pathToFile)) {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            return (X509Certificate) cf.generateCertificate(is);
        }
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

    private ECParameterSpec ecParameterSpecForCurve(String curveName)
            throws NoSuchAlgorithmException, InvalidParameterSpecException {
        AlgorithmParameters params = AlgorithmParameters.getInstance("EC");
        params.init(new ECGenParameterSpec(curveName));
        return params.getParameterSpec(ECParameterSpec.class);
    }
}
