/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.backend.verifier.sync.config;

import ch.admin.bag.covidcertificate.backend.verifier.sync.syncer.DGCClient;
import ch.admin.bag.covidcertificate.backend.verifier.sync.utils.RestTemplateHelper;
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

    @Bean
    public DGCClient dgcSyncer(RestTemplate restTemplate) {
        logger.info("Instantiated DGC Syncer with baseurl: {}", baseurl);
        return new DGCClient(baseurl, restTemplate);
    }

    @Bean
    public RestTemplate restTemplate() {
        final var rt = RestTemplateHelper.getRestTemplate();
        if (rt == null) {
            logger.error("Couldn't instantiate rt bean");
        }
        return rt;
    }
}
