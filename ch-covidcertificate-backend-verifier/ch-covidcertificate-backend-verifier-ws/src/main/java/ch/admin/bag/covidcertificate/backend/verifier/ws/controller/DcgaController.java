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
import ch.admin.bag.covidcertificate.backend.verifier.ws.utils.CacheUtil;
import ch.admin.bag.covidcertificate.backend.verifier.ws.utils.EtagUtil;
import ch.ubique.openapi.docannotations.Documentation;
import java.security.NoSuchAlgorithmException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.WebRequest;

@Controller
@RequestMapping("/dcga/v1")
public class DcgaController {

    private static final Logger logger = LoggerFactory.getLogger(DcgaController.class);

    private final ValueSetDataService valueSetDataService;

    public DcgaController(ValueSetDataService valueSetDataService) {
        this.valueSetDataService = valueSetDataService;
    }

    @Documentation(
            description = "get value sets",
            responses = {
                "200 => value sets",
                "304 => no changes since last request",
                "404 => no value sets found for given valueSetId"
            },
            responseHeaders = {"ETag:etag to set for next request:string"})
    @GetMapping(value = "/valueSets")
    public @ResponseBody ResponseEntity<String> getValueSets(
            WebRequest request,
            @RequestParam(
                            required = false,
                            defaultValue = "covid-19-lab-test-manufacturer-and-name")
                    String valueSetId)
            throws NoSuchAlgorithmException {
        String valueSet = valueSetDataService.findLatestValueSet(valueSetId);
        if (valueSet == null) {
            return ResponseEntity.notFound().build();
        }
        String etag = EtagUtil.getSha1HashForStrings(valueSet);
        if (request.checkNotModified(etag)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
        }
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(CacheUtil.VALUE_SETS_MAX_AGE))
                .body(valueSet);
    }
}
