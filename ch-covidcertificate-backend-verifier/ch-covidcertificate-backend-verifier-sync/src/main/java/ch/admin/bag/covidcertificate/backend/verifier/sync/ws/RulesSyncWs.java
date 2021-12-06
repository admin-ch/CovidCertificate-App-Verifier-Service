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
import ch.admin.bag.covidcertificate.backend.verifier.sync.syncer.model.RulesSyncResult;
import ch.ubique.openapi.docannotations.Documentation;
import net.javacrumbs.shedlock.core.DefaultLockingTaskExecutor;
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

    public RulesSyncWs(DgcRulesSyncer rulesSyncer) {
        this.rulesSyncer = rulesSyncer;
    }

    @Documentation(description = "Echo endpoint", responses = { "200 => Hello from internal rules sync WS" })
    @GetMapping(value = "")
    public @ResponseBody String hello() {
        return "Hello from internal rules sync WS";
    }

    @Documentation(description = "internal endpoint for triggering rules upload")
    @GetMapping(value = "trigger")
    public @ResponseBody ResponseEntity<RulesSyncResult> triggerRulesUpload() {
        String msg = "rules upload triggered";
        var rules = rulesSyncer.sync();
        logger.info(msg);
        return ResponseEntity.ok(rules);
    }
}
