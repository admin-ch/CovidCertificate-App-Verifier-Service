package ch.admin.bag.covidcertificate.backend.verifier.ws.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.admin.bag.covidcertificate.backend.verifier.ws.util.TestHelper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;

public abstract class VerificationRulesControllerTest extends BaseControllerTest {

    protected MediaType acceptMediaType;

    private String verificationRulesUrl = "/trust/v1/verificationRules";

    @Test
    public void verificationRulesTest() throws Exception {
        // get verification rules
        MockHttpServletResponse response =
                mockMvc.perform(get(verificationRulesUrl).accept(acceptMediaType))
                        .andExpect(status().is2xxSuccessful())
                        .andReturn()
                        .getResponse();

        // verify response
        assertNotNull(response);
        Map revocationList =
                testHelper.verifyAndReadValue(
                        response, acceptMediaType, TestHelper.PATH_TO_CA_PEM, Map.class);
        Map expected =
                testHelper
                        .getObjectMapper()
                        .readValue(
                                new ClassPathResource("verificationRules.json").getFile(),
                                Map.class);
        assertEquals(
                testHelper.getObjectMapper().writeValueAsString(expected),
                testHelper.getObjectMapper().writeValueAsString(revocationList));
    }

    @Override
    protected String getUrlForSecurityHeadersTest() {
        return verificationRulesUrl;
    }

    @Override
    protected MediaType getSecurityHeadersRequestMediaType() {
        return MediaType.APPLICATION_JSON;
    }
}
