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

import ch.admin.bag.covidcertificate.backend.verifier.data.ValueSetDataService;
import ch.admin.bag.covidcertificate.backend.verifier.data.util.CacheUtil;
import ch.admin.bag.covidcertificate.backend.verifier.ws.utils.EtagUtil;
import ch.ubique.openapi.docannotations.Documentation;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.WebRequest;

@Controller
@RequestMapping("trust/v2")
public class VerificationRulesControllerV2 {
    private static final Logger logger = LoggerFactory.getLogger(VerificationRulesControllerV2.class);
    private static final String VALUE_SETS_KEY = "valueSets";

    private final Map verificationRules;
    private final ValueSetDataService valueSetDataService;

    public VerificationRulesControllerV2(
            ValueSetDataService valueSetDataService, String[] disabledVerificationModes)
            throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        InputStream verificationRulesFile =
                new ClassPathResource("verificationRulesV2.json").getInputStream();
        JsonNode rules = mapper.readTree(verificationRulesFile);

        ArrayNode modes = (ArrayNode) rules.get("modeRules").get("activeModes");
        removeModes(modes, disabledVerificationModes);

        ArrayNode verifierModes = (ArrayNode) rules.get("modeRules").get("verifierActiveModes");
        removeModes(verifierModes, disabledVerificationModes);

        this.verificationRules = mapper.treeToValue(rules, Map.class);

        this.valueSetDataService = valueSetDataService;
    }

    private void removeModes(ArrayNode modes, String[] modesToRemove) {
        var modesIter = modes.iterator();
        while (modesIter.hasNext()) {
            var mode = modesIter.next();
            for (String disabledMode : modesToRemove) {
                if (disabledMode.equals(mode.get("id").asText())) {
                    modesIter.remove();
                    break;
                }
            }
        }
    }

    @Documentation(
            description = "get list of verification rules (uses the new format)",
            responses = {
                "200 => list of verification rules",
                "304 => no changes since last request"
            },
            responseHeaders = {"ETag:etag to set for next request:string"})
    @GetMapping(value = "/verificationRules")
    public @ResponseBody ResponseEntity<Map> getVerificationRules(WebRequest request)
            throws NoSuchAlgorithmException {
        Instant now = Instant.now();
        var allIds = valueSetDataService.findAllValueSetIds();
        HashMap<String, ArrayList<String>> valueSets = new HashMap<>();
        ObjectMapper mapper = new ObjectMapper();
        for (var id : allIds) {
            var valueSet = valueSetDataService.findLatestValueSet(id);
            if (valueSet != null) {
                try {
                    var entryJson = mapper.readTree(valueSet);
                    var fieldNameIterator = entryJson.get("valueSetValues").fieldNames();
                    var valueSetValues = new ArrayList<String>();
                    while (fieldNameIterator.hasNext()) {
                        valueSetValues.add(fieldNameIterator.next());
                    }
                    valueSets.put(id, valueSetValues);
                } catch (Exception ex) {
                    logger.error("Serving Rules failed", ex);
                }
            }
        }

        verificationRules.put(VALUE_SETS_KEY, valueSets);

        String etag = "";
        try {
            etag =
                    EtagUtil.getSha1HashForStrings(
                            true, mapper.writeValueAsString(verificationRules));
        } catch (JsonProcessingException e) {
            logger.error("Failed to calculate ETag for rules", e);
        }
        if (request.checkNotModified(etag)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
        }

        return ResponseEntity.ok()
                .headers(getVerificationRulesHeaders(now))
                .body(verificationRules);
    }

    private HttpHeaders getVerificationRulesHeaders(Instant now) {
        return CacheUtil.createExpiresHeader(
                CacheUtil.roundToNextVerificationRulesBucketStart(now));
    }
}
