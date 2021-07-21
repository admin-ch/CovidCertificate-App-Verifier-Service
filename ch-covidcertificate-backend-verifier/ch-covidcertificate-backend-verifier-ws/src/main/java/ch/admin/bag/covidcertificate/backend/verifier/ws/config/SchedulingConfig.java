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

import ch.admin.bag.covidcertificate.backend.verifier.ws.client.RevocationListSyncer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
public class SchedulingConfig {

    private static final Logger logger = LoggerFactory.getLogger(SchedulingConfig.class);

    private final RevocationListSyncer revocationListSyncer;

    public SchedulingConfig(RevocationListSyncer revocationListSyncer) {
        this.revocationListSyncer = revocationListSyncer;
    }

    // Sync revocation list every full hour (default)
    @Scheduled(cron = "${revocationList.sync.cron:0 0 * ? * *}")
    public void syncRevocationList() {
        revocationListSyncer.updateRevokedCerts();
    }
}
