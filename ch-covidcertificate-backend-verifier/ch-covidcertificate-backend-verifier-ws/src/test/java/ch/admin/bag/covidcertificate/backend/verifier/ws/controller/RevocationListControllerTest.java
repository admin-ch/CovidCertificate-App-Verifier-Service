package ch.admin.bag.covidcertificate.backend.verifier.ws.controller;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.admin.bag.covidcertificate.backend.verifier.ws.controller.RevocationListController.RevocationResponse;
import java.net.URI;
import java.util.Collections;
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

class RevocationListControllerTest extends BaseControllerTest {
    @Autowired RevocationListController revocationListController;
    @Autowired RestTemplate rt;

    @Value("${revocationList.baseurl}")
    String baseurl = "https://covidcertificate-management-d.bag.admin.ch/api";

    @Test
    public void getCertsTest() throws Exception {
        final var mockServer = MockRestServiceServer.createServer(rt);
        var expected = "urn:uvci:01:CH:F0FDABC1708A81BB1A843891";
        mockServer
                .expect(ExpectedCount.once(), requestTo(new URI(baseurl + "v1/revocation-list")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(
                        withStatus(HttpStatus.OK)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(objectMapper.writeValueAsString(new String[]{expected})));

        MockHttpServletResponse response =
                mockMvc.perform(get("/v1/revocation-list"))
                        .andExpect(status().is2xxSuccessful())
                        .andReturn()
                        .getResponse();

        mockServer.verify();
        assertNotNull(response);
        RevocationResponse revocationList =
                objectMapper.readValue(response.getContentAsString(), RevocationResponse.class);
        assertNotNull(revocationList.getRevokedCerts());
        assertEquals(1, revocationList.getRevokedCerts().size());
        assertEquals(expected, revocationList.getRevokedCerts().get(0));
    }
}
