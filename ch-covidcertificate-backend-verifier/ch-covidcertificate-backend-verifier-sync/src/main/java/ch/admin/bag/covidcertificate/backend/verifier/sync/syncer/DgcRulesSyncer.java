// Copyright (c) 2021 Patrick Amrein <amrein@ubique.ch>
//
// This software is released under the MIT License.
// https://opensource.org/licenses/MIT

package ch.admin.bag.covidcertificate.backend.verifier.sync.syncer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
            List<String> uploadedRuleIds = dgcRulesClient.upload(payload);
            var end = Instant.now();
            logger.info(
                    "Successfully Uploaded rules {} in {} ms",
                    uploadedRuleIds,
                    end.toEpochMilli() - start.toEpochMilli());
        } catch (Exception e) {
            logger.error("rules sync failed.", e);
        }
    }
}
