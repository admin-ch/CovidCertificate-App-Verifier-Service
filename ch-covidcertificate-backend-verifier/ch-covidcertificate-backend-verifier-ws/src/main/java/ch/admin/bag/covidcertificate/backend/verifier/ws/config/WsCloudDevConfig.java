package ch.admin.bag.covidcertificate.backend.verifier.ws.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("cloud-dev")
public class WsCloudDevConfig extends WsCloudBaseConfig {}
