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

import ch.admin.bag.covidcertificate.backend.verifier.data.AppTokenDataService;
import ch.admin.bag.covidcertificate.backend.verifier.ws.config.model.ApiKeyConfig;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Profile("app-token-db")
@Configuration
@EnableScheduling
public class AppTokenDbConfig {

    private static final Logger logger = LoggerFactory.getLogger(AppTokenDbConfig.class);
    private final ApiKeyConfig apiKeyConfig;
    private final AppTokenDataService appTokenDataService;

    public AppTokenDbConfig(ApiKeyConfig apiKeyConfig, AppTokenDataService appTokenDataService) {
        this.apiKeyConfig = apiKeyConfig;
        this.appTokenDataService = appTokenDataService;
    }

    // Refresh app tokens from DB every 5min
    @PostConstruct
    @Scheduled(cron = "${ws.authentication.cron:0 0/5 * ? * *}")
    public void updateAppTokens() {
        logger.info("Updating app tokens");
        final var appTokens = appTokenDataService.getAppTokens();
        final Map<String, String> apiKeys = new HashMap<>();
        for (var token : appTokens) {
            apiKeys.put(token.getDescription(), token.getApiKey());
        }
        apiKeyConfig.setApiKeys(apiKeys);
    }
}
