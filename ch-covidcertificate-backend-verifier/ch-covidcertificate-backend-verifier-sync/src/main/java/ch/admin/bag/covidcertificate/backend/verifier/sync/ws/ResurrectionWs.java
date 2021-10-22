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

import ch.admin.bag.covidcertificate.backend.verifier.data.VerifierDataService;
import ch.admin.bag.covidcertificate.backend.verifier.model.sync.DscRestoreResponse;
import ch.ubique.openapi.docannotations.Documentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("resurrection")
public class ResurrectionWs {
    private static final Logger logger = LoggerFactory.getLogger(ResurrectionWs.class);
    private final VerifierDataService verifierDataService;

    public ResurrectionWs(VerifierDataService verifierDataService) {
        this.verifierDataService = verifierDataService;
    }

    @Documentation(
            description = "Echo endpoint",
            responses = {"200 => Hello from internal resurrection WS"})
    @GetMapping(value = "")
    public @ResponseBody String hello() {
        return "Hello from internal resurrection WS";
    }

    @Documentation(description = "internal endpoint for triggering deleted dsc restore")
    @GetMapping(value = "dsc")
    public @ResponseBody ResponseEntity<String> triggerDeletedDscRestore() {
        DscRestoreResponse dscRestoreResponse = verifierDataService.restoreDeletedDscs();
        final String msg =
                String.format(
                        "restored %d dscs and %d cscas that were marked for deletion",
                        dscRestoreResponse.getRestoredDscCount(),
                        dscRestoreResponse.getRestoredCscaCount());
        logger.info(msg);
        return ResponseEntity.ok(msg);
    }
}
