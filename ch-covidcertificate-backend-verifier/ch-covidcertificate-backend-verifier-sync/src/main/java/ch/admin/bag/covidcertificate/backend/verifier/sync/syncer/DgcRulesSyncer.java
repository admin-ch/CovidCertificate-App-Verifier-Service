// Copyright (c) 2021 Patrick Amrein <amrein@ubique.ch>
// 
// This software is released under the MIT License.
// https://opensource.org/licenses/MIT

package ch.admin.bag.covidcertificate.backend.verifier.sync.syncer;

import java.time.Instant;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DgcRulesSyncer {
    private static final Logger logger = LoggerFactory.getLogger(DgcRulesSyncer.class);
    private final JsonNode payload;
    private final DgcRulesClient dgcRulesClient;

    public DgcRulesSyncer(String payload, DgcRulesClient dgcRulesClient) throws JsonMappingException, JsonProcessingException {
        var mapper = new ObjectMapper();
        var payloadObject = mapper.readTree(payload);
        this.payload = payloadObject;
        this.dgcRulesClient = dgcRulesClient;
    }

    public void sync() {
        logger.info("Start rules sync with DGC Gateway");
        var start = Instant.now();
        try {
            this.dgcRulesClient.upload(this.payload);
            var end = Instant.now();
            logger.info("Uploaded all rules successfully in {} ms", end.toEpochMilli() - start.toEpochMilli());
        } catch (Exception e) {
            logger.error("rules sync failed.", e);
        }
    }
}
