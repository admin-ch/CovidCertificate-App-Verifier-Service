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
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.junit.internal.Classes;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.core.io.ClassPathResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@TestInstance(Lifecycle.PER_CLASS)
public class VerificationRulesTest {
    private static final Logger logger = LoggerFactory.getLogger(VerificationRulesTest.class);
    private ObjectMapper mapper;

    private static final String RULES_V1_PATH = "src/main/resources/verificationRules.json";
    private static final String RULES_V2_PATH = "src/main/resources/verificationRulesV2.json";
    private static final String RULES_UPLOAD_PATH =
            "src/main/resources/verificationRulesUpload.json";
    private static final String RULES_MASTER_CLASSPATH = "verificationRulesMaster.json";
    private static final String RULES_MASTER_DIR = "master-rules";
    private static final String CH_ONLY_DIR = "ch-only-rules";
    private static final String EU_TEST_RULES_OUTPUT_PATH = "src/main/resources/CH/";

    @BeforeAll
    public void setup() throws Exception {
        DefaultPrettyPrinter pp =
                new CustomPrettyPrinter()
                        .withArrayIndenter(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE);

        this.mapper =
                new ObjectMapper().setDefaultPrettyPrinter(pp).registerModule(new JavaTimeModule());
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "COVIDCERT_GENERATE_VALIDATION_RULES", matches = "1")
    public void generateRulesJsons() throws Exception {
        JsonNode v2 = mapMasterToV2();
        mapV2RulesToUpload(v2);
        mapV2RulesToV1(v2);
    }

    private JsonNode mapMasterToV2() throws Exception {
        JsonNode master =
                mapper.readTree(new ClassPathResource(RULES_MASTER_CLASSPATH).getInputStream());

        List<JsonNode> rules =
                Arrays.stream(new ClassPathResource(RULES_MASTER_DIR).getFile().list())
                        .filter(filename -> filename.endsWith(".json"))
                        .sorted()
                        .map(
                                filename -> {
                                    try {
                                        return mapper.readTree(
                                                new ClassPathResource(
                                                                RULES_MASTER_DIR + "/" + filename)
                                                        .getInputStream());
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                        return null;
                                    }
                                })
                        .collect(Collectors.toList());
        ((ObjectNode) master).putArray("rules").addAll(rules);
        // Insert rules for display logic, for tests and vaccines only valid in
        // switzerland
        // NOTE: we need to use DeMorgans rule to desugar 'or' since CertLogic does not support it
        List<JsonNode> chDisplayRules =
                Arrays.stream(new ClassPathResource(CH_ONLY_DIR).getFile().list())
                        .filter(filename -> filename.endsWith(".json"))
                        .sorted()
                        .map(
                                filename -> {
                                    try {
                                        ObjectNode node = new ObjectNode(JsonNodeFactory.instance);
                                        node.putArray("!").add(
                                                mapper.readTree(
                                                new ClassPathResource(CH_ONLY_DIR + "/" + filename)
                                                        .getInputStream()));
                                        return node;
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                        return null;
                                    }
                                })
                        .collect(Collectors.toList());
        master.get("displayRules")
                .elements()
                .forEachRemaining(
                        (displayRule -> {
                            if (displayRule.get("id").asText().equals("is-only-valid-in-ch")) {
                                ((ObjectNode) displayRule.get("logic").get("!").get(0))
                                        .putArray("and")
                                        .addAll(chDisplayRules);
                            }
                        }));

        ObjectNode v2 = master.deepCopy();
        resolvedInlineVars(v2, master);
        mapper.writerWithDefaultPrettyPrinter().writeValue(new File(RULES_V2_PATH), v2);
        return v2;
    }

    private void resolvedInlineVars(JsonNode resolved, JsonNode master) {
        if (resolved.isArray()) {
            for (JsonNode element : resolved) {
                resolvedInlineVars(element, master);
            }
        } else if (resolved.isObject()) {
            Map<String, JsonNode> resolvedObjects = new LinkedHashMap<>();
            Iterator<Entry<String, JsonNode>> fieldIterator = resolved.fields();
            while (fieldIterator.hasNext()) {
                Entry<String, JsonNode> entry = fieldIterator.next();
                String key = entry.getKey();
                JsonNode value = entry.getValue();
                if (key.equals("var")) {
                    if (value.isTextual()) {
                        if (value.textValue().startsWith("external.valueSets")) {
                            resolvedObjects.put(
                                    key,
                                    master.at(
                                            value.textValue()
                                                    .substring("external".length())
                                                    .replace(".", "/")));
                        }
                    }
                } else {
                    resolvedInlineVars(value, master);
                }
            }
            for (Entry<String, JsonNode> resolvedEntry : resolvedObjects.entrySet()) {
                ((ObjectNode) resolved).set(resolvedEntry.getKey(), resolvedEntry.getValue());
            }
        }
    }

    private void mapV2RulesToUpload(JsonNode v2) throws Exception {
        Map<String, ArrayNode> uploadRules = new LinkedHashMap<>();
        for (var rule : v2.get("rules")) {
            ArrayNode rulesNode = mapper.createArrayNode();
            JsonNode pascaleCase = getJsonNodeWithCapitalizedTopLevelKeys(rule);
            JsonNode v2Rule = getJsonNodeWithFixedCertLogic(pascaleCase);
            rulesNode.add(v2Rule);
            uploadRules.put(rule.get("identifier").asText(), rulesNode);
        }
        mapper.writerWithDefaultPrettyPrinter()
                .writeValue(new File(RULES_UPLOAD_PATH), uploadRules);

        mapTestRules(uploadRules);
    }

    private void mapTestRules(Map<String, ArrayNode> uploadRules) throws IOException {
        for (Entry<String, ArrayNode> idToRule : uploadRules.entrySet()) {
            String ruleId = idToRule.getKey();
            new File(EU_TEST_RULES_OUTPUT_PATH + ruleId + "/tests").mkdirs();
            mapper.writerWithDefaultPrettyPrinter()
                    .writeValue(
                            new File(EU_TEST_RULES_OUTPUT_PATH + ruleId + "/rule.json"),
                            idToRule.getValue().get(0));
        }
    }

    private JsonNode getJsonNodeWithCapitalizedTopLevelKeys(JsonNode jsonNode) {
        Map<String, Object> map = mapper.convertValue(jsonNode, Map.class);
        Map<String, Object> capitalized = new LinkedHashMap<>();
        for (Entry<String, Object> entry : map.entrySet()) {
            capitalized.put(StringUtils.capitalize(entry.getKey()), entry.getValue());
        }
        return mapper.convertValue(capitalized, JsonNode.class);
    }

    private JsonNode getJsonNodeWithFixedCertLogic(JsonNode jsonNode) {
        Map<String, Object> map = mapper.convertValue(jsonNode, Map.class);
        Map<String, Object> fixed = new LinkedHashMap<>();
        for (Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            if (key.equalsIgnoreCase("engine")
                    && entry.getValue() instanceof String
                    && "CertLogic".equalsIgnoreCase((String) entry.getValue())) {
                fixed.put(key, ((String) entry.getValue()).toUpperCase());
            } else {
                fixed.put(entry.getKey(), entry.getValue());
            }
        }
        return mapper.convertValue(fixed, JsonNode.class);
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
