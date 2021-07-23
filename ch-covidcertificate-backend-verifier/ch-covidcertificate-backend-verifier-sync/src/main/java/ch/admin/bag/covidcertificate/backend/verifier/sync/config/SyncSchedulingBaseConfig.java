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

import ch.admin.bag.covidcertificate.backend.verifier.sync.syncer.DgcSyncer;
import net.javacrumbs.shedlock.core.LockAssert;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT10M")
public class SyncSchedulingBaseConfig {

    private final DgcSyncer dgcSyncer;

    public SyncSchedulingBaseConfig(DgcSyncer dgcSyncer) {
        this.dgcSyncer = dgcSyncer;
    }

    @Scheduled(cron = "${dgc.sync.cron}")
    @SchedulerLock(name = "DGC_download", lockAtLeastFor = "PT15S")
    public void dgcSyncCron() {
        LockAssert.assertLocked();
        dgcSyncer.sync();
    }

    @Scheduled(fixedRate = Long.MAX_VALUE, initialDelay = 0)
    @SchedulerLock(name = "DGC_download", lockAtLeastFor = "PT15S")
    public void dgcSyncOnStartup() {
        LockAssert.assertLocked();
        dgcSyncer.sync();
    }
}
