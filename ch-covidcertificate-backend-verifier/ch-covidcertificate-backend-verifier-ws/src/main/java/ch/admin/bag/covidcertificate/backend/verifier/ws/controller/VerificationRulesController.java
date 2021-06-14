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
import ch.admin.bag.covidcertificate.backend.verifier.ws.utils.EtagUtil;
import ch.ubique.openapi.docannotations.Documentation;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.WebRequest;

@Controller
@RequestMapping("trust/v1")
public class VerificationRulesController {

    private static final Logger logger = LoggerFactory.getLogger(VerificationRulesController.class);

    private final Map verificationRules;
    private final String verificationRulesEtag;

    public VerificationRulesController() throws IOException, NoSuchAlgorithmException {
        ObjectMapper mapper = new ObjectMapper();
        File verificationRulesFile = new ClassPathResource("verificationRules.json").getFile();
        this.verificationRules = mapper.readValue(verificationRulesFile, Map.class);
        this.verificationRulesEtag = EtagUtil.getSha1HashForFiles(verificationRulesFile.getPath());
    }

    @Documentation(
            description = "get list of verification rules",
            responses = {
                "200 => list of verification rules",
                "304 => no changes since last request"
            },
            responseHeaders = {"ETag:etag to set for next request:string"})
    @GetMapping(value = "/verificationRules")
    public @ResponseBody ResponseEntity<Map> getVerificationRules(WebRequest request) {
        if (request.checkNotModified(verificationRulesEtag)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
        }
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(CacheUtil.VERIFICATION_RULES_MAX_AGE))
                .body(verificationRules);
    }
}
