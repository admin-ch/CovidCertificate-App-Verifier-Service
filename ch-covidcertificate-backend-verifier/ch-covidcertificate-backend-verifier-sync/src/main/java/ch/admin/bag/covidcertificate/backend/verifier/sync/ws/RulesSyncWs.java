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

import ch.admin.bag.covidcertificate.backend.verifier.sync.syncer.DgcRulesSyncer;
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
@RequestMapping("rules/sync")
public class RulesSyncWs {
    private static final Logger logger = LoggerFactory.getLogger(RulesSyncWs.class);
    private final DgcRulesSyncer rulesSyncer;
    private final DefaultLockingTaskExecutor executor;

    public RulesSyncWs(DgcRulesSyncer rulesSyncer, LockProvider lockProvider) {
        this.rulesSyncer = rulesSyncer;
        this.executor = new DefaultLockingTaskExecutor(lockProvider);
    }

    @Documentation(
            description = "Echo endpoint",
            responses = {"200 => Hello from internal rules sync WS"})
    @GetMapping(value = "")
    public @ResponseBody String hello() {
        return "Hello from internal rules sync WS";
    }

    @Documentation(description = "internal endpoint for triggering rules upload")
    @GetMapping(value = "trigger")
    public @ResponseBody ResponseEntity<String> triggerRulesUpload() {
        String msg = "rules upload triggered";
        rulesSyncManual();
        logger.info(msg);
        return ResponseEntity.ok(msg);
    }

    public void rulesSyncManual() {
        final String name = "RULES_upload";
        final Duration lockAtMostFor = Duration.ofMinutes(10);
        final Duration lockAtLeastFor = Duration.ofSeconds(15);
        executor.executeWithLock(
                new Runnable() {
                    @Override
                    public void run() {
                        LockAssert.assertLocked();
                        rulesSyncer.sync();
                    }
                },
                new LockConfiguration(Instant.now(), name, lockAtMostFor, lockAtLeastFor));
    }
}
