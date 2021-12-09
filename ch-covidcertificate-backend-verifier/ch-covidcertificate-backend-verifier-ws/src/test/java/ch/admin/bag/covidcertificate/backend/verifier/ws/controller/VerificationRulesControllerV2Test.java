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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.http.HttpHeaders;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;

public abstract class VerificationRulesControllerV2Test extends BaseControllerTest {

    protected MediaType acceptMediaType;

    private String verificationRulesUrl = "/trust/v2/verificationRules";

    @Value("${testing.disabledModes}")
    private String[] disabledModes;

    private static final String PATH_TO_VERIFICATION_RULES = "classpath:verificationRulesV2.json";
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
        ObjectNode rules = objectMapper.valueToTree(testHelper.verifyAndReadValue(
                        response, acceptMediaType, TestHelper.PATH_TO_CA_PEM, Map.class));
        ObjectNode expected =
                (ObjectNode) testHelper
                        .getObjectMapper()
                        .readTree(
                                new ClassPathResource("verificationRulesV2.json").getInputStream());

        if(isNoModeRemovalTest()){
            //Make sure Spring didn't somehow smuggle in any values
            assert(disabledModes.length == 0);
        } else {
            //Check that we are actually trying to remove a mode
            assert(disabledModes.length >= 1);

            //Check if the modes we're trying to remove actually exist. Else we wouldn't be testing anything
            for (String disabledMode : disabledModes) {
                boolean modeExists = false;
                var iter = expected.get("modeRules").get("activeModes").iterator();
                while (iter.hasNext()) {
                    JsonNode mode = iter.next();
                    if (disabledMode.equals(mode.get("id").asText())) {
                        modeExists = true;
                        iter.remove();
                    }
                }
                if (!modeExists) {
                    throw new IllegalArgumentException(
                            "JSON doesn't seem to have the mode we're testing for. Edit test case or JSON");
                }
            }
        }

        expected.remove("valueSets");//ValueSets don't work in the test environment
        rules.remove("valueSets");


        assertEquals(
                testHelper.getObjectMapper().writeValueAsString(expected),
                testHelper.getObjectMapper().writeValueAsString(rules));
    }

    protected boolean isNoModeRemovalTest(){
        return false;
    }

    @Test
    @Disabled("calculating expected ETag from the file doesn't work for V2")
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
