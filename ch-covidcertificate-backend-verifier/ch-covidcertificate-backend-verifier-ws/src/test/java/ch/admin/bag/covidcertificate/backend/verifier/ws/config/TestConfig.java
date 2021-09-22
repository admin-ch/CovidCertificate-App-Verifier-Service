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

import ch.admin.bag.covidcertificate.backend.verifier.data.RevokedCertDataService;
import ch.admin.bag.covidcertificate.backend.verifier.data.impl.JdbcRevokedCertDataServiceImpl;
import ch.admin.bag.covidcertificate.backend.verifier.data.util.CacheUtil;
import ch.admin.bag.covidcertificate.backend.verifier.ws.controller.RevocationListController;
import ch.admin.bag.covidcertificate.backend.verifier.ws.controller.RevocationListControllerV2;
import ch.admin.bag.covidcertificate.backend.verifier.ws.utils.RestTemplateHelper;
import java.time.Duration;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestTemplate;

@Profile("test")
@Configuration
public class TestConfig extends WsBaseConfig {
    private static final Logger logger = LoggerFactory.getLogger(TestConfig.class);

    @Value("${revocationList.baseurl}")
    String baseurl = "https://covidcertificate-management-d.bag.admin.ch/api";

    @Autowired DataSource dataSource;

    @Override
    public DataSource dataSource() {
        return dataSource;
    }

    @Bean
    @Override
    public Flyway flyway() {
        final var flyway =
                Flyway.configure()
                        .dataSource(dataSource)
                        .locations("classpath:/db/migration/pgsql")
                        .validateOnMigrate(true)
                        .load();
        flyway.migrate();
        return flyway;
    }

    @Bean
    public RevocationListController revocationListController() {
        return new RevocationListController(baseurl);
    }

    @Bean
    public RestTemplate restTemplate() {
        logger.info("Instantiated RevocationListController with baseurl: {}", baseurl);
        return RestTemplateHelper.getRestTemplate();
    }

    @Bean
    public RevokedCertDataService revokedCertDataService(
            DataSource dataSource,
            @Value("${revocationList.batch-size:50}") Integer revokedCertBatchSize) {
        return new JdbcRevokedCertDataServiceImpl(dataSource, revokedCertBatchSize);
    }

    @Value("${ws.revocation-list.retention-bucket-duration:PT6H}")
    public void setRevocationRetentionBucketDuration(Duration bucketDuration) {
        CacheUtil.REVOCATION_RETENTION_BUCKET_DURATION = bucketDuration;
    }

    @Bean
    public RevocationListControllerV2 revocationListControllerV2(
            RevokedCertDataService revokedCertDataService) {
        return new RevocationListControllerV2(revokedCertDataService);
    }
}
