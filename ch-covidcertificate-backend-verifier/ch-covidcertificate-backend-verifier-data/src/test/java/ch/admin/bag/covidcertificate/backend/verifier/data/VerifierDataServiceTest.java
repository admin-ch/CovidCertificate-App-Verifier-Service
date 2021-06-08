package ch.admin.bag.covidcertificate.backend.verifier.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.admin.bag.covidcertificate.backend.verifier.model.cert.Algorithm;
import ch.admin.bag.covidcertificate.backend.verifier.model.cert.CertFormat;
import ch.admin.bag.covidcertificate.backend.verifier.model.cert.db.DbCsca;
import ch.admin.bag.covidcertificate.backend.verifier.model.cert.db.DbDsc;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

class VerifierDataServiceTest extends BaseDataServiceTest {

    @Autowired VerifierDataService verifierDataService;

    @Test
    @Transactional
    void insertCscasTest() {
        verifierDataService.insertCscas(Collections.emptyList());
        assertTrue(verifierDataService.findCscas("CH").isEmpty());
        var dbCsca = getDefaultCSCA(0, "CH");
        verifierDataService.insertCscas(Collections.singletonList(dbCsca));
        final var certList = verifierDataService.findCscas("CH");
        assertEquals(1, certList.size());
        assertEquals(dbCsca.getKeyId(), certList.get(0).getKeyId());
        assertNotNull(certList.get(0).getImportedAt());
    }

    @Test
    @Transactional
    void removeCscasNotInTest() {
        assertTrue(verifierDataService.findCscas("CH").isEmpty());
        verifierDataService.removeCscasNotIn(Collections.emptyList());
        verifierDataService.removeCscasNotIn(Collections.singletonList("keyid_0"));
        verifierDataService.insertCscas(Collections.singletonList(getDefaultCSCA(0, "CH")));
        verifierDataService.insertCscas(Collections.singletonList(getDefaultCSCA(1, "CH")));
        verifierDataService.removeCscasNotIn(Collections.singletonList("keyid_0"));
        assertEquals(1, verifierDataService.findCscas("CH").size());
    }

    @Test
    @Transactional
    void findCscaTest() {
        verifierDataService.insertCscas(List.of(getDefaultCSCA(0, "CH"), getDefaultCSCA(1, "DE")));
        assertEquals(1, verifierDataService.findCscas("CH").size());
    }

    @Test
    @Transactional
    void insertDscTest() {
        verifierDataService.insertCscas(Collections.singletonList(getDefaultCSCA(0, "CH")));
        final var cscaId = verifierDataService.findCscas("CH").get(0).getId();
        verifierDataService.insertDsc(Collections.emptyList());
        assertTrue(verifierDataService.findActiveDscKeyIds().isEmpty());
        final var rsaDsc = getRSADsc(0, "CH", cscaId);
        verifierDataService.insertDsc(Collections.singletonList(rsaDsc));
        assertEquals(1, verifierDataService.findActiveDscKeyIds().size());
        assertEquals(
                rsaDsc.getKeyId(),
                verifierDataService.findDscs(0L, CertFormat.ANDROID).get(0).getKeyId());
    }

    @Test
    @Transactional
    void removeDscsNotInTest() {
        verifierDataService.insertCscas(Collections.singletonList(getDefaultCSCA(0, "CH")));
        final var cscaId = verifierDataService.findCscas("CH").get(0).getId();
        verifierDataService.removeDscsNotIn(Collections.emptyList());
        verifierDataService.removeDscsNotIn(Collections.singletonList("keyid_0"));
        final var rsaDsc = getRSADsc(0, "CH", cscaId);
        final var ecDsc = getECDsc(1, "CH", cscaId);
        verifierDataService.insertDsc(List.of(rsaDsc, ecDsc));
        verifierDataService.removeDscsNotIn(Collections.singletonList(rsaDsc.getKeyId()));
        assertEquals(1, verifierDataService.findActiveDscKeyIds().size());
    }

    @Test
    @Transactional
    void findDscsTest() {
        verifierDataService.insertCscas(Collections.singletonList(getDefaultCSCA(0, "CH")));
        final var cscaId = verifierDataService.findCscas("CH").get(0).getId();
        verifierDataService.insertDsc(List.of(getRSADsc(0, "CH", cscaId), getECDsc(1, "DE", cscaId)));
        assertEquals(2, verifierDataService.findDscs(0L, CertFormat.IOS).size());
    }

    @Test
    @Transactional
    void findMaxDscsTest() {
        verifierDataService.insertCscas(Collections.singletonList(getDefaultCSCA(0, "CH")));
        final var cscaId = verifierDataService.findCscas("CH").get(0).getId();
        verifierDataService.insertDsc(List.of(getRSADsc(0, "CH", cscaId), getECDsc(1, "DE", cscaId)));
        final var maxDscPkId = verifierDataService.findMaxDscPkId();
        assertTrue(verifierDataService.findDscs(maxDscPkId, CertFormat.IOS).isEmpty());
        assertEquals(1, verifierDataService.findDscs(maxDscPkId-1, CertFormat.IOS).size());
    }

    private DbCsca getDefaultCSCA(int idSuffix, String origin) {
        var dbCsca = new DbCsca();
        dbCsca.setKeyId("keyid_" + idSuffix);
        dbCsca.setCertificateRaw("cert");
        dbCsca.setOrigin(origin);
        dbCsca.setSubjectPrincipalName("admin_ch");
        return dbCsca;
    }

    private DbDsc getRSADsc(int idSuffix, String origin, long fkCsca) {
        final var dbDsc = new DbDsc();
        dbDsc.setKeyId("keyid_" + idSuffix);
        dbDsc.setFkCsca(fkCsca);
        dbDsc.setCertificateRaw("cert");
        dbDsc.setOrigin(origin);
        dbDsc.setUse("DSC");
        dbDsc.setAlg(Algorithm.RS256);
        dbDsc.setN("n");
        dbDsc.setE("e");
        dbDsc.setSubjectPublicKeyInfo("pk");
        return dbDsc;
    }

    private DbDsc getECDsc(int idSuffix, String origin, long fkCsca) {
        final var dbDsc = new DbDsc();
        dbDsc.setKeyId("keyid_" + idSuffix);
        dbDsc.setFkCsca(fkCsca);
        dbDsc.setCertificateRaw("cert");
        dbDsc.setOrigin(origin);
        dbDsc.setUse("DSC");
        dbDsc.setAlg(Algorithm.ES256);
        dbDsc.setCrv("crv");
        dbDsc.setX("x");
        dbDsc.setY("y");
        return dbDsc;
    }
}
