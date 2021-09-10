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

import ch.admin.bag.covidcertificate.backend.verifier.ws.util.TestHelper;
import ch.admin.bag.covidcertificate.backend.verifier.ws.utils.EtagUtil;
import java.util.Map;
import org.apache.http.HttpHeaders;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;

public abstract class VerificationRulesControllerTest extends BaseControllerTest {

    protected MediaType acceptMediaType;

    private String verificationRulesUrl = "/trust/v1/verificationRules";

    private static final String PATH_TO_VERIFICATION_RULES = "classpath:verificationRules.json";

    @Test
    public void verificationRulesTest() throws Exception {
        // get verification rules
        MockHttpServletResponse response =
                mockMvc.perform(get(verificationRulesUrl).accept(acceptMediaType))
                        .andExpect(status().is2xxSuccessful())
                        .andReturn()
                        .getResponse();

        // verify response
        assertNotNull(response);
        Map revocationList =
                testHelper.verifyAndReadValue(
                        response, acceptMediaType, TestHelper.PATH_TO_CA_PEM, Map.class);
        Map expected =
                testHelper
                        .getObjectMapper()
                        .readValue(
                                new ClassPathResource("verificationRules.json").getInputStream(),
                                Map.class);
        assertEquals(
                testHelper.getObjectMapper().writeValueAsString(expected),
                testHelper.getObjectMapper().writeValueAsString(revocationList));
    }

    @Test
    public void notModifiedTest() throws Exception {
        String expectedEtag = EtagUtil.getSha1HashForFiles(true, PATH_TO_VERIFICATION_RULES);

        // get current etag
        MockHttpServletResponse response =
                mockMvc.perform(
                                get(verificationRulesUrl)
                                        .accept(acceptMediaType)
                                        .header(HttpHeaders.IF_NONE_MATCH, "random"))
                        .andExpect(status().is2xxSuccessful())
                        .andReturn()
                        .getResponse();

        // verify etag
        String etag = response.getHeader(HttpHeaders.ETAG);
        assertEquals(expectedEtag, etag);

        // test not modified
        mockMvc.perform(
                        get(verificationRulesUrl)
                                .accept(acceptMediaType)
                                .header(HttpHeaders.IF_NONE_MATCH, etag))
                .andExpect(status().isNotModified())
                .andReturn()
                .getResponse();
    }

    @Override
    protected String getUrlForSecurityHeadersTest() {
        return verificationRulesUrl;
    }

    @Override
    protected MediaType getSecurityHeadersRequestMediaType() {
        return this.acceptMediaType;
    }
}
