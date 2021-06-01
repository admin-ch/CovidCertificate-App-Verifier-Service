package ch.admin.bag.covidcertificate.backend.verifier.ws.controller;

import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.admin.bag.covidcertificate.backend.verifier.ws.controller.RevocationListController.RevocationResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;

class RevocationListControllerTest extends BaseControllerTest {
    @Autowired RevocationListController revocationListController;

    @Test
    public void getCertsTest() throws Exception {
        MockHttpServletResponse response =
                mockMvc.perform(get("/v1/revocation-list"))
                        .andExpect(status().is2xxSuccessful())
                        .andReturn()
                        .getResponse();
        assertNotNull(response);
        RevocationResponse revocationList =
                objectMapper.readValue(response.getContentAsString(), RevocationResponse.class);
        assertNotNull(revocationList.getRevokedCerts());
    }
}
