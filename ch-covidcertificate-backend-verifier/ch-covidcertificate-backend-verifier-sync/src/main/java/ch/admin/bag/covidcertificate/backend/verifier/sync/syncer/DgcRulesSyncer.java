/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.backend.verifier.sync.syncer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.admin.bag.covidcertificate.backend.verifier.sync.syncer.model.RulesSyncResult;

public class DgcRulesSyncer {
    private static final Logger logger = LoggerFactory.getLogger(DgcRulesSyncer.class);
    private final JsonNode payload;
    private final DgcRulesClient dgcRulesClient;

    public DgcRulesSyncer(String payload, DgcRulesClient dgcRulesClient)
            throws JsonProcessingException {
        var mapper = new ObjectMapper();
        var payloadObject = mapper.readTree(payload);
        this.payload = payloadObject;
        this.dgcRulesClient = dgcRulesClient;
    }

    public void sync() {
        logger.info("Start rules sync with DGC Gateway");
        var start = Instant.now();
        try {
            RulesSyncResult uploadedRuleIds = dgcRulesClient.upload(payload);
            var end = Instant.now();
            logger.info(
                    "Successfully Uploaded rules {} in {} ms",
                    uploadedRuleIds.getSuccesssfullRules(),
                    end.toEpochMilli() - start.toEpochMilli());
            if (uploadedRuleIds.getFailedRules().size() > 0) {
                logger.error("Failed to Upload rules {}", uploadedRuleIds.getFailedRules());
            }
        } catch (Exception e) {
            logger.error("rules sync failed.", e);
        }
    }
}
