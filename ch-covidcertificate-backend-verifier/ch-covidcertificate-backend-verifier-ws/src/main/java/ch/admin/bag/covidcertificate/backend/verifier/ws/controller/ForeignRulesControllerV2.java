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

import ch.admin.bag.covidcertificate.backend.verifier.data.ForeignRulesDataService;
import ch.admin.bag.covidcertificate.backend.verifier.data.ValueSetDataService;
import ch.admin.bag.covidcertificate.backend.verifier.data.util.CacheUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.WebRequest;

@Controller
@RequestMapping("trust/v2")
public class ForeignRulesControllerV2 {

    private static final Logger logger = LoggerFactory.getLogger(ForeignRulesControllerV2.class);

    private final ValueSetDataService valueSetDataService;
    private final ForeignRulesDataService foreignRulesDataService;

    public ForeignRulesControllerV2(
            ValueSetDataService valueSetDataService, ForeignRulesDataService dataService) {
        this.foreignRulesDataService = dataService;
        this.valueSetDataService = valueSetDataService;
    }

    @GetMapping(value = "/foreignRules")
    public @ResponseBody ResponseEntity<List<String>> getCountries(WebRequest request) {
        return ResponseEntity.ok(foreignRulesDataService.getCountries());
    }

    @GetMapping(value = "/foreignRules/{country}")
    public @ResponseBody ResponseEntity<Map> getForeignRules(
            @PathVariable("country") String country) {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> result = new HashMap<>();
        result.put("validDuration", 172800000);

        // Add rules to output
        var foreignRules = foreignRulesDataService.getRulesForCountry(country);
        if(foreignRules.isEmpty()){
            return ResponseEntity.notFound().build();
        }

        var rules =
                foreignRules.stream()
                        .map(
                                rule -> {
                                    try {
                                        return mapper.readTree(rule.getContent());
                                    } catch (JsonProcessingException e) {
                                        e.printStackTrace();
                                        return null;
                                    }
                                })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
        result.put("rules", rules);

        // Add valuesets to output
        var allIds = valueSetDataService.findAllValueSetIds();
        HashMap<String, ArrayList<String>> valueSets = new HashMap<>();
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
        result.put("valueSets", valueSets);

        return ResponseEntity.ok(result);
    }

    private HttpHeaders getVerificationRulesHeaders(Instant now) {
        return CacheUtil.createExpiresHeader(
                CacheUtil.roundToNextVerificationRulesBucketStart(now));
    }
}
