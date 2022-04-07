/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.backend.verifier.sync.syncer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;

class ForeignRulesSyncTest extends BaseDgcTest {

    private static final Logger logger = LoggerFactory.getLogger(ForeignRulesSyncTest.class);
    private final String COUNTRY_LIST_JSON = "src/test/resources/countrylist.json";
    private final String AT_JSON = "src/test/resources/AT.json";
    private final String DE_JSON = "src/test/resources/DE.json";

    @Value("${dgc.baseurl}")
    String baseurl = "https://testurl.europa.eu";

    @Autowired DgcForeignRulesSyncer rulesSyncer;

    @Test
    void downloadTest() throws Exception {
        var countryList = Files.readString(Path.of(COUNTRY_LIST_JSON));
        var at = Files.readString(Path.of(AT_JSON));
        var de = Files.readString(Path.of(DE_JSON));
        final var mockServer = MockRestServiceServer.createServer(rt);
        mockServer
                .expect(
                        ExpectedCount.once(),
                        requestTo(new URI(baseurl + "/countrylist")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(
                        withStatus(HttpStatus.OK)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(countryList));
        mockServer
                .expect(
                        ExpectedCount.once(),
                        requestTo(new URI(baseurl + "/rules/AT")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(
                        withStatus(HttpStatus.OK)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(at));
        mockServer
                .expect(
                        ExpectedCount.once(),
                        requestTo(new URI(baseurl + "/rules/DE")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(
                        withStatus(HttpStatus.OK)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(de));
        int result = rulesSyncer.sync();
        assertEquals(29, result);
    }

}
