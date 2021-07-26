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
import net.javacrumbs.shedlock.core.LockAssert;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT10M")
public class SchedulingConfig {

    private static final Logger logger = LoggerFactory.getLogger(SchedulingConfig.class);

    private final RevocationListSyncer revocationListSyncer;

    public SchedulingConfig(RevocationListSyncer revocationListSyncer) {
        this.revocationListSyncer = revocationListSyncer;
    }

    // Sync revocation list on start up
    @Scheduled(fixedDelay = Long.MAX_VALUE, initialDelay = 0)
    @SchedulerLock(name = "revocation_list_sync", lockAtLeastFor = "PT15S")
    public void syncRevocationListOnStartup() {
        LockAssert.assertLocked();
        revocationListSyncer.updateRevokedCerts();
    }

    // Sync revocation list every 5 minutes from the full hour (default)
    @Scheduled(cron = "${revocationList.sync.cron:0 0/5 * ? * *}")
    @SchedulerLock(name = "revocation_list_sync", lockAtLeastFor = "PT15S")
    public void syncRevocationList() {
        LockAssert.assertLocked();
        revocationListSyncer.updateRevokedCerts();
    }
}
