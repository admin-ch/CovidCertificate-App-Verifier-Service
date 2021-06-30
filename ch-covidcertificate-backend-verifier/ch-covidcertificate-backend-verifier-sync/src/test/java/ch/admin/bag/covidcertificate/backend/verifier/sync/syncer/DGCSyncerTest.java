package ch.admin.bag.covidcertificate.backend.verifier.sync.syncer;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import ch.admin.bag.covidcertificate.backend.verifier.data.VerifierDataService;
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

class DGCSyncerTest extends BaseDGCTest {

    private final String TEST_JSON_CSCA =
            "src/test/resources/covidcert-verifier_test_vectors_CSCA.json";
    private final String TEST_JSON_DSC =
            "src/test/resources/covidcert-verifier_test_vectors_DSC.json";

    private final String TEST_JSON_CSCA_STUB =
            "src/test/resources/covidcert-verifier_test_vectors_CSCA_stub.json";
    private final String TEST_JSON_DSC_STUB =
            "src/test/resources/covidcert-verifier_test_vectors_DSC_stub.json";

    @Value("${dgc.baseurl}")
    String baseurl = "https://testurl.europa.eu";

    @Autowired DGCSyncer dgcSyncer;

    @Autowired VerifierDataService verifierDataService;

    @Test
    void downloadTest() throws Exception {
        String expectedCSCA = Files.readString(Path.of(TEST_JSON_CSCA_STUB));
        String expectedDSC = Files.readString(Path.of(TEST_JSON_DSC_STUB));
        setMockServer(expectedCSCA, expectedDSC);
        dgcSyncer.sync();
    }

    private void setMockServer(String expectedCSCA, String expectedDSC) throws URISyntaxException {
        final var mockServer = MockRestServiceServer.createServer(rt);
        mockServer
                .expect(ExpectedCount.once(), requestTo(new URI(baseurl + "/trustList/CSCA")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(
                        withStatus(HttpStatus.OK)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(expectedCSCA));
        mockServer
                .expect(ExpectedCount.once(), requestTo(new URI(baseurl + "/trustList/DSC")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(
                        withStatus(HttpStatus.OK)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(expectedDSC));
    }
}
