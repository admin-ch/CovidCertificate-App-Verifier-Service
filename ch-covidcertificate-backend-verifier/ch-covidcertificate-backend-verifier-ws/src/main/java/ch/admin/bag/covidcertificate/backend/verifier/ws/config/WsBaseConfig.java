/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.backend.verifier.ws.config;

import ch.admin.bag.covidcertificate.backend.verifier.data.VerifierDataService;
import ch.admin.bag.covidcertificate.backend.verifier.data.impl.JdbcVerifierDataServiceImpl;
import ch.admin.bag.covidcertificate.backend.verifier.ws.controller.KeyController;
import ch.admin.bag.covidcertificate.backend.verifier.ws.controller.RevocationListController;
import ch.admin.bag.covidcertificate.backend.verifier.ws.controller.VerificationRulesController;
import ch.admin.bag.covidcertificate.backend.verifier.ws.interceptor.HeaderInjector;
import ch.admin.bag.covidcertificate.backend.verifier.ws.security.signature.JwsMessageConverter;
import ch.admin.bag.covidcertificate.backend.verifier.ws.utils.CacheUtil;
import ch.admin.bag.covidcertificate.backend.verifier.ws.utils.RestTemplateHelper;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public abstract class WsBaseConfig implements WebMvcConfigurer {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @Value(
            "#{${ws.security.headers: {'X-Content-Type-Options':'nosniff', 'X-Frame-Options':'DENY','X-Xss-Protection':'1; mode=block'}}}")
    Map<String, String> additionalHeaders;

    @Value("${revocationList.baseurl}")
    String revokedCertsBaseUrl;

    @Value("${ws.jws.p12:}")
    public String p12KeyStore;

    @Value("${ws.jws.password:}")
    public String p12KeyStorePassword;

    public abstract DataSource dataSource();

    public abstract Flyway flyway();

    @Value("${ws.revocationList.max-age:PT1M}")
    public void setRevocationListMaxAge(Duration maxAge) {
        CacheUtil.REVOCATION_LIST_MAX_AGE = maxAge;
    }

    @Value("${ws.verificationRules.max-age:PT1M}")
    public void setVerificationRulesMaxAge(Duration maxAge) {
        CacheUtil.VERIFICATION_RULES_MAX_AGE = maxAge;
    }

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        try {
            converters.add(
                    new JwsMessageConverter(jwsKeyStore(), p12KeyStorePassword.toCharArray()));
        } catch (KeyStoreException
                | NoSuchAlgorithmException
                | CertificateException
                | IOException
                | UnrecoverableKeyException e) {
            logger.error("Could not load key store", e);
            throw new RuntimeException("Could not add jws Converter");
        }
    }

    public KeyStore jwsKeyStore()
            throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        var keyStore = KeyStore.getInstance("pkcs12");
        var bais = new ByteArrayInputStream(Base64.getDecoder().decode(p12KeyStore));
        keyStore.load(bais, p12KeyStorePassword.toCharArray());
        return keyStore;
    }

    @Bean
    public HeaderInjector securityHeaderInjector() {
        return new HeaderInjector(additionalHeaders);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(securityHeaderInjector());
    }

    @Bean
    public VerifierDataService verifierDataService(DataSource dataSource) {
        return new JdbcVerifierDataServiceImpl(dataSource);
    }

    @Bean
    public KeyController keyController(VerifierDataService verifierDataService) {
        return new KeyController(verifierDataService);
    }

    @Bean
    public RevocationListController revocationListController() {
        return new RevocationListController(revokedCertsBaseUrl);
    }

    @Bean
    public VerificationRulesController verificationRulesController() throws IOException {
        return new VerificationRulesController();
    }

    @Bean
    public RestTemplate restTemplate() {
        return RestTemplateHelper.getRestTemplate();
    }
}
