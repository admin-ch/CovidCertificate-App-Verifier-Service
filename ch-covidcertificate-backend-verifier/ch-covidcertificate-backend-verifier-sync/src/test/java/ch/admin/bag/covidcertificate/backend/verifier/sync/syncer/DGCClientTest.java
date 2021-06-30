package ch.admin.bag.covidcertificate.backend.verifier.sync.syncer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import ch.admin.bag.covidcertificate.backend.verifier.model.sync.CertificateType;
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

class DGCClientTest extends BaseDGCTest {

    private static final Logger logger = LoggerFactory.getLogger(DGCClientTest.class);
    private final String TEST_JSON = "src/test/resources/covidcert-verifier_test_vectors.json";
    private final String TEST_PROBLEM_JSON =
            "src/test/resources/covidcert-verifier_problem-report.json";

    @Value("${dgc.baseurl}")
    String baseurl = "https://testurl.europa.eu";

    @Autowired
    DGCClient dgcClient;

    @Test
    void downloadTest() throws Exception {
        final var certType = CertificateType.CSCA;
        var expected = Files.readString(Path.of(TEST_JSON));
        logger.debug("Expected string: {}", expected);
        final var mockServer = MockRestServiceServer.createServer(rt);
        mockServer
                .expect(
                        ExpectedCount.once(),
                        requestTo(new URI(baseurl + "/trustList/" + certType.name())))
                .andExpect(method(HttpMethod.GET))
                .andRespond(
                        withStatus(HttpStatus.OK)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(expected));
        final var trustLists = dgcClient.download(certType);
        assertNotNull(trustLists);
        assertEquals(1, trustLists.length);
        assertEquals("ynSje/i0tac=", trustLists[0].getKid());
    }

    @Test
    void downloadProblemTest() throws Exception {
        final var certType = CertificateType.CSCA;
        var problem = Files.readString(Path.of(TEST_PROBLEM_JSON));
        logger.debug("Expected string: {}", problem);
        final var mockServer = MockRestServiceServer.createServer(rt);
        mockServer
                .expect(
                        ExpectedCount.once(),
                        requestTo(new URI(baseurl + "/trustList/" + certType.name())))
                .andExpect(method(HttpMethod.GET))
                .andRespond(
                        withStatus(HttpStatus.UNAUTHORIZED)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(problem));
        final var trustLists = dgcClient.download(certType);
        assertNotNull(trustLists);
        assertEquals(0, trustLists.length);
    }
}
