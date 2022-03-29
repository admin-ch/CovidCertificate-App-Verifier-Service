/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.backend.verifier.sync.syncer;

import ch.admin.bag.covidcertificate.backend.verifier.data.ForeignRulesDataService;
import ch.admin.bag.covidcertificate.backend.verifier.model.ForeignRule;
import ch.admin.bag.covidcertificate.backend.verifier.sync.utils.CmsUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.operator.OperatorCreationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DgcForeignRulesSyncer {
    private static final Logger logger = LoggerFactory.getLogger(DgcForeignRulesSyncer.class);
    private final ForeignRulesDataService foreignRulesDataService;
    private final DgcRulesClient dgcRulesClient;
    private final ObjectMapper mapper = new ObjectMapper();

    public DgcForeignRulesSyncer(
            DgcRulesClient dgcRulesClient, ForeignRulesDataService foreignRulesDataService) {
        this.dgcRulesClient = dgcRulesClient;
        this.foreignRulesDataService = foreignRulesDataService;
    }

    public int sync() {
        logger.info("Start foreign rules sync with DGC Gateway");
        var start = Instant.now();

        List<String> countries = dgcRulesClient.getCountries();
        countries.remove("CH");
        int successful = 0;
        for(var country: countries){
            var rules = new ArrayList<ForeignRule>();
            var downloaded = dgcRulesClient.download(country);
            downloaded.forEach( (id, ruleVersions) -> {
                ruleVersions.forEach( version ->{
                    try {
                        rules.add(
                                decodeRule(
                                        (ObjectNode) version, country, id));
                    } catch (Exception e) {
                        logger.error(
                                "Failed to decode rule {}",
                                id,
                                e);
                    }
                        }
                );
            });

            if(rules.isEmpty()){
                logger.error("No rules were downloaded or decoded for {}", country);
            }else{
                foreignRulesDataService.removeRuleSet(country);
                rules.forEach(foreignRulesDataService::insertRule);
                successful += rules.size();
            }
        }
        var end = Instant.now();
        logger.info(
                "Successfully downloaded {} foreign rules {} ms", successful,
                end.toEpochMilli() - start.toEpochMilli());
        return successful;
    }

    private ForeignRule decodeRule(ObjectNode rule, String country, String id)
            throws CertificateException, IOException, OperatorCreationException, CMSException {
        var foreignRule = new ForeignRule();
        String content = new String((byte[]) CmsUtil.decodeCms(rule.get("cms").asText()), StandardCharsets.UTF_8);
        ObjectNode convertedNode = mapper.createObjectNode();
        mapper.readTree(content).fields().forEachRemaining(field -> {
            convertedNode.set(field.getKey().toLowerCase(), field.getValue());
        });
        foreignRule.setContent(convertedNode.toString());
        var validUntil = LocalDateTime.parse(rule.get("validTo").asText(), DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        foreignRule.setValidUntil(validUntil);
        var validFrom = LocalDateTime.parse(rule.get("validTo").asText(), DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        foreignRule.setValidFrom(validFrom);
        foreignRule.setVersion(rule.get("version").asText());
        foreignRule.setCountry(country);
        foreignRule.setId(id);

        return foreignRule;
    }
}
