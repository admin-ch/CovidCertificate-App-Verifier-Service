package ch.admin.bag.covidcertificate.backend.verifier.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.admin.bag.covidcertificate.backend.verifier.model.CertSource;
import ch.admin.bag.covidcertificate.backend.verifier.model.cert.Algorithm;
import ch.admin.bag.covidcertificate.backend.verifier.model.cert.CertFormat;
import ch.admin.bag.covidcertificate.backend.verifier.model.cert.db.DbCsca;
import ch.admin.bag.covidcertificate.backend.verifier.model.cert.db.DbDsc;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.transaction.annotation.Transactional;

class VerifierDataServiceTest extends BaseDataServiceTest {

    @Autowired VerifierDataService verifierDataService;

    @Test
    @Transactional
    void insertCscasTest() {
        verifierDataService.insertCscas(Collections.emptyList());
        assertTrue(verifierDataService.findCscas("CH").isEmpty());
        var dbCsca = getDefaultCsca(0, "CH");
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
        verifierDataService.removeCscas(Collections.emptyList());
        verifierDataService.removeCscas(Collections.singletonList("keyid_0"));
        verifierDataService.insertCscas(Collections.singletonList(getDefaultCsca(0, "CH")));
        verifierDataService.insertCscas(Collections.singletonList(getDefaultCsca(1, "CH")));
        verifierDataService.removeCscas(Collections.singletonList("keyid_0"));
        assertEquals(1, verifierDataService.findCscas("CH").size());

        // verify manual doesnt get removed
        updateSourceForAllCscas(CertSource.MANUAL);
        verifierDataService.removeCscas(Collections.singletonList("keyid_1"));
        assertEquals(1, verifierDataService.findCscas("CH").size());

        updateSourceForAllCscas(CertSource.SYNC);
        verifierDataService.removeCscas(Collections.singletonList("keyid_1"));
        assertEquals(0, verifierDataService.findCscas("CH").size());
    }

    private void updateSourceForAllCscas(CertSource source) {
        jt.update(
                "update t_country_specific_certificate_authority set source = :source",
                new MapSqlParameterSource("source", source.name()));
    }

    private void updateSourceForAllDscs(CertSource source) {
        jt.update(
                "update t_document_signer_certificate set source = :source",
                new MapSqlParameterSource("source", source.name()));
    }

    @Test
    @Transactional
    void findCscaTest() {
        verifierDataService.insertCscas(List.of(getDefaultCsca(0, "CH"), getDefaultCsca(1, "DE")));
        assertEquals(1, verifierDataService.findCscas("CH").size());
    }

    @Test
    @Transactional
    void findActiveCscaKeyIdsTest() {
        final var chCsca = getDefaultCsca(0, "CH");
        final var deCsca = getDefaultCsca(1, "DE");
        verifierDataService.insertCscas(List.of(chCsca, deCsca));
        final var actualKeyIds = List.of(chCsca.getKeyId(), deCsca.getKeyId());
        final var activeCscaKeyIds = verifierDataService.findActiveCscaKeyIds();
        assertTrue(
                activeCscaKeyIds.size() == actualKeyIds.size()
                        && activeCscaKeyIds.containsAll(actualKeyIds)
                        && actualKeyIds.containsAll(activeCscaKeyIds));
    }

    @Test
    @Transactional
    void insertDscTest() {
        verifierDataService.insertCscas(Collections.singletonList(getDefaultCsca(0, "CH")));
        final var cscaId = verifierDataService.findCscas("CH").get(0).getId();
        verifierDataService.insertDscs(Collections.emptyList());
        assertTrue(verifierDataService.findActiveDscKeyIds(nowPlus1Min()).isEmpty());
        final var rsaDsc = getRSADsc(0, "CH", cscaId);
        verifierDataService.insertDscs(Collections.singletonList(rsaDsc));
        assertEquals(1, verifierDataService.findActiveDscKeyIds(nowPlus1Min()).size());
        assertEquals(
                rsaDsc.getKeyId(),
                verifierDataService
                        .findDscs(0L, CertFormat.ANDROID, nowPlus1Min())
                        .get(0)
                        .getKeyId());
    }

    @Test
    @Transactional
    void removeDscsNotInTest() {
        verifierDataService.insertCscas(Collections.singletonList(getDefaultCsca(0, "CH")));
        final var cscas = verifierDataService.findCscas("CH");
        assertEquals(1, cscas.size());
        final var cscaId = cscas.get(0).getId();
        final var rsaDsc = getRSADsc(0, "CH", cscaId);
        verifierDataService.insertDscs(Collections.singletonList(rsaDsc));
        verifierDataService.removeDscsNotIn(Collections.emptyList());
        assertTrue(verifierDataService.findActiveDscKeyIds(nowPlus1Min()).isEmpty());
        verifierDataService.removeDscsNotIn(Collections.singletonList("keyid_0"));
        final var ecDsc = getECDsc(1, "CH", cscaId);
        verifierDataService.insertDscs(List.of(rsaDsc, ecDsc));
        verifierDataService.removeDscsNotIn(Collections.singletonList(rsaDsc.getKeyId()));
        assertEquals(1, verifierDataService.findActiveDscKeyIds(nowPlus1Min()).size());

        // verify manual doesnt get removed
        updateSourceForAllDscs(CertSource.MANUAL);
        verifierDataService.removeDscsNotIn(Collections.singletonList(ecDsc.getKeyId()));
        assertEquals(1, verifierDataService.findActiveDscKeyIds(nowPlus1Min()).size());

        updateSourceForAllDscs(CertSource.SYNC);
        verifierDataService.removeDscsNotIn(Collections.singletonList(ecDsc.getKeyId()));
        assertEquals(0, verifierDataService.findActiveDscKeyIds(nowPlus1Min()).size());
    }

    @Test
    @Transactional
    void removeDscsWithCscaIn() {
        verifierDataService.insertCscas(Collections.singletonList(getDefaultCsca(0, "DE")));
        verifierDataService.insertCscas(Collections.singletonList(getDefaultCsca(1, "DE")));
        final var cscas = verifierDataService.findCscas("DE");
        assertEquals(2, cscas.size());
        final var cscaId0 = cscas.get(0).getId();
        final var cscaId1 = cscas.get(1).getId();
        final var rsaDsc = getRSADsc(0, "DE", cscaId0);
        final var ecDsc = getECDsc(1, "DE", cscaId1);
        verifierDataService.insertDscs(List.of(rsaDsc, ecDsc));
        verifierDataService.removeDscsWithCscaIn(Collections.emptyList());
        assertEquals(2, verifierDataService.findActiveDscKeyIds(nowPlus1Min()).size());
        verifierDataService.removeDscsWithCscaIn(List.of(cscas.get(0).getKeyId()));
        assertEquals(1, verifierDataService.findActiveDscKeyIds(nowPlus1Min()).size());

        // verify manual doesnt get removed
        updateSourceForAllDscs(CertSource.MANUAL);
        verifierDataService.removeDscsWithCscaIn(List.of(cscas.get(1).getKeyId()));
        assertEquals(1, verifierDataService.findActiveDscKeyIds(nowPlus1Min()).size());

        updateSourceForAllDscs(CertSource.SYNC);
        verifierDataService.removeDscsWithCscaIn(List.of(cscas.get(1).getKeyId()));
        assertEquals(0, verifierDataService.findActiveDscKeyIds(nowPlus1Min()).size());
    }

    @Test
    @Transactional
    void findDscsTest() {
        verifierDataService.insertCscas(Collections.singletonList(getDefaultCsca(0, "CH")));
        final var cscaId = verifierDataService.findCscas("CH").get(0).getId();
        verifierDataService.insertDscs(
                List.of(getRSADsc(0, "CH", cscaId), getECDsc(1, "DE", cscaId)));
        assertEquals(2, verifierDataService.findDscs(0L, CertFormat.IOS, nowPlus1Min()).size());
    }

    @Test
    @Transactional
    void findMaxDscsTest() {
        verifierDataService.insertCscas(Collections.singletonList(getDefaultCsca(0, "CH")));
        final var cscaId = verifierDataService.findCscas("CH").get(0).getId();
        verifierDataService.insertDscs(
                List.of(getRSADsc(0, "CH", cscaId), getECDsc(1, "DE", cscaId)));
        final var maxDscPkId = verifierDataService.findMaxDscPkId();
        assertTrue(
                verifierDataService.findDscs(maxDscPkId, CertFormat.IOS, nowPlus1Min()).isEmpty());
        assertEquals(
                1,
                verifierDataService.findDscs(maxDscPkId - 1, CertFormat.IOS, nowPlus1Min()).size());
    }

    private DbCsca getDefaultCsca(int idSuffix, String origin) {
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
        dbDsc.setUse("sig");
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
        dbDsc.setUse("sig");
        dbDsc.setAlg(Algorithm.ES256);
        dbDsc.setCrv("crv");
        dbDsc.setX("x");
        dbDsc.setY("y");
        return dbDsc;
    }

    private Date nowPlus1Min() {
        return Date.from(OffsetDateTime.now().plusMinutes(1).toInstant());
    }
}
