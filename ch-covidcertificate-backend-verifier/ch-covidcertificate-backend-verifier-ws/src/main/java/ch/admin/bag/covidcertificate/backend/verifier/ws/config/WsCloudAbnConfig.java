package ch.admin.bag.covidcertificate.backend.verifier.ws.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("cloud-abn")
public class WsCloudAbnConfig extends WsCloudBaseConfig {}
