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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import ch.admin.bag.covidcertificate.backend.verifier.data.VerifierDataService;
import ch.admin.bag.covidcertificate.backend.verifier.sync.exception.DgcSyncException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;

class DgcSyncerTest extends BaseDgcTest {

    private final String TEST_JSON_CSCA =
            "src/test/resources/csca.json";
    private final String TEST_JSON_DSC =
            "src/test/resources/dsc.json";

    private final String TEST_JSON_CSCA_STUB =
            "src/test/resources/covidcert-verifier_test_vectors_CSCA_stub.json";
    private final String TEST_JSON_DSC_STUB =
            "src/test/resources/covidcert-verifier_test_vectors_DSC_stub.json";

    @Value("${dgc.baseurl}")
    String baseurl = "https://testurl.europa.eu";

    @Autowired DgcCertSyncer dgcSyncer;

    @Autowired VerifierDataService verifierDataService;

    @Test
    void downloadTest() throws Exception {
        String expectedCsca = Files.readString(Path.of(TEST_JSON_CSCA_STUB));
        String expectedDsc = Files.readString(Path.of(TEST_JSON_DSC_STUB));
        setMockServer(expectedCsca, expectedDsc);
        dgcSyncer.sync();
    }

    @Test
    void rollbackOnErrorTest() throws Exception {
        String expectedCsca = Files.readString(Path.of(TEST_JSON_CSCA));
        String expectedDsc = Files.readString(Path.of(TEST_JSON_DSC));
        // we set the mock server which returns 500 for any dsc request...
        setErrorMockServer(expectedCsca);
        //...hence the function throws a DgcSyncException!
        assertThrows(DgcSyncException.class, () -> dgcSyncer.sync());

        // We did not insert anything and the request failed, so the database should be
        // empty
        assertEquals(0, verifierDataService.findActiveCscaKeyIds().size());
        assertEquals(0, verifierDataService.findActiveDscKeyIds().size());

        // now set the server which succeeds and try sync again
        setMockServer(expectedCsca, expectedDsc);
        dgcSyncer.sync();

        // Now the database should _not_ be empty
        assertNotEquals(0, verifierDataService.findActiveCscaKeyIds().size());
        assertNotEquals(0, verifierDataService.findActiveDscKeyIds().size());
    }

    /**
     * Set a mock server which returns a 500 Internal Server Error if the /trustlist/DSC endpoint is called
     * @param expectedCsca JSON response of the CSCA request
     * @throws Exception
     */
    private void setErrorMockServer(String expectedCsca) throws Exception {
        final var mockServer = MockRestServiceServer.createServer(rt);
        mockServer
                .expect(ExpectedCount.once(), requestTo(new URI(baseurl + "/trustList/CSCA")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(
                        withStatus(HttpStatus.OK)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(expectedCsca));
        mockServer
                .expect(ExpectedCount.once(), requestTo(new URI(baseurl + "/trustList/DSC")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR).body("ERROR"));
    }

    /**
     * Set a mock server which succeeds for dsc and cscas both with a 200 and the corresponding JSON 
     * @param expectedCsca JSON Response of the CSCA request
     * @param expectedDsc JSON response of the dsc request
     * @throws URISyntaxException
     */
    private void setMockServer(String expectedCsca, String expectedDsc) throws URISyntaxException {
        final var mockServer = MockRestServiceServer.createServer(rt);
        mockServer
                .expect(ExpectedCount.once(), requestTo(new URI(baseurl + "/trustList/CSCA")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(
                        withStatus(HttpStatus.OK)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(expectedCsca));
        mockServer
                .expect(ExpectedCount.once(), requestTo(new URI(baseurl + "/trustList/DSC")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(
                        withStatus(HttpStatus.OK)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(expectedDsc));
    }
}
