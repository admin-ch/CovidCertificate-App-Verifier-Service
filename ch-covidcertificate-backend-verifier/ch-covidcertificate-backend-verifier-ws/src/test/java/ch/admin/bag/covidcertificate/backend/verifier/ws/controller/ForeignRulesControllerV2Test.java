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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.admin.bag.covidcertificate.backend.verifier.ws.util.TestHelper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;

public class ForeignRulesControllerV2Test extends BaseControllerTest {

    protected MediaType acceptMediaType = MediaType.APPLICATION_JSON;

    private String atRulesUrl = "/trust/v2/foreignRules/AT";
    private String countryListUrl = "/trust/v2/foreignRules";


    @Test
    public void countryListTest() throws Exception {
        // get verification rules
        MockHttpServletResponse response =
                mockMvc.perform(get(countryListUrl).accept(acceptMediaType))
                        .andExpect(status().is2xxSuccessful())
                        .andReturn()
                        .getResponse();

        // verify response
        assertNotNull(response);

        ObjectNode countries = objectMapper.valueToTree(testHelper.verifyAndReadValue(
                response, acceptMediaType, TestHelper.PATH_TO_CA_PEM, Map.class));
        assertTrue(countries.get("countries").get(0).asText().equals("AT"));
        assertTrue(countries.get("countries").get(1).asText().equals("DE"));

    }

    @Test
    public void foreignRulesTest() throws Exception {
        // get verification rules
        MockHttpServletResponse response =
                mockMvc.perform(get(atRulesUrl).accept(acceptMediaType))
                        .andExpect(status().is2xxSuccessful())
                        .andReturn()
                        .getResponse();

        // verify response
        assertNotNull(response);
        ObjectNode rules = objectMapper.valueToTree(testHelper.verifyAndReadValue(
                response, acceptMediaType, TestHelper.PATH_TO_CA_PEM, Map.class));
        assertNotNull(rules.get("rules").get(0));
    }

    @Test
    @Disabled
    public void notModifiedTest() throws Exception {
        /*String expectedEtag = EtagUtil.getSha1HashForStrings()

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
                .getResponse();*/
    }

    @Override
    protected String getUrlForSecurityHeadersTest() {
        return atRulesUrl;
    }

    @Override
    protected MediaType getSecurityHeadersRequestMediaType() {
        return this.acceptMediaType;
    }
}
