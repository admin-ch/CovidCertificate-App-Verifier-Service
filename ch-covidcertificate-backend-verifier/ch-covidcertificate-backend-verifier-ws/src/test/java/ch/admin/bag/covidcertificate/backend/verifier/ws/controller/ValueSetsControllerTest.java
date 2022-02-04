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

import ch.admin.bag.covidcertificate.backend.verifier.data.ValueSetDataService;
import ch.admin.bag.covidcertificate.backend.verifier.ws.model.valuesets.ValueSets;
import ch.admin.bag.covidcertificate.backend.verifier.ws.util.TestHelper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.apache.http.HttpHeaders;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;

@TestInstance(Lifecycle.PER_CLASS)
public abstract class ValueSetsControllerTest extends BaseControllerTest {

    protected MediaType acceptMediaType;

    private String valueSetsUrl = "/trust/v1/metadata";

    @Autowired ValueSetDataService valueSetDataService;

    @BeforeAll
    public void setup() throws Exception {
        final Map<String, String> valueSets = new HashMap<>();
        valueSets.put(
                "covid-19-lab-test-manufacturer-and-name",
                Files.readString(Path.of("src/main/resources/valuesets/test-manf.json")));
        this.valueSetDataService.insertValueSets(valueSets);
    }

    @Test
    public void valueSetsTest() throws Exception {
        // get value sets
        MockHttpServletResponse response =
                mockMvc.perform(get(valueSetsUrl).accept(acceptMediaType))
                        .andExpect(status().is2xxSuccessful())
                        .andReturn()
                        .getResponse();

        // verify response
        assertNotNull(response);
        ValueSets valueSets =
                testHelper.verifyAndReadValue(
                        response, acceptMediaType, TestHelper.PATH_TO_CA_PEM, ValueSets.class);

        Map expected =
                testHelper
                        .getObjectMapper()
                        .readValue(
                                new ClassPathResource("valuesets/test-manf.json").getFile(),
                                Map.class);
        assertEquals(
                testHelper.getObjectMapper().writeValueAsString(expected),
                testHelper.getObjectMapper().writeValueAsString(valueSets.getTest().getManf()));

        expected =
                testHelper
                        .getObjectMapper()
                        .readValue(
                                new ClassPathResource("valuesets/test-type.json").getFile(),
                                Map.class);
        assertEquals(
                testHelper.getObjectMapper().writeValueAsString(expected),
                testHelper.getObjectMapper().writeValueAsString(valueSets.getTest().getType()));

        expected =
                testHelper
                        .getObjectMapper()
                        .readValue(
                                new ClassPathResource("valuesets/vaccine-mah-manf.json").getFile(),
                                Map.class);
        assertEquals(
                testHelper.getObjectMapper().writeValueAsString(expected),
                testHelper
                        .getObjectMapper()
                        .writeValueAsString(valueSets.getVaccine().getMahManf()));

        expected =
                testHelper
                        .getObjectMapper()
                        .readValue(
                                new ClassPathResource("valuesets/vaccine-medicinal-product.json")
                                        .getFile(),
                                Map.class);
        assertEquals(
                testHelper.getObjectMapper().writeValueAsString(expected),
                testHelper
                        .getObjectMapper()
                        .writeValueAsString(valueSets.getVaccine().getMedicinalProduct()));

        expected =
                testHelper
                        .getObjectMapper()
                        .readValue(
                                new ClassPathResource("valuesets/vaccine-prophylaxis.json")
                                        .getFile(),
                                Map.class);
        assertEquals(
                testHelper.getObjectMapper().writeValueAsString(expected),
                testHelper
                        .getObjectMapper()
                        .writeValueAsString(valueSets.getVaccine().getProphylaxis()));
    }

    @Test
    public void notModifiedTest() throws Exception {
        // get current etag
        MockHttpServletResponse response =
                mockMvc.perform(
                                get(valueSetsUrl)
                                        .accept(acceptMediaType)
                                        .header(HttpHeaders.IF_NONE_MATCH, "random"))
                        .andExpect(status().is2xxSuccessful())
                        .andReturn()
                        .getResponse();

        // test not modified
        String etag = response.getHeader(HttpHeaders.ETAG);
        mockMvc.perform(
                        get(valueSetsUrl)
                                .accept(acceptMediaType)
                                .header(HttpHeaders.IF_NONE_MATCH, etag))
                .andExpect(status().isNotModified())
                .andReturn()
                .getResponse();
    }

    @Override
    protected String getUrlForSecurityHeadersTest() {
        return valueSetsUrl;
    }

    @Override
    protected MediaType getSecurityHeadersRequestMediaType() {
        return this.acceptMediaType;
    }
}
