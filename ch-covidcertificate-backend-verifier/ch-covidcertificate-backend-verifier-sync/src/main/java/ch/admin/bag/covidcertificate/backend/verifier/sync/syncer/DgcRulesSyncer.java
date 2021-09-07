// Copyright (c) 2021 Patrick Amrein <amrein@ubique.ch>
// 
// This software is released under the MIT License.
// https://opensource.org/licenses/MIT

package ch.admin.bag.covidcertificate.backend.verifier.sync.syncer;

import java.time.Instant;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.admin.bag.covidcertificate.backend.verifier.sync.exception.ValueSetFormatException;

public class DgcRulesSyncer {
    private static final Logger logger = LoggerFactory.getLogger(DgcRulesSyncer.class);
    private final String payload;
    private final DgcValueSetClient dgcValueSetClient;

    public DgcRulesSyncer(String payload, DgcValueSetClient dgcValueSetClient) {
        this.payload = payload;
        this.dgcValueSetClient = dgcValueSetClient;
    }

    public void sync() {
        logger.info("Start rules sync with DGC Gateway");
        var start = Instant.now();
        try {
          
        } catch (Exception e) {
            logger.error("rules sync failed.", e);
        }
    }
}
