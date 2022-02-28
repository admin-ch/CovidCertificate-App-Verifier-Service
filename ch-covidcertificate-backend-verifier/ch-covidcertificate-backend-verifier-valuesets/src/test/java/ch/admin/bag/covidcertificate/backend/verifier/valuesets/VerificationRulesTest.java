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
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.core.io.ClassPathResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@TestInstance(Lifecycle.PER_CLASS)
public class VerificationRulesTest {
    private static class CheckMode {
        private final String id;
        private final String displayName;

        private CheckMode(String id, String displayName) {
            this.id = id;
            this.displayName = displayName;
        }

        public String getId() {
            return id;
        }

        public String getDisplayName() {
            return displayName;
        }

        private static final List<CheckMode> ACTIVE_MODES =
                Arrays.asList(new CheckMode("THREE_G", "3G"), new CheckMode("TWO_G", "2G"));
        private static final List<CheckMode> WALLET_ACTIVE_MODES =
                Arrays.asList(
                        new CheckMode("THREE_G", "3G"),
                        new CheckMode("TWO_G", "2G"),
                        new CheckMode("TWO_G_PLUS", "2G+"));
        private static final List<CheckMode> VERIFIER_ACTIVE_MODES =
                Arrays.asList(
                        new CheckMode("THREE_G", "3G"),
                        new CheckMode("TWO_G", "2G"),
                        new CheckMode("TWO_G_PLUS", "2G+"),
                        new CheckMode("TEST_CERT", "Testzertifikat"));
    }

    private static final Logger logger = LoggerFactory.getLogger(VerificationRulesTest.class);
    private ObjectMapper mapper;

    private static final String RULE_VERSION = "1.0.11";
    private static final String RULE_VALID_FROM = "2022-03-04T00:00:00Z";

    private static final String RULES_V1_PATH = "src/main/resources/verificationRules.json";
    private static final String RULES_V2_PATH = "src/main/resources/verificationRulesV2.json";
    private static final String RULES_UPLOAD_PATH =
            "src/main/resources/verificationRulesUpload.json";
    private static final String MASTER_TEMPLATE_CLASSPATH = "templates/masterTemplate.json";
    private static final String RULE_TEMPLATE_CLASSPATH = "templates/ruleTemplate.json";
    private static final String VERIFICATION_RULES_SOURCE_DIR = "verification-rules";
    private static final String UPLOAD_RULES_SOURCE_DIR = "upload-rules";
    private static final String VERIFICATION_RULES_COMPILE_DIR = "generated/verification-rules";
    private static final String DISPLAY_RULES_COMPILE_DIR = "generated/display-rules";
    private static final String UPLOAD_RULES_COMPILE_DIR = "generated/upload-rules";
    private static final String CH_ONLY_RULES_COMPILE_DIR = "generated/ch-only-rules";
    private static final String EU_TEST_RULES_OUTPUT_PATH = "src/main/resources/CH/";
    private static final String MODE_RULE_PATH = "generated/mode-rules/modeRules.aifc.json";

    // matches multiline comments without the leading and trailing /* and */
    private static final Pattern commentPattern =
            Pattern.compile("(?<!\\/)(?<=\\/\\*)((?:(?!\\*\\/).|\\s)*)(?=\\*\\/)");

    // matches the part after the payload. in "payload.v.0.foo"
    private static final Pattern payloadPattern = Pattern.compile("(?<=\"payload\\.).*?(?=\")");

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
        JsonNode v2 = generateV2();
        mapV2RulesToUpload(v2);
        mapV2RulesToV1(v2);
        mapTestRules(v2);
    }


    private List<ObjectNode> readRuleSet(String sourceDir, String compileDir) throws IOException {
        String[] sourceFiles =
                (new ClassPathResource(sourceDir).getFile().list());
        Arrays.sort(sourceFiles);
        List<ObjectNode> rules = new ArrayList<>();

        for (String sourceFile : sourceFiles) {
            String filename = sourceFile + ".json";
            ObjectNode rule =
                    (ObjectNode)
                            mapper.readTree(
                                    new ClassPathResource(RULE_TEMPLATE_CLASSPATH)
                                            .getInputStream());
            switch (filename.charAt(0)) {
                case 'G':
                    rule.put("certificateType", "General");
                    break;
                case 'T':
                    rule.put("certificateType", "Test");
                    break;
                case 'R':
                    rule.put("certificateType", "Recovery");
                    break;
                case 'V':
                    rule.put("certificateType", "Vaccination");
                    break;
                default:
                    logger.error("Rule file name does not start with G, R, T or V: ?{}", filename);
            }
            String[] nameComponents = sourceFile.split("([_.])");
            String ruleId = nameComponents[0];
            String sourceFileContent =
                    new String(
                            new ClassPathResource(
                                    Paths.get(sourceDir, sourceFile)
                                            .toString())
                                    .getInputStream()
                                    .readAllBytes());
            Matcher matcher = commentPattern.matcher(sourceFileContent);
            if (matcher.find()) {
                String description = matcher.group().replace("\n", "");
                ((ObjectNode) (((ArrayNode) rule.get("description")).get(0)))
                        .put("desc", description);
            } else {
                logger.warn(
                        "File contained no comment, rule description will be empty {}", filename);
            }
            JsonNode logic =
                            mapper.readTree(
                                    new ClassPathResource(
                                            Paths.get(
                                                            compileDir,
                                                            filename)
                                                    .toString())
                                            .getInputStream());
            rule.set("logic", logic);
            Matcher payloadMatcher = payloadPattern.matcher(logic.toString());
            Set<String> affectedFields = new HashSet<>();
            while (payloadMatcher.find()) {
                affectedFields.add(payloadMatcher.group());
            }
            affectedFields.forEach(field -> ((ArrayNode) rule.get("affectedFields")).add(field));
            rule.put("identifier", ruleId);
            rule.put("validFrom", RULE_VALID_FROM);
            rule.put("version", RULE_VERSION);
            rules.add(rule);
        }
        return rules;
    }


    private JsonNode generateV2() throws Exception {
        JsonNode v2 =
                mapper.readTree(new ClassPathResource(MASTER_TEMPLATE_CLASSPATH).getInputStream());

        List<ObjectNode> rules = readRuleSet(VERIFICATION_RULES_SOURCE_DIR, VERIFICATION_RULES_COMPILE_DIR);

        ObjectNode modeRule = (ObjectNode) v2.get("modeRules");
        ArrayNode activeModesArray = modeRule.putArray("activeModes");
        ArrayNode walletActiveModesArray = modeRule.putArray("walletActiveModes");
        ArrayNode verifierActiveModesArray = modeRule.putArray("verifierActiveModes");
        CheckMode.ACTIVE_MODES.forEach(activeModesArray::addPOJO);
        CheckMode.WALLET_ACTIVE_MODES.forEach(walletActiveModesArray::addPOJO);
        CheckMode.VERIFIER_ACTIVE_MODES.forEach(verifierActiveModesArray::addPOJO);
        modeRule.set(
                "logic",
                mapper.readTree(
                        new ClassPathResource(Paths.get(MODE_RULE_PATH).toString())
                                .getInputStream()));

        String[] displayRuleFiles =
                (new ClassPathResource(DISPLAY_RULES_COMPILE_DIR).getFile().list());
        List<ObjectNode> displayRules = new ArrayList<>();
        for (String filename : displayRuleFiles) {
            ObjectNode rule = ((ArrayNode) v2.get("displayRules")).addObject();
            rule.put("id", filename.split("\\.")[0]);
            rule.set(
                    "logic",
                    mapper.readTree(
                            new ClassPathResource(
                                            Paths.get(DISPLAY_RULES_COMPILE_DIR, filename)
                                                    .toString())
                                    .getInputStream()));
            displayRules.add(rule);
        }

        ObjectNode chOnlyDisplayRule = ((ArrayNode) v2.get("displayRules")).addObject();
        chOnlyDisplayRule.put("id", "is-only-valid-in-ch");
        ObjectNode logic = chOnlyDisplayRule.putObject("logic");
        ArrayNode notAndArray = logic.putArray("!").addObject().putArray("and");
        Arrays.stream(new ClassPathResource(CH_ONLY_RULES_COMPILE_DIR).getFile().list())
                .filter(filename -> filename.endsWith(".json"))
                .sorted()
                .forEach(
                        filename -> {
                            try {
                                ObjectNode node = notAndArray.addObject();
                                node.putArray("!")
                                        .add(
                                                mapper.readTree(
                                                        new ClassPathResource(
                                                                        CH_ONLY_RULES_COMPILE_DIR
                                                                                + "/"
                                                                                + filename)
                                                                .getInputStream()));
                            } catch (IOException e) {
                                logger.error(
                                        "Something failed while generating only-valid-in-ch rules",
                                        e);
                            }
                        });
        ArrayNode rulesArray = (ArrayNode) v2.get("rules");
        rulesArray.addAll(rules);
        mapper.writerWithDefaultPrettyPrinter().writeValue(new File(RULES_V2_PATH), v2);
        return v2;
    }

    private void mapV2RulesToUpload(JsonNode v2) throws Exception {
        Map<String, ArrayNode> uploadRules = new LinkedHashMap<>();
        List<ObjectNode> rules = readRuleSet(UPLOAD_RULES_SOURCE_DIR, UPLOAD_RULES_COMPILE_DIR);
        for (var rule : rules) {
            ArrayNode rulesNode = mapper.createArrayNode();
            JsonNode pascaleCase = getJsonNodeWithCapitalizedTopLevelKeys(rule);
            JsonNode v2Rule = getJsonNodeWithFixedCertLogic(pascaleCase);
            rulesNode.add(v2Rule);
            uploadRules.put(rule.get("identifier").asText(), rulesNode);
        }
        mapper.writerWithDefaultPrettyPrinter()
                .writeValue(new File(RULES_UPLOAD_PATH), uploadRules);


    }

    private void mapTestRules(JsonNode v2) throws IOException {
        Map<String, ArrayNode> uploadRules = new LinkedHashMap<>();
        for (var rule : v2.get("rules")) {
            ArrayNode rulesNode = mapper.createArrayNode();
            JsonNode pascaleCase = getJsonNodeWithCapitalizedTopLevelKeys(rule);
            JsonNode v2Rule = getJsonNodeWithFixedCertLogic(pascaleCase);
            rulesNode.add(v2Rule);
            uploadRules.put(rule.get("identifier").asText(), rulesNode);
        }

        Files.walk(Path.of(EU_TEST_RULES_OUTPUT_PATH))
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
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
