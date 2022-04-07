package ch.admin.bag.covidcertificate.backend.verifier.ws.controller;

import ch.admin.bag.covidcertificate.backend.verifier.ws.security.signature.JwsMessageConverter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

@TestInstance(Lifecycle.PER_CLASS)
public class ForeignRulesControllerV2JWSTest extends ForeignRulesControllerV2Test{

    @BeforeAll
    public void setup(){
        this.acceptMediaType = JwsMessageConverter.JWS_MEDIA_TYPE;
    }
}
