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

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles({"actuator-security", "api-key"})
@SpringBootTest(
        properties = {
            "ws.monitor.prometheus.user=prometheus",
            "ws.monitor.prometheus.password=prometheus",
            "management.endpoints.enabled-by-default=true",
            "management.endpoints.web.exposure.include=*",
            "ws.authentication.apiKeys.unit-test=4d1d5663-b4ef-46a5-85b6-3d1d376429da"
        })
@TestInstance(Lifecycle.PER_CLASS)
public class ApiKeyAuthenticationTest extends BaseControllerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiKeyAuthenticationTest.class);

    private static final String AUTHORIZATION = "Bearer 4d1d5663-b4ef-46a5-85b6-3d1d376429da";
    private static final String WRONG_AUTHORIZATION = "Bearer 251f03cf-5c13-4120-8fe6-b2abf784c007";
    private static final String BASE_URL = "/trust/v1";
    private static final List<String> AUTHENTICATED_ENDPOINTS =
            List.of(
                    BASE_URL + "/keys/list",
                    BASE_URL + "/keys/updates?certFormat=IOS",
                    BASE_URL + "/verificationRules");

    @Override
    protected String getUrlForSecurityHeadersTest() {
        return BASE_URL + "/keys";
    }

    @Test
    public void testUnprotectedHello() throws Exception {
        final MockHttpServletResponse response =
                mockMvc.perform(get(BASE_URL + "/keys").accept(MediaType.TEXT_PLAIN))
                        .andExpect(status().is2xxSuccessful())
                        .andReturn()
                        .getResponse();

        assertNotNull(response);
        assertEquals("Hello from CH Covidcertificate Verifier WS", response.getContentAsString());
    }

    @Test
    public void testAuthentication() throws Exception {
        for (String endpoint : AUTHENTICATED_ENDPOINTS) {
            testAuthenticationForEndpoint(endpoint);
        }
    }

    private void testAuthenticationForEndpoint(String url) throws Exception {
        LOGGER.info("testing authentication for endpoint: {}", url);

        // authorization header not set
        mockMvc.perform(get(url).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andReturn()
                .getResponse();

        // invalid api key
        mockMvc.perform(
                        get(url).accept(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, WRONG_AUTHORIZATION))
                .andExpect(status().isForbidden())
                .andReturn()
                .getResponse();

        // valid api key
        final MockHttpServletResponse response =
                mockMvc.perform(
                                get(url).accept(MediaType.APPLICATION_JSON)
                                        .header(HttpHeaders.AUTHORIZATION, AUTHORIZATION))
                        .andExpect(status().is2xxSuccessful())
                        .andReturn()
                        .getResponse();
        assertNotNull(response);
    }
}
