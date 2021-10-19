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
import ch.admin.bag.covidcertificate.backend.verifier.ws.model.valuesets.TestValueSets;
import ch.admin.bag.covidcertificate.backend.verifier.ws.model.valuesets.VaccineValueSets;
import ch.admin.bag.covidcertificate.backend.verifier.ws.model.valuesets.ValueSets;
import ch.admin.bag.covidcertificate.backend.verifier.ws.utils.EtagUtil;
import ch.ubique.openapi.docannotations.Documentation;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
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
public class ValueSetsController {

    private static final Logger logger = LoggerFactory.getLogger(ValueSetsController.class);

    public static final List<String> PATHS_TO_VALUE_SETS =
            List.of(
                    "valuesets/test-manf.json",
                    "valuesets/test-type.json",
                    "valuesets/vaccine-mah-manf.json",
                    "valuesets/vaccine-medicinal-product.json",
                    "valuesets/vaccine-prophylaxis.json");

    private final ValueSets valueSets;
    private final String valueSetsEtag;
    private final ObjectMapper mapper = new ObjectMapper();

    public ValueSetsController() throws IOException, NoSuchAlgorithmException {
        this.valueSets = new ValueSets(getTestValueSet(), getVaccineValueSet());

        List<String> pathsToValueSets = new ArrayList<>();
        for (String path : PATHS_TO_VALUE_SETS) {
            pathsToValueSets.add(new ClassPathResource(path).getFile().getPath());
        }
        this.valueSetsEtag =
                EtagUtil.getSha1HashForFiles(
                        true, pathsToValueSets.toArray(new String[pathsToValueSets.size()]));
    }

    private VaccineValueSets getVaccineValueSet() throws IOException {
        VaccineValueSets vaccine = new VaccineValueSets();
        vaccine.setMahManf(readFileAsMap("valuesets/vaccine-mah-manf.json"));
        vaccine.setMedicinalProduct(readFileAsMap("valuesets/vaccine-medicinal-product.json"));
        vaccine.setProphylaxis(readFileAsMap("valuesets/vaccine-prophylaxis.json"));
        return vaccine;
    }

    private TestValueSets getTestValueSet() throws IOException {
        TestValueSets test = new TestValueSets();
        test.setManf(readFileAsMap("valuesets/test-manf.json"));
        test.setType(readFileAsMap("valuesets/test-type.json"));
        return test;
    }

    /**
     * maps a file to a generic {@link Map}
     *
     * @param path relative to classpath
     * @return
     */
    private Map readFileAsMap(String path) throws IOException {
        File file = new ClassPathResource(path).getFile();
        return mapper.readValue(file, Map.class);
    }

    @Documentation(
            description = "get value sets",
            responses = {"200 => value sets", "304 => no changes since last request"},
            responseHeaders = {"ETag:etag to set for next request:string"})
    @GetMapping(value = "/metadata")
    public @ResponseBody ResponseEntity<ValueSets> getValueSets(WebRequest request) {
        if (request.checkNotModified(valueSetsEtag)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
        }
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(CacheUtil.VALUE_SETS_MAX_AGE))
                .body(valueSets);
    }
}
