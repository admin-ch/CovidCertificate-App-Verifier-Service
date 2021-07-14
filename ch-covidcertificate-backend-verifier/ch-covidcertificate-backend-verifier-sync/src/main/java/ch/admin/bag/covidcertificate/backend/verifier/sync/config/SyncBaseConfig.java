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

import ch.admin.bag.covidcertificate.backend.verifier.data.VerifierDataService;
import ch.admin.bag.covidcertificate.backend.verifier.data.impl.JdbcVerifierDataServiceImpl;
import ch.admin.bag.covidcertificate.backend.verifier.sync.syncer.DgcClient;
import ch.admin.bag.covidcertificate.backend.verifier.sync.syncer.DgcSyncer;
import ch.admin.bag.covidcertificate.backend.verifier.sync.utils.RestTemplateHelper;
import javax.sql.DataSource;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
    public VerifierDataService verifierDataService(DataSource dataSource) {
        return new JdbcVerifierDataServiceImpl(dataSource);
    }

    @Bean
    public DgcClient dgcClient(RestTemplate restTemplate) {
        return new DgcClient(baseurl, restTemplate);
    }

    @Bean
    public DgcSyncer dgcSyncer(DgcClient dgcClient, VerifierDataService verifierDataService) {
        return new DgcSyncer(dgcClient, verifierDataService);
    }
}
