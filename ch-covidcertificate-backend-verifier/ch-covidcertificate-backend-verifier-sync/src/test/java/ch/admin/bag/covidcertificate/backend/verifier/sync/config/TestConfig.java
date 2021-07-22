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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestTemplate;

@Profile("test")
@Configuration
public class TestConfig {
    private static final Logger logger = LoggerFactory.getLogger(TestConfig.class);

    @Value("${dgc.baseurl}")
    String baseurl = "https://testurl.europa.eu";

    @Value("${ws.keys.batch-size:1000}")
    protected Integer dscBatchSize;

    @Bean
    public DgcSyncer dgcSyncer(DgcClient dgcClient, VerifierDataService verifierDataService) {
        logger.info("Instantiated DGC Syncer with baseurl: {}", baseurl);
        return new DgcSyncer(dgcClient, verifierDataService);
    }

    @Bean
    public DgcClient dgcClient(RestTemplate restTemplate) {
        logger.info("Instantiated DGC Syncer with baseurl: {}", baseurl);
        return new DgcClient(baseurl, restTemplate);
    }

    @Bean
    public VerifierDataService verifierDataService(DataSource dataSource) {
        return new JdbcVerifierDataServiceImpl(dataSource, dscBatchSize);
    }

    @Bean
    public RestTemplate restTemplate() {
        return RestTemplateHelper.getRestTemplate();
    }
}
