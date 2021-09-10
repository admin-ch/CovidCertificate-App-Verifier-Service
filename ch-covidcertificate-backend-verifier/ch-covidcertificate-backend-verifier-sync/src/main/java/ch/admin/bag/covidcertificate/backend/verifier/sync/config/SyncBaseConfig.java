/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.backend.verifier.sync.config;

import ch.admin.bag.covidcertificate.backend.verifier.data.ValueSetDataService;
import ch.admin.bag.covidcertificate.backend.verifier.data.VerifierDataService;
import ch.admin.bag.covidcertificate.backend.verifier.data.impl.JdbcValueSetDataServiceImpl;
import ch.admin.bag.covidcertificate.backend.verifier.data.impl.JdbcVerifierDataServiceImpl;
import ch.admin.bag.covidcertificate.backend.verifier.sync.syncer.DgcCertClient;
import ch.admin.bag.covidcertificate.backend.verifier.sync.syncer.DgcCertSyncer;
import ch.admin.bag.covidcertificate.backend.verifier.sync.syncer.DgcRulesClient;
import ch.admin.bag.covidcertificate.backend.verifier.sync.syncer.DgcRulesSyncer;
import ch.admin.bag.covidcertificate.backend.verifier.sync.syncer.DgcValueSetClient;
import ch.admin.bag.covidcertificate.backend.verifier.sync.syncer.DgcValueSetSyncer;
import ch.admin.bag.covidcertificate.backend.verifier.sync.utils.RestTemplateHelper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import javax.sql.DataSource;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestTemplate;

@Configuration
public abstract class SyncBaseConfig {

    @Value("${dgc.baseurl}")
    String baseurl;

    @Value("${dgc.clientcert}")
    String authClientCert;

    @Value("${dgc.clientcert.password}")
    String authClientCertPassword;

    @Value("${sign.baseurl}")
    String signBaseUrl;

    @Value("${sign.cc-signing-service.key-store}")
    private String keyStore;

    @Value("${sign.cc-signing-service.key-store-password}")
    private String keyStorePassword;

    @Value("${sign.cc-signing-service.key-alias}")
    private String keyAlias;

    @Value("${sign.cc-signing-service.key-password}")
    private String keyPassword;

    @Value("${sign.cc-signing-service.trust-store}")
    private String trustStore;

    @Value("${sign.cc-signing-service.trust-store-password}")
    private String trustStorePassword;

    @Value("${ws.keys.batch-size:1000}")
    protected Integer dscBatchSize;

    public abstract DataSource dataSource();

    public abstract Flyway flyway();

    public abstract String getDbType();

    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(
                JdbcTemplateLockProvider.Configuration.builder()
                        .withTableName("t_shedlock")
                        .withJdbcTemplate(new JdbcTemplate(dataSource))
                        // Works on Postgres, MySQL, MariaDb, MS SQL, Oracle, DB2, HSQL and H2
                        .usingDbTime()
                        .build());
    }

    @Bean
    public RestTemplate restTemplate() {
        return RestTemplateHelper.getRestTemplateWithClientCerts(
                authClientCert, authClientCertPassword);
    }

    @Bean
    public RestTemplate signRestTemplate()
            throws UnrecoverableKeyException, KeyManagementException, CertificateException,
                    NoSuchAlgorithmException, KeyStoreException, IOException {
        return RestTemplateHelper.signingServiceRestTemplate(
                trustStore, keyStore, trustStorePassword, keyStorePassword, keyPassword, keyAlias);
    }

    @Bean
    public VerifierDataService verifierDataService(DataSource dataSource) {
        return new JdbcVerifierDataServiceImpl(dataSource, dscBatchSize);
    }

    @Bean
    public DgcCertClient dgcClient(RestTemplate restTemplate) {
        return new DgcCertClient(baseurl, restTemplate);
    }

    @Bean
    public DgcCertSyncer dgcSyncer(
            DgcCertClient dgcClient, VerifierDataService verifierDataService) {
        return new DgcCertSyncer(dgcClient, verifierDataService);
    }

    @Bean
    public ValueSetDataService valueSetDataService(
            DataSource dataSource, @Value("${value-set.max-history:10}") int maxHistory) {
        return new JdbcValueSetDataServiceImpl(dataSource, maxHistory);
    }

    @Bean
    public DgcValueSetClient dgcValueSetClient(RestTemplate restTemplate) {
        return new DgcValueSetClient(baseurl, restTemplate);
    }

    @Bean
    public DgcValueSetSyncer dgcValueSetSyncer(
            ValueSetDataService valueSetDataService, DgcValueSetClient dgcValueSetClient) {
        return new DgcValueSetSyncer(valueSetDataService, dgcValueSetClient);
    }

    @Bean
    public DgcRulesClient dgcRulesClient(RestTemplate restTemplate, RestTemplate signRestTemplate) {
        return new DgcRulesClient(baseurl, restTemplate, signBaseUrl, signRestTemplate);
    }

    @Bean
    public DgcRulesSyncer dgcRulesSyncer(DgcRulesClient dgcRulesClient) throws IOException {
        var verificationRulesUploadString =
                new String(
                        new ClassPathResource("verificationRulesUpload.json")
                                .getInputStream()
                                .readAllBytes(),
                        StandardCharsets.UTF_8);
        return new DgcRulesSyncer(verificationRulesUploadString, dgcRulesClient);
    }
}
