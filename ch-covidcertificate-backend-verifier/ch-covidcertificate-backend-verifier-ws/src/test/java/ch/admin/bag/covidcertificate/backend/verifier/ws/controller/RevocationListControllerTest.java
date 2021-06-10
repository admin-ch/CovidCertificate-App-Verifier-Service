package ch.admin.bag.covidcertificate.backend.verifier.ws.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.admin.bag.covidcertificate.backend.verifier.model.RevocationResponse;
import ch.admin.bag.covidcertificate.backend.verifier.ws.util.TestHelper;
import java.net.URI;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

public abstract class RevocationListControllerTest extends BaseControllerTest {
    @Autowired RestTemplate rt;

    protected MediaType acceptMediaType;

    @Value("${revocationList.baseurl}")
    private String baseurl = "https://covidcertificate-management-d.bag.admin.ch/api";

    private String revocationListUrl = "/trust/v1/revocationList";
    private MockRestServiceServer mockServer;

    @BeforeAll
    public void setup() {
        this.mockServer = MockRestServiceServer.createServer(rt);
    }

    @Test
    public void getCertsTest() throws Exception {
        var expected = "urn:uvci:01:CH:F0FDABC1708A81BB1A843891";

        // setup mock server
        mockServer
                .expect(ExpectedCount.once(), requestTo(new URI(baseurl + "v1/revocation-list")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(
                        withStatus(HttpStatus.OK)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(objectMapper.writeValueAsString(new String[] {expected})));

        // get revocation list
        MockHttpServletResponse response =
                mockMvc.perform(get(revocationListUrl).accept(acceptMediaType))
                        .andExpect(status().is2xxSuccessful())
                        .andReturn()
                        .getResponse();

        mockServer.verify();

        // verify response
        assertNotNull(response);
        RevocationResponse revocationList =
                testHelper.verifyAndReadValue(
                        response,
                        acceptMediaType,
                        TestHelper.PATH_TO_CA_PEM,
                        RevocationResponse.class);
        assertNotNull(revocationList.getRevokedCerts());
        assertEquals(1, revocationList.getRevokedCerts().size());
        assertEquals(expected, revocationList.getRevokedCerts().get(0));
    }

    @Override
    protected String getUrlForSecurityHeadersTest() {
        return revocationListUrl;
    }

    @Override
    public void testSecurityHeaders() throws Exception {
        // setup mock server
        mockServer
                .expect(ExpectedCount.once(), requestTo(new URI(baseurl + "v1/revocation-list")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(
                        withStatus(HttpStatus.OK)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(objectMapper.writeValueAsString(new String[] {})));

        super.testSecurityHeaders();
    }
}
