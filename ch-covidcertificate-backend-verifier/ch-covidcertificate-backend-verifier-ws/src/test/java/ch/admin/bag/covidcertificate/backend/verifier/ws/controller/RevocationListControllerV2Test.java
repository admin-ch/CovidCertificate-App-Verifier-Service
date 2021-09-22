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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.admin.bag.covidcertificate.backend.verifier.data.RevokedCertDataService;
import ch.admin.bag.covidcertificate.backend.verifier.data.util.CacheUtil;
import ch.admin.bag.covidcertificate.backend.verifier.data.util.TestUtil;
import ch.admin.bag.covidcertificate.backend.verifier.model.RevocationResponse;
import ch.admin.bag.covidcertificate.backend.verifier.ws.util.TestHelper;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.UnsupportedEncodingException;
import java.time.Instant;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;

public abstract class RevocationListControllerV2Test extends BaseControllerTest {
    @Autowired protected RevokedCertDataService revokedCertDataService;
    @Autowired protected DataSource dataSource;

    protected MediaType acceptMediaType;

    private static final String NEXT_SINCE_HEADER = "X-Next-Since";
    private static final String UP_TO_DATE_HEADER = "up-to-date";
    private static final String EXPIRES_HEADER = "Expires";

    private static final String SINCE_QUERY_PARAM = "since";

    private String revocationListUrl = "/trust/v2/revocationList";
    private NamedParameterJdbcTemplate jt;

    void setup() {
        this.jt = new NamedParameterJdbcTemplate(dataSource);
    }

    @Test
    public void getRevokedCertsTest() throws Exception {
        String since = null;

        /** no revoked certs */

        // clear revoked certs
        TestUtil.clearRevokedCerts(jt);

        // get next revocation list batch
        MockHttpServletResponse response = getNextRevocationListBatch(since);

        // verify response
        assertRevocationListBatchResponse(
                response,
                0,
                true,
                "0",
                CacheUtil.roundToNextRevocationRetentionBucketStart(Instant.now()));

        /** imported revoked certs (all released) */

        // import revoked certs
        int releasedRevokedCertCount = 20;
        revokedCertDataService.replaceRevokedCerts(
                TestUtil.getRevokedCertUvcis(releasedRevokedCertCount));

        // release certs
        TestUtil.releaseRevokedCerts(jt, Instant.now());

        // get next revocation list batch
        response = getNextRevocationListBatch(since);

        // verify response
        assertRevocationListBatchResponse(
                response,
                releasedRevokedCertCount,
                true,
                null,
                CacheUtil.roundToNextRevocationRetentionBucketStart(Instant.now()));
        String nextSince = response.getHeader(NEXT_SINCE_HEADER);

        /** imported additional revoked certs (new not released yet) */

        // import with new (unreleased) revoked certs
        int newRevokedCertCount = 10;
        revokedCertDataService.replaceRevokedCerts(
                TestUtil.getRevokedCertUvcis(releasedRevokedCertCount + newRevokedCertCount));

        // get next revocation list batch
        response = getNextRevocationListBatch(since);

        // verify response (unchanged)
        assertRevocationListBatchResponse(
                response,
                releasedRevokedCertCount,
                true,
                nextSince,
                CacheUtil.roundToNextRevocationRetentionBucketStart(Instant.now()));

        /** release new certs */

        // release certs
        TestUtil.releaseRevokedCerts(jt, Instant.now());
        releasedRevokedCertCount += newRevokedCertCount;

        // get next revocation list batch
        response = getNextRevocationListBatch(since);

        // verify response
        assertRevocationListBatchResponse(
                response,
                releasedRevokedCertCount,
                true,
                String.valueOf(Long.parseLong(nextSince) + newRevokedCertCount),
                CacheUtil.roundToNextRevocationRetentionBucketStart(Instant.now()));

        /** test batching */

        // import and release
        int batchSize = revokedCertDataService.getRevokedCertBatchSize();
        int batchCount = 4;
        releasedRevokedCertCount = batchSize * batchCount;
        revokedCertDataService.replaceRevokedCerts(
                TestUtil.getRevokedCertUvcis(releasedRevokedCertCount));
        TestUtil.releaseRevokedCerts(jt, Instant.now());

        // fetch and assert
        for (int i = 1; i <= batchCount; i++) {
            // get next revocation list batch
            response = getNextRevocationListBatch(since);
            // verify response (unchanged)
            assertRevocationListBatchResponse(
                    response,
                    batchSize,
                    i == batchCount,
                    since != null ? String.valueOf(Long.parseLong(since) + batchSize) : null,
                    CacheUtil.roundToNextRevocationRetentionBucketStart(Instant.now()));
            since = response.getHeader(NEXT_SINCE_HEADER);
        }
    }

    private MockHttpServletResponse getNextRevocationListBatch(String since) throws Exception {
        return mockMvc.perform(
                        get(revocationListUrl)
                                .queryParam(SINCE_QUERY_PARAM, since)
                                .accept(acceptMediaType))
                .andExpect(status().is2xxSuccessful())
                .andReturn()
                .getResponse();
    }

    private void assertRevocationListBatchResponse(
            MockHttpServletResponse response,
            int expectedSize,
            boolean expectedUpToDate,
            String expectedNextSinceHeader,
            Instant expectedExpire)
            throws JsonProcessingException, UnsupportedEncodingException {
        assertNotNull(response);

        RevocationResponse revocationList =
                testHelper.verifyAndReadValue(
                        response,
                        acceptMediaType,
                        TestHelper.PATH_TO_CA_PEM,
                        RevocationResponse.class);
        assertNotNull(revocationList.getRevokedCerts());
        assertEquals(expectedSize, revocationList.getRevokedCerts().size());

        assertHeaders(response, expectedUpToDate, expectedNextSinceHeader, expectedExpire);
    }

    private void assertHeaders(
            MockHttpServletResponse response,
            boolean expectedUpToDate,
            String expectedNextSinceHeader,
            Instant expectedExpires) {
        assertEquals(expectedUpToDate ? "true" : "false", response.getHeader(UP_TO_DATE_HEADER));
        if (expectedNextSinceHeader != null) {
            assertEquals(expectedNextSinceHeader, response.getHeader(NEXT_SINCE_HEADER));
        }
        assertExpires(response, expectedExpires);
    }

    private void assertExpires(MockHttpServletResponse response, Instant expectedExpires) {
        assertEquals(
                CacheUtil.formatHeaderDate(expectedExpires), response.getHeader(EXPIRES_HEADER));
    }

    @Override
    protected String getUrlForSecurityHeadersTest() {
        return revocationListUrl;
    }

    @Test
    @Override
    public void testSecurityHeaders() throws Exception {
        super.testSecurityHeaders();
    }

    @Override
    protected MediaType getSecurityHeadersRequestMediaType() {
        return this.acceptMediaType;
    }
}
