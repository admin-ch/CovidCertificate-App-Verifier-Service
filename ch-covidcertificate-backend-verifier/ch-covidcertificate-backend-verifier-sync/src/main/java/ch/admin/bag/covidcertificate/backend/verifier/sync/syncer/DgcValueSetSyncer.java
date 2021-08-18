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

import ch.admin.bag.covidcertificate.backend.verifier.data.ValueSetDataService;
import ch.admin.bag.covidcertificate.backend.verifier.sync.exception.ValueSetFormatException;
import java.time.Instant;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DgcValueSetSyncer {

    private static final Logger logger = LoggerFactory.getLogger(DgcValueSetSyncer.class);
    private final ValueSetDataService valueSetDataService;
    private final DgcValueSetClient dgcValueSetClient;

    public DgcValueSetSyncer(
            ValueSetDataService valueSetDataService, DgcValueSetClient dgcValueSetClient) {
        this.valueSetDataService = valueSetDataService;
        this.dgcValueSetClient = dgcValueSetClient;
    }

    public void sync() {
        logger.info("Start value set sync with DGC Gateway");
        var start = Instant.now();
        try {
            Map<String, String> valueSetsById = dgcValueSetClient.download();
            valueSetDataService.insertValueSets(valueSetsById);
            var end = Instant.now();
            logger.info(
                    "Finished syncing {} value sets in {} ms",
                    valueSetsById.size(),
                    end.toEpochMilli() - start.toEpochMilli());
        } catch (ValueSetFormatException e) {
            logger.error("received unexpected value set format", e);
        } catch (Exception e) {
            logger.error("value set sync failed.", e);
        }
    }
}
