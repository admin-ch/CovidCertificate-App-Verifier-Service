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

import ch.admin.bag.covidcertificate.backend.verifier.data.ValueSetDataService;
import ch.admin.bag.covidcertificate.backend.verifier.data.VerifierDataService;
import ch.admin.bag.covidcertificate.backend.verifier.model.exception.DgcSyncException;
import ch.admin.bag.covidcertificate.backend.verifier.sync.syncer.DgcCertSyncer;
import ch.admin.bag.covidcertificate.backend.verifier.sync.syncer.DgcValueSetSyncer;
import net.javacrumbs.shedlock.core.LockAssert;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT10M")
public class SyncSchedulingBaseConfig {

    private static final Logger logger = LoggerFactory.getLogger(SyncSchedulingBaseConfig.class);
    private final DgcCertSyncer dgcSyncer;
    private final DgcValueSetSyncer dgcValueSetSyncer;
    private final ValueSetDataService valueSetDataService;
    private final VerifierDataService verifierDataService;
    private final boolean syncCronEnabled;

    public SyncSchedulingBaseConfig(
            DgcCertSyncer dgcSyncer,
            DgcValueSetSyncer dgcValueSetSyncer,
            ValueSetDataService valueSetDataService,
            VerifierDataService verifierDataService,
            @Value("${dgc.sync.cron.enable:true}") boolean syncCronEnabled) {
        this.dgcSyncer = dgcSyncer;
        this.dgcValueSetSyncer = dgcValueSetSyncer;
        this.valueSetDataService = valueSetDataService;
        this.verifierDataService = verifierDataService;
        this.syncCronEnabled = syncCronEnabled;
    }

    @Scheduled(cron = "${dgc.sync.cron}")
    @SchedulerLock(name = "DGC_download", lockAtLeastFor = "PT15S")
    public void dgcSyncCron() {
        if (syncCronEnabled) {
            LockAssert.assertLocked();
            try {
                dgcSyncer.sync();
            } catch (DgcSyncException e) {
                logger.error("{}", DgcSyncException.EXCEPTION_TAG, e.getInnerException());
            } catch (Exception e) {
                logger.error("{}", DgcSyncException.EXCEPTION_TAG, e);
            }
        }
    }

    @Scheduled(cron = "${dsc.deleted.clean.cron:-}")
    @SchedulerLock(name = "DSC_clean", lockAtLeastFor = "PT15S")
    public void cleanUpDeletedDscs() {
        int removedCount = verifierDataService.cleanUpDscsMarkedForDeletion();
        logger.info(
                "removed {} dscs marked for deletion for over {}",
                removedCount,
                verifierDataService.getKeepDscsMarkedForDeletionDuration());
    }

    @Scheduled(cron = "${value-set.clean.cron:0 0 1 ? * *}")
    @SchedulerLock(name = "value_set_clean", lockAtLeastFor = "PT15S")
    public void valueSetCleanCron() {
        LockAssert.assertLocked();
        valueSetDataService.deleteOldValueSets();
    }

    @Scheduled(cron = "${value-set.sync.cron:0 0 0 ? * *}")
    @SchedulerLock(name = "value_set_sync", lockAtLeastFor = "PT15S")
    public void valueSetSyncCron() {
        LockAssert.assertLocked();
        dgcValueSetSyncer.sync();
    }

    @Scheduled(fixedRate = Long.MAX_VALUE, initialDelay = 0)
    @SchedulerLock(name = "value_set_sync", lockAtLeastFor = "PT15S")
    public void valueSetSyncOnStartup() {
        LockAssert.assertLocked();
        dgcValueSetSyncer.sync();
    }
}
