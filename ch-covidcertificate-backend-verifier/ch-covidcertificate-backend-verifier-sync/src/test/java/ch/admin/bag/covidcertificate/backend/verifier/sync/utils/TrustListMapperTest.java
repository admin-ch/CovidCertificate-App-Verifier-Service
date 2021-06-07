package ch.admin.bag.covidcertificate.backend.verifier.sync.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ch.admin.bag.covidcertificate.backend.verifier.model.cert.db.DbCsca;
import ch.admin.bag.covidcertificate.backend.verifier.model.cert.db.DbDsc;
import ch.admin.bag.covidcertificate.backend.verifier.model.sync.TrustList;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import org.junit.jupiter.api.Test;

class TrustListMapperTest {

    private final String TEST_JSON = "src/test/resources/covidcert-verifier_test_vectors.json";
    private final TrustListMapper trustListMapper = new TrustListMapper();


    @Test
    void mapCsca() throws Exception {
        final var expected = Files.readString(Path.of(TEST_JSON));
        final TrustList[] trustLists =
            new ObjectMapper().readValue(expected, TrustList[].class);
        assertEquals(1, trustLists.length);
        final DbCsca dbCsca = trustListMapper.mapCsca(trustLists[0]);
        assertEquals("ynSje/i0tac=", dbCsca.getKeyId());
    }

    @Test
    void mapDsc() throws Exception {
        final var expected = Files.readString(Path.of(TEST_JSON));
        final TrustList[] trustLists =
            new ObjectMapper().readValue(expected, TrustList[].class);
        assertEquals(1, trustLists.length);
        final DbDsc dbDsc = trustListMapper.mapDsc(trustLists[0]);
        assertEquals("ynSje/i0tac=", dbDsc.getKeyId());
    }
}