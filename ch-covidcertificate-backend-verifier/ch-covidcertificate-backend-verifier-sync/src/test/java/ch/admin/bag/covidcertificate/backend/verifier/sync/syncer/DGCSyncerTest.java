package ch.admin.bag.covidcertificate.backend.verifier.sync.syncer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import ch.admin.bag.covidcertificate.backend.verifier.data.VerifierDataService;
import ch.admin.bag.covidcertificate.backend.verifier.model.sync.CertificateType;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
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

class DGCSyncerTest extends BaseDGCTest {

    private static final Logger logger = LoggerFactory.getLogger(DGCSyncerTest.class);
    private final String TEST_JSON_CSCA = "src/test/resources/covidcert-verifier_test_vectors_CSCA_stub.json";
    private final String TEST_JSON_DSC = "src/test/resources/covidcert-verifier_test_vectors_DSC_stub.json";
    private final String TEST_PROBLEM_JSON =
            "src/test/resources/covidcert-verifier_problem-report.json";

    @Value("${dgc.baseurl}")
    String baseurl = "https://testurl.europa.eu";

    @Autowired
    DGCSyncer dgcSyncer;

    @Autowired
    VerifierDataService verifierDataService;

    // TODO: Try running these tests with certificates from production environment

    @Test
    void downloadTest() throws Exception {
        final var certType = CertificateType.CSCA;
        var expectedCSCA = Files.readString(Path.of(TEST_JSON_CSCA));
        var expectedDSC = Files.readString(Path.of(TEST_JSON_DSC));
        final var mockServer = MockRestServiceServer.createServer(rt);
        mockServer
                .expect(
                        ExpectedCount.once(),
                        requestTo(new URI(baseurl + "/trustList/CSCA")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(
                        withStatus(HttpStatus.OK)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(expectedCSCA));
        mockServer
                .expect(
                        ExpectedCount.once(),
                        requestTo(new URI(baseurl + "/trustList/DSC")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(
                        withStatus(HttpStatus.OK)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(expectedDSC));
        dgcSyncer.sync();
    }
}
