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

import ch.admin.bag.covidcertificate.backend.verifier.sync.syncer.DGCSyncer;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
public class SyncSchedulingBaseConfig {

    private final DGCSyncer dgcSyncer;

    public SyncSchedulingBaseConfig(DGCSyncer dgcSyncer) {
        this.dgcSyncer = dgcSyncer;
    }

    @Scheduled(cron = "${dgc.sync.cron}")
    public void dgcSyncCron() {
        dgcSyncer.sync();
    }

    @Scheduled(fixedRate = Long.MAX_VALUE, initialDelay = 0)
    public void dgcSyncOnStartup() {
        dgcSyncer.sync();
    }
}
