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

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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

import ch.admin.bag.covidcertificate.backend.verifier.data.ValueSetDataService;
import ch.admin.bag.covidcertificate.backend.verifier.ws.utils.CacheUtil;
import ch.admin.bag.covidcertificate.backend.verifier.ws.utils.EtagUtil;
import ch.ubique.openapi.docannotations.Documentation;

@Controller
@RequestMapping("trust/v2")
public class VerificationRulesControllerV2 {
    private static final Logger logger = LoggerFactory.getLogger(VerificationRulesController.class);
    private static String VALUE_SETS_KEY = "valueSets";

    private final Map verificationRules;
    private final String verificationRulesEtag;
    private final ValueSetDataService valueSetDataService;

    public VerificationRulesControllerV2(
            ValueSetDataService valueSetDataService) throws IOException, NoSuchAlgorithmException {
        ObjectMapper mapper = new ObjectMapper();
        File verificationRulesFile = new ClassPathResource("verificationRulesV2.json").getFile();
        this.verificationRules = mapper.readValue(verificationRulesFile, Map.class);
        this.verificationRulesEtag = EtagUtil.getSha1HashForFiles(verificationRulesFile.getPath());
        this.valueSetDataService = valueSetDataService;
    }

    @Documentation(description = "get list of verification rules (uses the new format)", responses = { "200 => list of verification rules",
            "304 => no changes since last request" }, responseHeaders = { "ETag:etag to set for next request:string" })
    @GetMapping(value = "/verificationRules")
    public @ResponseBody ResponseEntity<Map> getVerificationRules(WebRequest request) throws NoSuchAlgorithmException {
        var allIds = valueSetDataService.findAllValueSetIds();
        HashMap<String, ArrayList<String>> valueSets = new HashMap<>();
        ArrayList<String> strings = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();
        for(var id : allIds) {
            var valueSet = valueSetDataService.findLatestValueSet(id);
            if(valueSet != null) {
                try {
                    var entryJson = mapper.readTree(valueSet);
                    strings.add(valueSet);
                    var fieldNameIterator = entryJson.get("valueSetValues").fieldNames();
                    var valueSetValues = new ArrayList<String>();
                    while(fieldNameIterator.hasNext()) {
                        valueSetValues.add(fieldNameIterator.next());
                    }
                    valueSets.put(id, valueSetValues);
                }
                catch(Exception ex) {
                    continue;
                }
               
            }
        }
        var etag = EtagUtil.getSha1HashForStrings(strings.toArray(new String[0]));
        if (request.checkNotModified(verificationRulesEtag+etag)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
        }
        verificationRules.put(VALUE_SETS_KEY, valueSets);
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(CacheUtil.VERIFICATION_RULES_MAX_AGE))
                .body(verificationRules);
    }
}

