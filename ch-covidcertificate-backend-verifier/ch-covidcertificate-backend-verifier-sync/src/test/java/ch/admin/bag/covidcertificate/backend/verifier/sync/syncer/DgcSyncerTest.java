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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import ch.admin.bag.covidcertificate.backend.verifier.data.VerifierDataService;
import ch.admin.bag.covidcertificate.backend.verifier.sync.exception.DgcSyncException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Disabled;
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
    private final String TEST_JSON_TRUNCATED_CSCA = "src/test/resources/csca_truncated.json";
    private final String TEST_JSON_TRUNCATED_DSC = "src/test/resources/dsc_truncated.json";
    
    private final String TEST_JSON_HUGE_CSCA = "src/test/resources/csca_huge.json";
    private final String TEST_JSON_HUGE_DSC = "src/test/resources/dsc_huge.json";

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
        String expectedCsca = Files.readString(Path.of(TEST_JSON_CSCA));
        String expectedDsc = Files.readString(Path.of(TEST_JSON_DSC));
        
        setMockServer(expectedCsca, expectedDsc);
        dgcSyncer.sync();
        
    }

    @Test
    void emptyListDoesNotDeleteTest() throws Exception {
        String expectedCsca = Files.readString(Path.of(TEST_JSON_CSCA));
        String expectedDsc = Files.readString(Path.of(TEST_JSON_DSC));
        // we set the mock server which returns normal lists
        setMockServer(expectedCsca, expectedDsc);
        // ...hence the function inserts 7 * 20 certificates
        dgcSyncer.sync();

        assertEquals(7, verifierDataService.findActiveCscaKeyIds().size());
        assertEquals(140, verifierDataService.findActiveDscKeyIds().size());

        // Now we feed an empty list...
        String expectedEmptyCsca = Files.readString(Path.of(TEST_JSON_CSCA_STUB));
        String expectedEmptyDsc = Files.readString(Path.of(TEST_JSON_DSC_STUB));

        // ...this should actually not do a thing
        setMockServer(expectedEmptyCsca, expectedEmptyDsc);
        // ... in fact it should throw
        assertThrows(DgcSyncException.class, () -> dgcSyncer.sync());

        assertEquals(7, verifierDataService.findActiveCscaKeyIds().size());
        assertEquals(140, verifierDataService.findActiveDscKeyIds().size());
    }

    @Disabled
    @Test
    void hugeResponseTest() throws Exception {
        String expectedCsca = Files.readString(Path.of(TEST_JSON_HUGE_CSCA));
        String expectedDsc = Files.readString(Path.of(TEST_JSON_HUGE_DSC));
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
        assertEquals(7, verifierDataService.findActiveCscaKeyIds().size());
        assertEquals(140, verifierDataService.findActiveDscKeyIds().size());
    }

    @Test
    void deletionAndRecoveryTest() throws Exception {
        // Start with our set of 7 CSCAs where each CSCA has 20 DSCs
        String expectedCsca = Files.readString(Path.of(TEST_JSON_CSCA));
        String expectedDsc = Files.readString(Path.of(TEST_JSON_DSC));
        setMockServer(expectedCsca, expectedDsc);
        dgcSyncer.sync();
        // save max PK id, since after reinsertion the pk should increase 
        var maxPkId = verifierDataService.findMaxDscPkId();
        // Everything worked so we should have the full list
        assertEquals(7, verifierDataService.findActiveCscaKeyIds().size());
        assertEquals(140, verifierDataService.findActiveDscKeyIds().size());

        // we delete the DE CSCA and two XX DSCs -> we should end up with... 
        String expectedTruncatedCsca = Files.readString(Path.of(TEST_JSON_TRUNCATED_CSCA));
        String expectedTruncatedDsc = Files.readString(Path.of(TEST_JSON_TRUNCATED_DSC));
        setMockServer(expectedTruncatedCsca, expectedTruncatedDsc);
        dgcSyncer.sync();
        // ... 6 CSCAS ...
        assertEquals(7-1, verifierDataService.findActiveCscaKeyIds().size());
        // ... whereas the DE one is not there ...
        assertFalse(verifierDataService.findActiveCscaKeyIds().stream().anyMatch(a -> a.equals("mvIaDalHQRo=")));
        // ... and 2 XX and all of the 20 DEs are deleted 
        assertEquals(140-22, verifierDataService.findActiveDscKeyIds().size());

        // Now the DGC hub all of a sudden returns the same set again
        setMockServer(expectedCsca, expectedDsc);
        dgcSyncer.sync();
        var newMaxPkId = verifierDataService.findMaxDscPkId();
        // since we deleted 22 and now inserted 22 new, the max pk should be maxPkId + 22
        assertEquals(maxPkId + 22, newMaxPkId);
        // We also should be back to our intial state
        assertEquals(7, verifierDataService.findActiveCscaKeyIds().size());
        assertEquals(140, verifierDataService.findActiveDscKeyIds().size());

        // we delete the DE CSCA and two XX DSCs again to test the manual undelete -> we should end up with...
        setMockServer(expectedTruncatedCsca, expectedTruncatedDsc);
        dgcSyncer.sync();
        // ... 6 CSCAS ...
        assertEquals(7 - 1, verifierDataService.findActiveCscaKeyIds().size());
        // ... whereas the DE one is not there ...
        assertFalse(verifierDataService.findActiveCscaKeyIds().stream().anyMatch(a -> a.equals("mvIaDalHQRo=")));
        // ... and 2 XX and all of the 20 DEs are deleted
        assertEquals(140 - 22, verifierDataService.findActiveDscKeyIds().size());

        // Let's recover the deleted dscs
        var cscaMarkedForDeletion = verifierDataService.findCscaMarkedForDeletion();
        verifierDataService.insertCscas(cscaMarkedForDeletion);
        var dscMarkedForDeletion = verifierDataService.findDscsMarkedForDeletion();
        verifierDataService.insertDscs(dscMarkedForDeletion);
        // We should now end up with all original certificates
        assertEquals(7 , verifierDataService.findActiveCscaKeyIds().size());
        assertEquals(140, verifierDataService.findActiveDscKeyIds().size());
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
