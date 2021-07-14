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

import ch.admin.bag.covidcertificate.backend.verifier.ws.controller.RevocationListController;
import ch.admin.bag.covidcertificate.backend.verifier.ws.utils.RestTemplateHelper;
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
}
