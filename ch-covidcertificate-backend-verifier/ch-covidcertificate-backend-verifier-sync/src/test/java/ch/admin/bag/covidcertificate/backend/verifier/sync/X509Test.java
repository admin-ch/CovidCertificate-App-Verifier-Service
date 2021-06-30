package ch.admin.bag.covidcertificate.backend.verifier.sync;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import ch.admin.bag.covidcertificate.backend.verifier.model.cert.db.DbCsca;
import ch.admin.bag.covidcertificate.backend.verifier.model.cert.db.DbDsc;
import ch.admin.bag.covidcertificate.backend.verifier.sync.utils.TrustListMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class X509Test {

    private static final String RSA_DSC_PATH = "src/test/resources/rsa_dsc_ch_prod.pem";
    private static final String RSA_CSCA_PATH = "src/test/resources/rsa_csca_ch_prod.pem";

    private static final String EC_DSC_PATH = "src/test/resources/ec_dsc_li.pem";
    private static final String EC_CSCA_PATH = "src/test/resources/ec_csca_li.pem";

    private static final String DSC_USE_V_PATH = "src/test/resources/dsc_use_v.pem";

    @Test
    void testX509Rsa() throws Exception {
        X509Certificate dscX509 = getX509(RSA_DSC_PATH);
        X509Certificate cscaX509 = getX509(RSA_CSCA_PATH);

        assertIssuerPrincipalName(dscX509, cscaX509);
        assertDoesNotThrow(() -> verifyDscSignature(dscX509, cscaX509));

        DbDsc dsc = TrustListMapper.mapDsc(dscX509, "CH", "kid_0");
        assertEquals("kid_0", dsc.getKeyId());
        DbCsca csca = TrustListMapper.mapCsca(cscaX509, "CH", "kid_1");
        assertEquals("kid_1", csca.getKeyId());
    }

    @Test
    void testX509EC() throws Exception {
        X509Certificate dscX509 = getX509(EC_DSC_PATH);
        X509Certificate cscaX509 = getX509(EC_CSCA_PATH);

        assertIssuerPrincipalName(dscX509, cscaX509);
        assertDoesNotThrow(() -> verifyDscSignature(dscX509, cscaX509));

        DbDsc dsc = TrustListMapper.mapDsc(dscX509, "LI", "kid_0");
        assertEquals("kid_0", dsc.getKeyId());
        DbCsca csca = TrustListMapper.mapCsca(cscaX509, "LI", "kid_1");
        assertEquals("kid_1", csca.getKeyId());
    }

    @Test
    void testX509ToBase64() throws Exception {
        X509Certificate original = getX509(RSA_DSC_PATH);
        String base64EncodedX509 = TrustListMapper.getBase64EncodedStr(original);
        assertEquals(original, TrustListMapper.fromBase64EncodedStr(base64EncodedX509));
    }

    @Test
    void testExtendedKeyUsage() throws Exception {
        // contains vaccinations
        X509Certificate useVDsc = getX509(DSC_USE_V_PATH);
        assertEquals("v", TrustListMapper.getUse(useVDsc.getExtendedKeyUsage()));
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

    private X509Certificate getX509(String pathToFile) throws IOException, CertificateException {
        final var base64 =
                Base64.getEncoder()
                        .encodeToString(
                                Files.readString(Path.of(pathToFile))
                                        .getBytes(StandardCharsets.UTF_8));
        return TrustListMapper.fromBase64EncodedStr(base64);
    }
}
