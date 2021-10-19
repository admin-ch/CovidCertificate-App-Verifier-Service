/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.backend.verifier.data;

import static ch.admin.bag.covidcertificate.backend.verifier.data.util.TestUtil.getDefaultCsca;
import static ch.admin.bag.covidcertificate.backend.verifier.data.util.TestUtil.getEcDsc;
import static ch.admin.bag.covidcertificate.backend.verifier.data.util.TestUtil.getRsaDsc;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.admin.bag.covidcertificate.backend.verifier.data.util.TestUtil;
import ch.admin.bag.covidcertificate.backend.verifier.model.CertSource;
import ch.admin.bag.covidcertificate.backend.verifier.model.cert.CertFormat;
import ch.admin.bag.covidcertificate.backend.verifier.model.cert.ClientCert;
import ch.admin.bag.covidcertificate.backend.verifier.model.cert.db.DbDsc;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
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
        assertTrue(verifierDataService.findActiveDscKeyIds().isEmpty());
        final var rsaDsc = getRsaDsc(0, "CH", cscaId);
        verifierDataService.insertDscs(Collections.singletonList(rsaDsc));

        List<String> activeDscKeyIds = verifierDataService.findActiveDscKeyIds();
        assertEquals(1, activeDscKeyIds.size());
        assertEquals(rsaDsc.getKeyId(), activeDscKeyIds.get(0));
        assertEquals(
                rsaDsc.getKeyId(),
                verifierDataService.findDscs(0L, CertFormat.ANDROID, null).get(0).getKeyId());
    }

    @Test
    @Transactional
    void removeDscsNotInTest() {
        // insert and test csca
        verifierDataService.insertCscas(Collections.singletonList(getDefaultCsca(0, "CH")));
        final var cscas = verifierDataService.findCscas("CH");
        assertEquals(1, cscas.size());

        // insert and test dsc
        final var cscaId = cscas.get(0).getId();
        final var rsaDsc = getRsaDsc(0, "CH", cscaId);
        verifierDataService.insertDscs(Collections.singletonList(rsaDsc));
        assertEquals(1, verifierDataService.findActiveDscKeyIds().size());

        // remove all dscs
        verifierDataService.removeDscsNotIn(List.of("INEXISTENT_KEY_ID"));
        assertTrue(verifierDataService.findActiveDscKeyIds().isEmpty());
        assertEquals(
                rsaDsc.getKeyId(),
                verifierDataService.findDscsMarkedForDeletion().get(0).getKeyId());

        // insert 2 dscs
        final var ecDsc = getEcDsc(1, "CH", cscaId);
        verifierDataService.insertDscs(List.of(rsaDsc, ecDsc));

        // remove 1 dsc
        verifierDataService.removeDscsNotIn(Collections.singletonList(ecDsc.getKeyId()));
        assertEquals(1, verifierDataService.findActiveDscKeyIds().size());
        assertEquals(ecDsc.getKeyId(), verifierDataService.findActiveDscKeyIds().get(0));
        assertEquals(
                rsaDsc.getKeyId(),
                verifierDataService.findDscsMarkedForDeletion().get(0).getKeyId());

        // set source to 'MANUAL' for current dsc in db
        updateSourceForAllDscs(CertSource.MANUAL);
        // insert another dsc with source = 'SYNC'
        verifierDataService.insertDscs(List.of(rsaDsc));
        assertEquals(2, verifierDataService.findActiveDscKeyIds().size());
        // verify manual doesnt get removed
        verifierDataService.removeDscsNotIn(List.of("INEXISTENT_KEY_ID"));
        assertEquals(1, verifierDataService.findActiveDscKeyIds().size());
        assertEquals(ecDsc.getKeyId(), verifierDataService.findActiveDscKeyIds().get(0));

        // reset sources to 'SYNC'. everything should now be removed
        updateSourceForAllDscs(CertSource.SYNC);
        verifierDataService.removeDscsNotIn(List.of("INEXISTENT_KEY_ID"));
        assertEquals(0, verifierDataService.findActiveDscKeyIds().size());
        assertEquals(2, verifierDataService.findDscsMarkedForDeletion().size());
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
        final var rsaDsc = getRsaDsc(0, "DE", cscaId0);
        final var ecDsc = getEcDsc(1, "DE", cscaId1);
        verifierDataService.insertDscs(List.of(rsaDsc, ecDsc));
        verifierDataService.removeDscsWithCscaIn(Collections.emptyList());
        assertEquals(2, verifierDataService.findActiveDscKeyIds().size());
        verifierDataService.removeDscsWithCscaIn(List.of(cscas.get(0).getKeyId()));
        assertEquals(1, verifierDataService.findActiveDscKeyIds().size());

        // verify manual doesnt get removed
        updateSourceForAllDscs(CertSource.MANUAL);
        verifierDataService.removeDscsWithCscaIn(List.of(cscas.get(1).getKeyId()));
        assertEquals(1, verifierDataService.findActiveDscKeyIds().size());

        updateSourceForAllDscs(CertSource.SYNC);
        verifierDataService.removeDscsWithCscaIn(List.of(cscas.get(1).getKeyId()));
        assertEquals(0, verifierDataService.findActiveDscKeyIds().size());
    }

    @Test
    @Transactional
    void findDscsTest() {

        verifierDataService.insertCscas(Collections.singletonList(getDefaultCsca(0, "CH")));
        final var cscaId = verifierDataService.findCscas("CH").get(0).getId();

        List<DbDsc> dscs = List.of(getRsaDsc(0, "CH", cscaId), getEcDsc(1, "DE", cscaId));
        verifierDataService.insertDscs(dscs);

        assertEquals(dscs.size(), verifierDataService.findDscs(0L, CertFormat.IOS, null).size());

        // test upTo
        long maxPkBeforeInsert = verifierDataService.findMaxDscPkId();
        verifierDataService.insertDscs(List.of(getEcDsc(2, "DE", cscaId)));
        assertEquals(
                dscs.size() + 1, verifierDataService.findDscs(0L, CertFormat.IOS, null).size());
        List<ClientCert> upTo1 =
                verifierDataService.findDscs(0L, CertFormat.IOS, maxPkBeforeInsert);
        assertEquals(dscs.size(), upTo1.size());
        List<String> expectedKeyIds =
                dscs.stream().map(DbDsc::getKeyId).collect(Collectors.toList());
        List<String> actualKeyIds =
                upTo1.stream().map(ClientCert::getKeyId).collect(Collectors.toList());
        assertTrue(expectedKeyIds.containsAll(actualKeyIds));
    }

    @Test
    @Transactional
    void findMaxDscsTest() {
        verifierDataService.insertCscas(Collections.singletonList(getDefaultCsca(0, "CH")));
        final var cscaId = verifierDataService.findCscas("CH").get(0).getId();
        verifierDataService.insertDscs(
                List.of(getRsaDsc(0, "CH", cscaId), getEcDsc(1, "DE", cscaId)));
        final var maxDscPkId = verifierDataService.findMaxDscPkId();
        assertTrue(verifierDataService.findDscs(maxDscPkId, CertFormat.IOS, null).isEmpty());
        assertEquals(1, verifierDataService.findDscs(maxDscPkId - 1, CertFormat.IOS, null).size());
    }

    @Test
    @Transactional
    void cleanUpDscsMarkedForDeletionTest() {
        // insert csca and 2 dscs
        verifierDataService.insertCscas(Collections.singletonList(getDefaultCsca(0, "CH")));
        final var cscaId = verifierDataService.findCscas("CH").get(0).getId();
        DbDsc toRemove = getRsaDsc(0, "CH", cscaId);
        DbDsc toKeep = getEcDsc(1, "DE", cscaId);
        verifierDataService.insertDscs(List.of(toRemove, toKeep));

        // mark 1 dsc for deletion (assert pre and post)
        assertEquals(0, verifierDataService.findDscsMarkedForDeletion().size());
        assertEquals(2, verifierDataService.findActiveDscKeyIds().size());
        verifierDataService.removeDscsNotIn(List.of(toKeep.getKeyId()));
        assertEquals(1, verifierDataService.findDscsMarkedForDeletion().size());
        assertEquals(1, verifierDataService.findActiveDscKeyIds().size());

        // run clean up. dsc should not be removed since it is too young
        verifierDataService.cleanUpDscsMarkedForDeletion();
        assertEquals(1, verifierDataService.findDscsMarkedForDeletion().size());
        assertEquals(1, verifierDataService.findActiveDscKeyIds().size());

        // move deleted_at timestamp back 2 days and test again.
        // dsc should not be removed since it is too young
        TestUtil.shiftDscDeletedAtBack(jt, 2);
        verifierDataService.cleanUpDscsMarkedForDeletion();
        assertEquals(1, verifierDataService.findDscsMarkedForDeletion().size());
        assertEquals(1, verifierDataService.findActiveDscKeyIds().size());

        // move deleted_at timestamp back 6 days and test again.
        // dsc should now be removed
        TestUtil.shiftDscDeletedAtBack(jt, 6);
        verifierDataService.cleanUpDscsMarkedForDeletion();
        assertEquals(0, verifierDataService.findDscsMarkedForDeletion().size());
        assertEquals(1, verifierDataService.findActiveDscKeyIds().size());
    }
}
