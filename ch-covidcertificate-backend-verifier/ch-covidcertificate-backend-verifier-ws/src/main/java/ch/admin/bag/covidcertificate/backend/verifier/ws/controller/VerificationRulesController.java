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

import ch.admin.bag.covidcertificate.backend.verifier.data.util.CacheUtil;
import ch.admin.bag.covidcertificate.backend.verifier.ws.utils.EtagUtil;
import ch.ubique.openapi.docannotations.Documentation;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
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

        InputStream verificationRulesFile =
                new ClassPathResource("verificationRules.json").getInputStream();
        this.verificationRules = mapper.readValue(verificationRulesFile, Map.class);
        this.verificationRulesEtag =
                EtagUtil.getSha1HashForFiles(true, "classpath:verificationRules.json");
    }

    private ArrayList<Map> mapV2RulesToV1(JsonNode template) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        InputStream verificationRulesV2File =
                new ClassPathResource("verificationRulesV2.json").getInputStream();
        var newFormat = mapper.readTree(verificationRulesV2File);
        ArrayList<Map> rules = new ArrayList<>();
        for (var rule : newFormat.get("rules")) {
            HashMap<String, Object> newRule = new HashMap<>();
            newRule.put("id", rule.get("identifier"));
            newRule.put("logic", rule.get("logic"));
            newRule.put("description", rule.get("description").get(0).get("desc"));
            newRule.put("inputParameter", rule.get("affectedFields").toString());
            rules.add(newRule);
        }
        return rules;
    }

    @Documentation(
            description = "get list of verification rules",
            responses = {
                    "200 => list of verification rules",
                    "304 => no changes since last request"
            },
            responseHeaders = {"ETag:etag to set for next request:string"})
    @GetMapping(value = "/verificationRules")
    public @ResponseBody
    ResponseEntity<Map> getVerificationRules(WebRequest request) {
        if (request.checkNotModified(verificationRulesEtag)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
        }
        HttpHeaders headers =
                CacheUtil.createExpiresHeader(
                        CacheUtil.roundToNextVerificationRulesBucketStart(Instant.now()));
        return ResponseEntity.ok()
                .headers(headers)
                .body(verificationRules);
    }
}
