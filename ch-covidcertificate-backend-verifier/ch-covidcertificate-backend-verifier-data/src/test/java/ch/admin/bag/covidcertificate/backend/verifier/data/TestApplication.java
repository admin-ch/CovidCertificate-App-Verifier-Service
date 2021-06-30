package ch.admin.bag.covidcertificate.backend.verifier.data;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;

@SpringBootApplication(
        scanBasePackages = {"ch.admin.bag.covidcertificate.backend.verifier.data"},
        exclude = {SecurityAutoConfiguration.class})
public class TestApplication {}
