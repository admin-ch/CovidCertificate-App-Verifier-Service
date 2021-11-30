/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.backend.verifier.valuesets;

import ch.ubique.openapi.docannotations.Documentation;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.List;
import java.util.Map;

public class VerificationRules {
    private List<Map> rules;
    private final JsonNode valueSets;

    @Documentation(
            description = "describes how long the list response is valid for in ms",
            example = "172800000")
    private final Duration validDuration = Duration.ofHours(48);

    public VerificationRules(List<Map> rules) throws JsonProcessingException {
        this.rules = rules;
        this.valueSets =
                new ObjectMapper()
                        .readTree(
                                "{\n"
                                        + "    \"disease-agent-targeted\": [\n"
                                        + "        \"840539006\"\n"
                                        + "    ],\n"
                                        + "    \"accepted-vaccines\": [\n"
                                        + "        \"EU/1/20/1528\",\n"
                                        + "        \"EU/1/20/1507\",\n"
                                        + "        \"EU/1/21/1529\",\n"
                                        + "        \"EU/1/20/1525\",\n"
                                        + "        \"CoronaVac\",\n"
                                        + "        \"BBIBP-CorV\",\n"
                                        + "        \"Covishield\",\n"
                                        + "        \"Covaxin\",\n"
                                        + "        \"BBIBP-CorV_T\",\n"
                                        + "        \"CoronaVac_T\",\n"
                                        + "        \"Covaxin_T\"\n"
                                        + "    ],\n"
                                        + "    \"two-dose-vaccines\": [\n"
                                        + "        \"EU/1/20/1528\",\n"
                                        + "        \"EU/1/20/1507\",\n"
                                        + "        \"EU/1/21/1529\",\n"
                                        + "        \"CoronaVac\",\n"
                                        + "        \"BBIBP-CorV\",\n"
                                        + "        \"Covishield\",\n"
                                        + "        \"Covaxin\",\n"
                                        + "        \"BBIBP-CorV_T\",\n" 
                                        + "        \"CoronaVac_T\",\n"
                                        + "        \"Covaxin_T\"\n"
                                        + "    ],\n"
                                        + "    \"one-dose-vaccines-with-offset\": [\n"
                                        + "        \"EU/1/20/1525\"\n"
                                        + "    ],\n"
                                        + "    \"covid-19-lab-test-type\": [\n"
                                        + "        \"LP217198-3\",\n"
                                        + "        \"LP6464-4\",\n"
                                        + "        \"94504-8\"\n"
                                        + "    ],\n"
                                        + "    \"acceptance-criteria\": {\n"
                                        + "        \"single-vaccine-validity-offset\": 21,\n"
                                        + "        \"vaccine-immunity\": 364,\n"
                                        + "        \"rat-test-validity\": 24,\n"
                                        + "        \"pcr-test-validity\": 48,\n"
                                        + "        \"recovery-offset-valid-from\": 10,\n"
                                        + "        \"recovery-offset-valid-until\": 364\n"
                                        + "    }\n"
                                        + "}");
    }

    public List<Map> getRules() {
        return rules;
    }

    public JsonNode getValueSets() {
        return valueSets;
    }

    public Long getValidDuration() {
        return validDuration.toMillis();
    }
}
