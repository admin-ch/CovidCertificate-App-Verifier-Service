/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.backend.verifier.ws.controller;

import static ch.admin.bag.covidcertificate.backend.verifier.data.util.TestUtil.getDefaultCsca;
import static ch.admin.bag.covidcertificate.backend.verifier.data.util.TestUtil.getEcDsc;
import static ch.admin.bag.covidcertificate.backend.verifier.data.util.TestUtil.getRsaDsc;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.admin.bag.covidcertificate.backend.verifier.data.VerifierDataService;
import ch.admin.bag.covidcertificate.backend.verifier.data.util.CacheUtil;
import ch.admin.bag.covidcertificate.backend.verifier.model.cert.ActiveCertsResponse;
import ch.admin.bag.covidcertificate.backend.verifier.model.cert.CertFormat;
import ch.admin.bag.covidcertificate.backend.verifier.model.cert.CertsResponse;
import ch.admin.bag.covidcertificate.backend.verifier.model.cert.ClientCert;
import ch.admin.bag.covidcertificate.backend.verifier.model.cert.db.DbDsc;
import ch.admin.bag.covidcertificate.backend.verifier.ws.util.TestHelper;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import org.apache.http.HttpHeaders;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.transaction.annotation.Transactional;

public abstract class KeyControllerV2Test extends BaseControllerTest {

    @Autowired protected VerifierDataService verifierDataService;

    protected MediaType acceptMediaType;

    private static final String BASE_URL = "/trust/v2/keys/";
    private static final String UPDATES_ENDPOINT = "updates";
    private static final String LIST_ENDPOINT = "list";

    private static final String NEXT_SINCE_HEADER = "X-Next-Since";
    private static final String UP_TO_DATE_HEADER = "up-to-date";
    private static final String UP_TO_HEADER = "up-to";

    private static final String UP_TO_QUERY_PARAM = "upTo";
    private static final String CERT_FORMAT_QUERY_PARAM = "certFormat";
    private static final String SINCE_QUERY_PARAM = "since";

    private static final String ORIGIN_CH = "CH";
    private static final String ORIGIN_DE = "DE";

    private Random rand = new Random();
    private List<Integer> suffixes = new ArrayList<>();

    @BeforeAll
    public void setup() {
        for (int i = 0; i < verifierDataService.getDscBatchSize() * 10; i++) {
            suffixes.add(i);
        }
    }

    @Test
    void helloTest() throws Exception {
        final MockHttpServletResponse response =
                mockMvc.perform(get(BASE_URL).accept(MediaType.TEXT_PLAIN))
                        .andExpect(status().is2xxSuccessful())
                        .andReturn()
                        .getResponse();

        assertNotNull(response);
        assertEquals("Hello from CH Covidcertificate Verifier WS", response.getContentAsString());
    }

    @Test
    public void keysUpdatesInvalidRequestTest() throws Exception {
        ResultMatcher expected = status().is4xxClientError();
        // missing param `certFormat`
        mockMvc.perform(
                        get(BASE_URL + UPDATES_ENDPOINT)
                                .accept(acceptMediaType)
                                .queryParam(UP_TO_QUERY_PARAM, String.valueOf(rand.nextInt())))
                .andExpect(expected)
                .andReturn();

        // missing param `upTo`
        mockMvc.perform(
                        get(BASE_URL + UPDATES_ENDPOINT)
                                .accept(acceptMediaType)
                                .queryParam(CERT_FORMAT_QUERY_PARAM, CertFormat.ANDROID.name()))
                .andExpect(expected)
                .andReturn();

        // invalid value `certFormat`
        mockMvc.perform(
                        get(BASE_URL + UPDATES_ENDPOINT)
                                .queryParam(UP_TO_QUERY_PARAM, String.valueOf(rand.nextInt()))
                                .queryParam(CERT_FORMAT_QUERY_PARAM, "windows")
                                .accept(acceptMediaType))
                .andExpect(expected)
                .andReturn();

        // invalid value `upTo`
        mockMvc.perform(
                        get(BASE_URL + UPDATES_ENDPOINT)
                                .queryParam(UP_TO_QUERY_PARAM, "a")
                                .queryParam(CERT_FORMAT_QUERY_PARAM, CertFormat.ANDROID.name())
                                .accept(acceptMediaType))
                .andExpect(expected)
                .andReturn();

        // invalid value `since`
        mockMvc.perform(
                        get(BASE_URL + UPDATES_ENDPOINT)
                                .queryParam(SINCE_QUERY_PARAM, "a")
                                .queryParam(UP_TO_QUERY_PARAM, String.valueOf(rand.nextInt()))
                                .queryParam(CERT_FORMAT_QUERY_PARAM, CertFormat.ANDROID.name())
                                .accept(acceptMediaType))
                .andExpect(expected)
                .andReturn();
    }

    @Test
    public void keyUpdatesValidRequestTest() throws Exception {
        ResultMatcher expected = status().is2xxSuccessful();
        // valid
        mockMvc.perform(
                        get(BASE_URL + UPDATES_ENDPOINT)
                                .queryParam(UP_TO_QUERY_PARAM, String.valueOf(rand.nextInt()))
                                .queryParam(CERT_FORMAT_QUERY_PARAM, CertFormat.ANDROID.name())
                                .accept(acceptMediaType))
                .andExpect(expected)
                .andReturn();

        // valid
        mockMvc.perform(
                        get(BASE_URL + UPDATES_ENDPOINT)
                                .queryParam(SINCE_QUERY_PARAM, String.valueOf(rand.nextInt()))
                                .queryParam(UP_TO_QUERY_PARAM, String.valueOf(rand.nextInt()))
                                .queryParam(CERT_FORMAT_QUERY_PARAM, CertFormat.ANDROID.name())
                                .accept(acceptMediaType))
                .andExpect(expected)
                .andReturn();
    }

    @Test
    @Transactional
    public void keysUpdatesTest() throws Exception {
        // fill db
        final Long cscaId = insertCsca();
        List<DbDsc> dscs = insertNDscs(cscaId, 15);
        int minPkId = (int) verifierDataService.findMaxDscPkId() - dscs.size();

        // get keys updates (all no since)
        Integer since = null;
        int upTo = (int) verifierDataService.findMaxDscPkId();
        MockHttpServletResponse response = getKeysUpdates(since, upTo);
        // verify response
        assertUpdatesResponse(response, dscs, since, upTo, true);

        // get keys updates (all with since)
        since = minPkId;
        response = getKeysUpdates(since, upTo);
        // verify response
        assertUpdatesResponse(response, dscs, since, upTo, true);

        // get keys updates (cut off with since)
        since = minPkId + rand.nextInt(7);
        response = getKeysUpdates(since, upTo);
        // verify response
        assertUpdatesResponse(response, dscs, since, upTo, true);

        // get keys updates (cut off with since and upTo)
        upTo = (int) verifierDataService.findMaxDscPkId() - rand.nextInt(7);
        response = getKeysUpdates(since, upTo);
        // verify response
        assertUpdatesResponse(response, dscs, since, upTo, true);

        // get keys updates (cut off with upTo)
        since = minPkId;
        response = getKeysUpdates(since, upTo);
        // verify response
        assertUpdatesResponse(response, dscs, since, upTo, true);

        // test batching
        int batchCount = 4;
        dscs.addAll(insertNDscs(cscaId, verifierDataService.getDscBatchSize() * (batchCount - 1)));

        // upTo set so no batching kicks in
        since = null;
        response = getKeysUpdates(since, upTo);
        // verify response
        assertUpdatesResponse(response, dscs, since, upTo, true);

        // upTo set so everything is returned and batching is required (page through)
        upTo = (int) verifierDataService.findMaxDscPkId();
        for (int i = 1; i <= batchCount; i++) {
            response = getKeysUpdates(since, upTo);
            // verify response
            assertUpdatesResponse(response, dscs, since, upTo, i == batchCount);
            since = Integer.valueOf(response.getHeader(NEXT_SINCE_HEADER));
        }
    }

    private MockHttpServletResponse getKeysUpdates(Integer since, int upTo) throws Exception {
        String sinceStr = since != null ? String.valueOf(since) : "";
        return mockMvc.perform(
                        get(BASE_URL + UPDATES_ENDPOINT)
                                .queryParam(SINCE_QUERY_PARAM, sinceStr)
                                .queryParam(UP_TO_QUERY_PARAM, String.valueOf(upTo))
                                .queryParam(CERT_FORMAT_QUERY_PARAM, CertFormat.ANDROID.name())
                                .accept(acceptMediaType))
                .andExpect(status().is2xxSuccessful())
                .andReturn()
                .getResponse();
    }

    private void assertUpdatesResponse(
            MockHttpServletResponse response,
            List<DbDsc> dscs,
            Integer since,
            int upTo,
            boolean expectedUpToDate)
            throws Exception {
        if (since == null) {
            since = 0;
        }
        assertNotNull(response);
        List<ClientCert> certs =
                testHelper
                        .verifyAndReadValue(
                                response,
                                acceptMediaType,
                                TestHelper.PATH_TO_CA_PEM,
                                CertsResponse.class)
                        .getCerts();

        // assert count
        int lowerCutCount =
                Math.max(0, since - ((int) verifierDataService.findMaxDscPkId() - dscs.size()));
        int upperCutCount = Math.max(0, (int) verifierDataService.findMaxDscPkId() - upTo);
        int expectedSize =
                Math.min(
                        verifierDataService.getDscBatchSize(),
                        Math.max(0, dscs.size() - lowerCutCount - upperCutCount));
        assertEquals(expectedSize, certs.size());

        // assert certs
        if (expectedSize > 0) {
            List<String> expectedKeyIds = new ArrayList<>();
            for (int i = lowerCutCount; i < (expectedSize + lowerCutCount); i++) {
                expectedKeyIds.add(dscs.get(i).getKeyId());
            }
            List<String> actualKeyIds =
                    certs.stream().map(ClientCert::getKeyId).collect(Collectors.toList());
            assertTrue(expectedKeyIds.containsAll(actualKeyIds));
        }

        // assert headers
        assertEquals(expectedUpToDate ? "true" : "false", response.getHeader(UP_TO_DATE_HEADER));
        assertEquals(
                String.valueOf(
                        Math.max((int) verifierDataService.findMaxDscPkId() - dscs.size(), since)
                                + expectedSize),
                response.getHeader(NEXT_SINCE_HEADER));
        assertExpiry(response, CacheUtil.KEYS_BUCKET_DURATION);
    }

    private void assertExpiry(MockHttpServletResponse response, Duration bucketDuration)
            throws ParseException {
        Instant expires =
                new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz")
                        .parse(response.getHeader("Expires"))
                        .toInstant();
        assertTrue(expires.isAfter(Instant.now()));
        assertFalse(expires.isAfter(Instant.now().plus(bucketDuration)));
    }

    @Test
    @Transactional
    public void keysListAndUpdatesUpToTest() throws Exception {
        // fill db
        final Long cscaId = insertCsca();
        List<DbDsc> dscs = insertSomeDscs(cscaId);

        // get active keys
        MockHttpServletResponse response =
                mockMvc.perform(get(BASE_URL + LIST_ENDPOINT).accept(acceptMediaType))
                        .andExpect(status().is2xxSuccessful())
                        .andReturn()
                        .getResponse();

        // verify response
        assertNotNull(response);
        ActiveCertsResponse activeCerts =
                testHelper.verifyAndReadValue(
                        response,
                        acceptMediaType,
                        TestHelper.PATH_TO_CA_PEM,
                        ActiveCertsResponse.class);

        List<String> expectedActiveKeyIds =
                dscs.stream().map(DbDsc::getKeyId).collect(Collectors.toList());
        List<String> activeKeyIds = activeCerts.getActiveKeyIds();
        assertEquals(expectedActiveKeyIds.size(), activeKeyIds.size());
        assertTrue(expectedActiveKeyIds.containsAll(activeKeyIds));

        assertEquals(Duration.ofHours(48).toMillis(), activeCerts.getValidDuration());

        String upTo = response.getHeader(UP_TO_HEADER);
        assertEquals(String.valueOf((int) verifierDataService.findMaxDscPkId()), upTo);
        assertExpiry(response, CacheUtil.KEYS_BUCKET_DURATION);

        // insert new dscs
        List<DbDsc> newDscs = insertSomeDscs(cscaId);

        // get keys updates up to
        response =
                mockMvc.perform(
                                get(BASE_URL + UPDATES_ENDPOINT)
                                        .queryParam(UP_TO_QUERY_PARAM, upTo)
                                        .queryParam(
                                                CERT_FORMAT_QUERY_PARAM, CertFormat.ANDROID.name())
                                        .accept(acceptMediaType))
                        .andExpect(status().is2xxSuccessful())
                        .andReturn()
                        .getResponse();

        // verify response
        CertsResponse certs =
                testHelper.verifyAndReadValue(
                        response, acceptMediaType, TestHelper.PATH_TO_CA_PEM, CertsResponse.class);
        List<ClientCert> clientCerts = certs.getCerts();
        assertEquals(expectedActiveKeyIds.size(), clientCerts.size());
        assertTrue(
                expectedActiveKeyIds.containsAll(
                        clientCerts.stream()
                                .map(ClientCert::getKeyId)
                                .collect(Collectors.toList())));

        // get active keys again
        response =
                mockMvc.perform(get(BASE_URL + LIST_ENDPOINT).accept(acceptMediaType))
                        .andExpect(status().is2xxSuccessful())
                        .andReturn()
                        .getResponse();

        // verify response
        activeCerts =
                testHelper.verifyAndReadValue(
                        response,
                        acceptMediaType,
                        TestHelper.PATH_TO_CA_PEM,
                        ActiveCertsResponse.class);

        expectedActiveKeyIds.addAll(
                newDscs.stream().map(DbDsc::getKeyId).collect(Collectors.toList()));
        activeKeyIds = activeCerts.getActiveKeyIds();
        assertEquals(expectedActiveKeyIds.size(), activeKeyIds.size());
        assertTrue(expectedActiveKeyIds.containsAll(activeKeyIds));

        upTo = response.getHeader(UP_TO_HEADER);
        assertEquals(String.valueOf((int) verifierDataService.findMaxDscPkId()), upTo);
    }

    @Test
    @Transactional
    public void keysListByCountryTest() throws Exception {
        // fill db with certs from CH and DE
        final Long cscaId = insertCsca();
        List<DbDsc> dscs = insertMultiCountryDscs(cscaId);

        // get active certs for CH
        MockHttpServletResponse response =
                mockMvc.perform(
                                get(BASE_URL + LIST_ENDPOINT)
                                        .accept(acceptMediaType)
                                        .queryParam("country", ORIGIN_CH))
                        .andExpect(status().is2xxSuccessful())
                        .andReturn()
                        .getResponse();

        // verify response
        assertNotNull(response);
        ActiveCertsResponse activeCerts =
                testHelper.verifyAndReadValue(
                        response,
                        acceptMediaType,
                        TestHelper.PATH_TO_CA_PEM,
                        ActiveCertsResponse.class);

        //list of expected CH certs
        List<String> expectedActiveKeyIds =
                dscs.stream()
                        .filter(dsc -> dsc.getOrigin().equals(ORIGIN_CH))
                        .map(DbDsc::getKeyId)
                        .collect(Collectors.toList());
        List<String> activeKeyIds = activeCerts.getActiveKeyIds();

        //check we really got the CH certs
        assertEquals(expectedActiveKeyIds.size(), activeKeyIds.size());
        assertTrue(expectedActiveKeyIds.containsAll(activeKeyIds));

        //validity field should be the same 48h as for the normal request
        assertEquals(Duration.ofHours(48).toMillis(), activeCerts.getValidDuration());

        //check the up to header is set correctly
        String upTo = response.getHeader(UP_TO_HEADER);
        assertEquals(String.valueOf((int) verifierDataService.findMaxDscPkIdForCountry(ORIGIN_CH)), upTo);
        assertExpiry(response, CacheUtil.KEYS_BUCKET_DURATION);
    }

    @Test
    @Transactional
    public void notModifiedTest() throws Exception {
        // get current etag
        MockHttpServletResponse response =
                mockMvc.perform(
                                get(BASE_URL + LIST_ENDPOINT)
                                        .accept(acceptMediaType)
                                        .header(HttpHeaders.IF_NONE_MATCH, "random"))
                        .andExpect(status().is2xxSuccessful())
                        .andReturn()
                        .getResponse();

        // test not modified
        String etag = response.getHeader(HttpHeaders.ETAG);
        mockMvc.perform(
                        get(BASE_URL + LIST_ENDPOINT)
                                .accept(acceptMediaType)
                                .header(HttpHeaders.IF_NONE_MATCH, etag))
                .andExpect(status().isNotModified())
                .andReturn()
                .getResponse();

        // add more dscs
        final Long cscaId = insertCsca();
        insertSomeDscs(cscaId);

        // get current etag
        response =
                mockMvc.perform(
                                get(BASE_URL + LIST_ENDPOINT)
                                        .accept(acceptMediaType)
                                        .header(HttpHeaders.IF_NONE_MATCH, etag))
                        .andExpect(status().is2xxSuccessful())
                        .andReturn()
                        .getResponse();

        // test not modified
        etag = response.getHeader(HttpHeaders.ETAG);
        mockMvc.perform(
                        get(BASE_URL + LIST_ENDPOINT)
                                .accept(acceptMediaType)
                                .header(HttpHeaders.IF_NONE_MATCH, etag))
                .andExpect(status().isNotModified())
                .andReturn()
                .getResponse();
    }

    private Long insertCsca() {
        verifierDataService.insertCscas(Collections.singletonList(getDefaultCsca(0, ORIGIN_CH)));
        return verifierDataService.findCscas(ORIGIN_CH).get(0).getId();
    }

    private List<DbDsc> insertNDscs(Long cscaId, Integer numToInsert) {
        List<DbDsc> dscs = new ArrayList<>();
        dscs.addAll(
                getRandomSuffixes().stream()
                        .map(s -> getRsaDsc(s, ORIGIN_CH, cscaId))
                        .collect(Collectors.toList()));
        dscs.addAll(
                getRandomSuffixes().stream()
                        .map(s -> getEcDsc(s, ORIGIN_CH, cscaId))
                        .collect(Collectors.toList()));
        if (numToInsert != null) {
            while (dscs.size() < numToInsert) {
                dscs.addAll(
                        getRandomSuffixes().stream()
                                .map(s -> getEcDsc(s, ORIGIN_CH, cscaId))
                                .collect(Collectors.toList()));
            }
            dscs = dscs.subList(0, numToInsert);
        }
        verifierDataService.insertDscs(dscs);
        return dscs;
    }

    private List<DbDsc> insertSomeDscs(Long cscaId) {
        return insertNDscs(cscaId, null);
    }

    private List<DbDsc> insertMultiCountryDscs(Long cscaId) {
        var result = insertNDscs(cscaId, null);
        var dsc =
                getRandomSuffixes().stream()
                        .map(s -> getRsaDsc(s, ORIGIN_DE, cscaId))
                        .findFirst()
                        .get();
        verifierDataService.insertManualDsc(dsc);
        result.add(dsc);
        return result;
    }

    /**
     * returns 1-10 random suffixes between 1 and 1000. no duplicates are returned within the scope
     * of this test class
     */
    private List<Integer> getRandomSuffixes() {
        if (suffixes.isEmpty()) {
            throw new RuntimeException("need more suffixes");
        }

        List<Integer> randomSuffixes = new ArrayList<>();

        final int minCount = 1;
        final int maxCount = 10;
        for (int i = 0; i < rand.nextInt(maxCount - minCount) + minCount; i++) {
            int randIndex = rand.nextInt(suffixes.size());
            randomSuffixes.add(suffixes.get(randIndex));
            suffixes.remove(randIndex);
        }
        return randomSuffixes;
    }

    @Override
    protected String getUrlForSecurityHeadersTest() {
        return BASE_URL;
    }

    @Override
    protected MediaType getSecurityHeadersRequestMediaType() {
        return MediaType.TEXT_PLAIN;
    }
}
