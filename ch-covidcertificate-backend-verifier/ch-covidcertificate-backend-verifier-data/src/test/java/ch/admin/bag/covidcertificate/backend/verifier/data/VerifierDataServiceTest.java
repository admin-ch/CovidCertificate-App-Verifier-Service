package ch.admin.bag.covidcertificate.backend.verifier.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.admin.bag.covidcertificate.backend.verifier.model.cert.db.DbCsca;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

class VerifierDataServiceTest extends BaseDataServiceTest {

    @Autowired VerifierDataService verifierDataService;

    @Test
    @Transactional
    void insertCscas() {
        verifierDataService.insertCscas(Collections.emptyList());
        assertTrue(verifierDataService.findCscas("CH").isEmpty());
        var dbCsca = getDefaultCSCA(0, "CH");
        verifierDataService.insertCscas(Collections.singletonList(dbCsca));
        final var certList = verifierDataService.findCscas("CH");
        assertEquals(1, certList.size());
        assertEquals("keyid_0", certList.get(0).getKeyId());
        assertNotNull(certList.get(0).getImportedAt());
    }

    @Test
    @Transactional
    void removeCscasNotIn() {
        assertTrue(verifierDataService.findCscas("CH").isEmpty());
        verifierDataService.removeCscasNotIn(Collections.singletonList("keyid_0"));
        verifierDataService.insertCscas(Collections.singletonList(getDefaultCSCA(0, "CH")));
        verifierDataService.insertCscas(Collections.singletonList(getDefaultCSCA(1, "CH")));
        verifierDataService.removeCscasNotIn(Collections.singletonList("keyid_0"));
        assertEquals(1, verifierDataService.findCscas("CH").size());
    }

    @Test
    @Transactional
    void findCscas() {
        verifierDataService.insertCscas(Collections.singletonList(getDefaultCSCA(0, "CH")));
        verifierDataService.insertCscas(Collections.singletonList(getDefaultCSCA(1, "DE")));
        assertEquals(1, verifierDataService.findCscas("CH").size());
    }

    private DbCsca getDefaultCSCA(int idSuffix, String origin) {
        var dbCsca = new DbCsca();
        dbCsca.setKeyId("keyid_" + idSuffix);
        dbCsca.setCertificateRaw("cert");
        dbCsca.setOrigin(origin);
        dbCsca.setSubjectPrincipalName("admin_ch");
        return dbCsca;
    }
}
