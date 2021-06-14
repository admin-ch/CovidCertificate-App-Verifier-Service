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
import ch.admin.bag.covidcertificate.backend.verifier.ws.utils.EtagUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import org.apache.http.HttpHeaders;
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
    private static final String REVOKED_CERT = "urn:uvci:01:CH:F0FDABC1708A81BB1A843891";

    @Autowired RestTemplate rt;

    protected MediaType acceptMediaType;

    @Value("${revocationList.baseurl}")
    private String baseurl = "https://covidcertificate-management-d.bag.admin.ch/api";

    private String revocationListUrl = "/trust/v1/revocationList";

    @Test
    public void getCertsTest() throws Exception {
        MockRestServiceServer mockServer = setupExternalRevocationListMock(1);

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
        assertEquals(REVOKED_CERT, revocationList.getRevokedCerts().get(0));
    }

    private MockRestServiceServer setupExternalRevocationListMock(int expectedCallCount)
            throws URISyntaxException, JsonProcessingException {
        MockRestServiceServer mockServer = MockRestServiceServer.createServer(rt);
        // setup mock server
        mockServer
                .expect(
                        ExpectedCount.times(expectedCallCount),
                        requestTo(new URI(baseurl + "v1/revocation-list")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(
                        withStatus(HttpStatus.OK)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(
                                        objectMapper.writeValueAsString(
                                                new String[] {REVOKED_CERT})));
        return mockServer;
    }

    @Test
    public void notModifiedTest() throws Exception {
        String expectedEtag = "\"" + EtagUtil.getUnsortedListHashcode(List.of(REVOKED_CERT)) + "\"";

        // get current etag
        setupExternalRevocationListMock(2);
        MockHttpServletResponse response =
                mockMvc.perform(
                                get(revocationListUrl)
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
                        get(revocationListUrl)
                                .accept(acceptMediaType)
                                .header(HttpHeaders.IF_NONE_MATCH, etag))
                .andExpect(status().isNotModified())
                .andReturn()
                .getResponse();
    }

    @Override
    protected String getUrlForSecurityHeadersTest() {
        return revocationListUrl;
    }

    @Test
    @Override
    public void testSecurityHeaders() throws Exception {
        setupExternalRevocationListMock(1);
        super.testSecurityHeaders();
    }

    @Override
    protected MediaType getSecurityHeadersRequestMediaType() {
        return this.acceptMediaType;
    }
}
