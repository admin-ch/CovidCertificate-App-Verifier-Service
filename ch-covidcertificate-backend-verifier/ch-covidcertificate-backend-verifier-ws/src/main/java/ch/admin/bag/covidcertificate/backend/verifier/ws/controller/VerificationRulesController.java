/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.backend.verifier.ws.controller;

import ch.admin.bag.covidcertificate.backend.verifier.ws.utils.CacheUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("trust/v1")
public class VerificationRulesController {

    private static final Logger logger = LoggerFactory.getLogger(VerificationRulesController.class);

    private final Map verificationRules;

    public VerificationRulesController() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        this.verificationRules =
                mapper.readValue(
                        new ClassPathResource("verificationRules.json").getFile(), Map.class);
    }

    @GetMapping(value = "/verificationRules")
    public @ResponseBody ResponseEntity<Map> getVerificationRules() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(CacheUtil.VERIFICATION_RULES_MAX_AGE))
                .body(verificationRules);
    }
}
