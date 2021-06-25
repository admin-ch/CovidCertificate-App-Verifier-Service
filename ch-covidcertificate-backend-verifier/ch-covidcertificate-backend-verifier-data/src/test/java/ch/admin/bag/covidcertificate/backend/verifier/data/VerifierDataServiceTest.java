package ch.admin.bag.covidcertificate.backend.verifier.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
        verifierDataService.insertCSCAs(Collections.emptyList());
        assertTrue(verifierDataService.findCSCAs("CH").isEmpty());
        var dbCsca = getDefaultCSCA(0, "CH");
        verifierDataService.insertCSCAs(Collections.singletonList(dbCsca));
        final var certList = verifierDataService.findCSCAs("CH");
        assertEquals(1, certList.size());
        assertEquals(dbCsca.getKeyId(), certList.get(0).getKeyId());
        assertNotNull(certList.get(0).getImportedAt());
    }

    @Test
    @Transactional
    void removeCscasNotInTest() {
        assertTrue(verifierDataService.findCSCAs("CH").isEmpty());
        verifierDataService.removeCSCAs(Collections.emptyList());
        verifierDataService.removeCSCAs(Collections.singletonList("keyid_0"));
        verifierDataService.insertCSCAs(Collections.singletonList(getDefaultCSCA(0, "CH")));
        verifierDataService.insertCSCAs(Collections.singletonList(getDefaultCSCA(1, "CH")));
        verifierDataService.removeCSCAs(Collections.singletonList("keyid_0"));
        assertEquals(1, verifierDataService.findCSCAs("CH").size());
    }

    @Test
    @Transactional
    void findCscaTest() {
        verifierDataService.insertCSCAs(List.of(getDefaultCSCA(0, "CH"), getDefaultCSCA(1, "DE")));
        assertEquals(1, verifierDataService.findCSCAs("CH").size());
    }

    @Test
    @Transactional
    void findActiveCscaKeyIdsTest() {
        final var chCSCA = getDefaultCSCA(0, "CH");
        final var deCSCA = getDefaultCSCA(1, "DE");
        verifierDataService.insertCSCAs(List.of(chCSCA, deCSCA));
        final var actualKeyIds = List.of(chCSCA.getKeyId(), deCSCA.getKeyId());
        final var activeCscaKeyIds = verifierDataService.findActiveCSCAKeyIds();
        assertTrue(
                activeCscaKeyIds.size() == actualKeyIds.size()
                        && activeCscaKeyIds.containsAll(actualKeyIds)
                        && actualKeyIds.containsAll(activeCscaKeyIds));
    }

    @Test
    @Transactional
    void insertDscTest() {
        verifierDataService.insertCSCAs(Collections.singletonList(getDefaultCSCA(0, "CH")));
        final var cscaId = verifierDataService.findCSCAs("CH").get(0).getId();
        verifierDataService.insertDSC(Collections.emptyList());
        assertTrue(verifierDataService.findActiveDSCKeyIds().isEmpty());
        final var rsaDsc = getRSADsc(0, "CH", cscaId);
        verifierDataService.insertDSC(Collections.singletonList(rsaDsc));
        assertEquals(1, verifierDataService.findActiveDSCKeyIds().size());
        assertEquals(
                rsaDsc.getKeyId(),
                verifierDataService.findDSCs(0L, CertFormat.ANDROID).get(0).getKeyId());
    }

    @Test
    @Transactional
    void removeDscsNotInTest() {
        verifierDataService.insertCSCAs(Collections.singletonList(getDefaultCSCA(0, "CH")));
        final var cscas = verifierDataService.findCSCAs("CH");
        assertEquals(1, cscas.size());
        final var cscaId = cscas.get(0).getId();
        final var rsaDsc = getRSADsc(0, "CH", cscaId);
        verifierDataService.insertDSC(Collections.singletonList(rsaDsc));
        verifierDataService.removeDSCsNotIn(Collections.emptyList());
        assertTrue(verifierDataService.findActiveDSCKeyIds().isEmpty());
        verifierDataService.removeDSCsNotIn(Collections.singletonList("keyid_0"));
        final var ecDsc = getECDsc(1, "CH", cscaId);
        verifierDataService.insertDSC(List.of(rsaDsc, ecDsc));
        verifierDataService.removeDSCsNotIn(Collections.singletonList(rsaDsc.getKeyId()));
        assertEquals(1, verifierDataService.findActiveDSCKeyIds().size());
    }

    @Test
    @Transactional
    void removeDscsWithCSCAIn() {
        verifierDataService.insertCSCAs(Collections.singletonList(getDefaultCSCA(0, "DE")));
        verifierDataService.insertCSCAs(Collections.singletonList(getDefaultCSCA(1, "DE")));
        final var cscas = verifierDataService.findCSCAs("DE");
        assertEquals(2, cscas.size());
        final var cscaId0 = cscas.get(0).getId();
        final var cscaId1 = cscas.get(1).getId();
        final var rsaDsc = getRSADsc(0, "DE", cscaId0);
        final var ecDsc = getECDsc(1, "DE", cscaId1);
        verifierDataService.insertDSC(List.of(rsaDsc, ecDsc));
        verifierDataService.removeDSCsWithCSCAIn(Collections.emptyList());
        assertEquals(2, verifierDataService.findActiveDSCKeyIds().size());
        verifierDataService.removeDSCsWithCSCAIn(List.of(cscas.get(0).getKeyId()));
        assertEquals(1, verifierDataService.findActiveDSCKeyIds().size());
    }

    @Test
    @Transactional
    void findDscsTest() {
        verifierDataService.insertCSCAs(Collections.singletonList(getDefaultCSCA(0, "CH")));
        final var cscaId = verifierDataService.findCSCAs("CH").get(0).getId();
        verifierDataService.insertDSC(
                List.of(getRSADsc(0, "CH", cscaId), getECDsc(1, "DE", cscaId)));
        assertEquals(2, verifierDataService.findDSCs(0L, CertFormat.IOS).size());
    }

    @Test
    @Transactional
    void findMaxDscsTest() {
        verifierDataService.insertCSCAs(Collections.singletonList(getDefaultCSCA(0, "CH")));
        final var cscaId = verifierDataService.findCSCAs("CH").get(0).getId();
        verifierDataService.insertDSC(
                List.of(getRSADsc(0, "CH", cscaId), getECDsc(1, "DE", cscaId)));
        final var maxDscPkId = verifierDataService.findMaxDSCPkId();
        assertTrue(verifierDataService.findDSCs(maxDscPkId, CertFormat.IOS).isEmpty());
        assertEquals(1, verifierDataService.findDSCs(maxDscPkId - 1, CertFormat.IOS).size());
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
