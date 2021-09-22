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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.core.io.ClassPathResource;

@TestInstance(Lifecycle.PER_CLASS)
public class VerificationRulesTest {

    private ObjectMapper mapper;

    private static final String RULES_V1_PATH = "src/main/resources/verificationRules.json";
    private static final String RULES_V2_PATH = "src/main/resources/verificationRulesV2.json";
    private static final String RULES_UPLOAD_PATH =
            "src/main/resources/verificationRulesUpload.json";
    private static final String RULES_MASTER_CLASSPATH = "verificationRulesMaster.json";

    @BeforeAll
    public void setup() throws Exception {
        DefaultPrettyPrinter pp =
                new CustomPrettyPrinter()
                        .withArrayIndenter(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE);

        this.mapper =
                new ObjectMapper().setDefaultPrettyPrinter(pp).registerModule(new JavaTimeModule());
    }

    @Test
    @Disabled("enable and run manually to generate new rules jsons")
    public void generateRulesJsons() throws Exception {
        JsonNode v2 = mapMasterToV2();
        mapV2RulesToUpload(v2);
        mapV2RulesToV1(v2);
    }

    private JsonNode mapMasterToV2() throws Exception {
        JsonNode master =
                mapper.readTree(new ClassPathResource(RULES_MASTER_CLASSPATH).getInputStream());
        // TODO map to V2 and return V2 JsonNode
        mapper.writerWithDefaultPrettyPrinter().writeValue(new File(RULES_V2_PATH), master);
        return master;
    }

    private void mapV2RulesToUpload(JsonNode v2) throws Exception {
        Map uploadRules = new LinkedHashMap<String, Object>();
        for (var rule : v2.get("rules")) {
            ArrayNode rulesNode = mapper.createArrayNode();
            rulesNode.add(getJsonNodeWithCapitalizedTopLevelKeys(rule));
            uploadRules.put(rule.get("identifier").asText(), rulesNode);
        }
        mapper.writerWithDefaultPrettyPrinter()
                .writeValue(new File(RULES_UPLOAD_PATH), uploadRules);
    }

    private JsonNode getJsonNodeWithCapitalizedTopLevelKeys(JsonNode jsonNode) {
        Map<String, Object> map = mapper.convertValue(jsonNode, Map.class);
        Map<String, Object> capitalized = new LinkedHashMap<>();
        for (Entry<String, Object> entry : map.entrySet()) {
            capitalized.put(StringUtils.capitalize(entry.getKey()), entry.getValue());
        }
        return mapper.convertValue(capitalized, JsonNode.class);
    }

    private void mapV2RulesToV1(JsonNode v2) throws Exception {
        ArrayList<Map> rules = new ArrayList<>();
        for (var rule : v2.get("rules")) {
            HashMap<String, Object> v1Rule = new HashMap<>();
            v1Rule.put("id", rule.get("identifier"));
            v1Rule.put("logic", dateComparisonV2ToV1(rule.get("logic")));
            v1Rule.put("description", rule.get("description").get(0).get("desc"));
            v1Rule.put("inputParameter", rule.get("affectedFields").toString());
            rules.add(v1Rule);
        }
        mapper.writerWithDefaultPrettyPrinter()
                .writeValue(new File(RULES_V1_PATH), new VerificationRules(rules));
    }

    private JsonNode dateComparisonV2ToV1(JsonNode logic) throws JsonProcessingException {
        return mapper.readTree(
                logic.toString()
                        .replace("\"not-before\"", "\">=\"")
                        .replace("\"not-after\"", "\"<=\"")
                        .replace("\"before\"", "\"<\"")
                        .replace("\"after\"", "\">\""));
    }

    private class CustomPrettyPrinter extends DefaultPrettyPrinter {

        @Override
        public DefaultPrettyPrinter createInstance() {
            return this;
        }

        @Override
        public void writeObjectFieldValueSeparator(JsonGenerator jg) throws IOException {
            jg.writeRaw(": ");
        }

        @Override
        public DefaultPrettyPrinter withArrayIndenter(Indenter i) {
            this._arrayIndenter = i;
            return this;
        }
    }
}
