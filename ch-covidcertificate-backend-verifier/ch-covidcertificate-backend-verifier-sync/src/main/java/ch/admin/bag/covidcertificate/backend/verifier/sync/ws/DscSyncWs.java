/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.backend.verifier.sync.ws;

import ch.admin.bag.covidcertificate.backend.verifier.sync.exception.DgcSyncException;
import ch.admin.bag.covidcertificate.backend.verifier.sync.syncer.DgcCertSyncer;
import ch.ubique.openapi.docannotations.Documentation;
import java.time.Duration;
import java.time.Instant;
import net.javacrumbs.shedlock.core.DefaultLockingTaskExecutor;
import net.javacrumbs.shedlock.core.LockAssert;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("dsc/sync")
public class DscSyncWs {
    private static final Logger logger = LoggerFactory.getLogger(DscSyncWs.class);
    private final DgcCertSyncer dgcSyncer;
    private final DefaultLockingTaskExecutor executor;

    public DscSyncWs(DgcCertSyncer dgcSyncer, LockProvider lockProvider) {
        this.dgcSyncer = dgcSyncer;
        this.executor = new DefaultLockingTaskExecutor(lockProvider);
    }

    @Documentation(
            description = "Echo endpoint",
            responses = {"200 => Hello from internal dsc sync WS"})
    @GetMapping(value = "")
    public @ResponseBody String hello() {
        return "Hello from internal DSC sync WS";
    }

    @Documentation(description = "internal endpoint for triggering dsc download")
    @GetMapping(value = "trigger")
    public @ResponseBody ResponseEntity<String> triggerDscDownload() {
        String msg = "dgc sync triggered";
        dgcSyncManual();
        logger.info(msg);
        return ResponseEntity.ok(msg);
    }

    public void dgcSyncManual() {
        final String name = "DGC_download";
        final Duration lockAtMostFor = Duration.ofMinutes(10);
        final Duration lockAtLeastFor = Duration.ofSeconds(15);
        executor.executeWithLock(
                new Runnable() {
                    @Override
                    public void run() {
                        LockAssert.assertLocked();
                        try {
                            dgcSyncer.sync();
                        } catch (DgcSyncException e) {
                            logger.error("{}", DgcSyncException.EXCEPTION_TAG, e.getInnerException());
                        } catch (Exception e) {
                            logger.error("{}", DgcSyncException.EXCEPTION_TAG, e);
                        }
                    }
                },
                new LockConfiguration(Instant.now(), name, lockAtMostFor, lockAtLeastFor));
    }
}
