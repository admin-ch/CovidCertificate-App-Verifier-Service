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

import ch.admin.bag.covidcertificate.backend.verifier.model.sync.SigningPayload;
import ch.admin.bag.covidcertificate.backend.verifier.sync.syncer.SigningClient.SigningException;
import ch.admin.bag.covidcertificate.backend.verifier.sync.syncer.model.RulesSyncResult;
import ch.admin.bag.covidcertificate.backend.verifier.sync.utils.CmsUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

public class DgcRulesClient {
    private static final Logger logger = LoggerFactory.getLogger(DgcRulesClient.class);
    private static final String RULE_UPLOAD_PATH = "/rules";
    private static final String RULE_DELETE_PATH = "/rules/delete";
    private static final String DOWNLOAD_PATH = "/rules/%s";
    private final String dgcBaseUrl;
    private final RestTemplate dgcRT;
    private final SigningClient signingClient;

    public DgcRulesClient(String dgcBaseUrl, RestTemplate dgcRT, SigningClient signingClient) {
        this.dgcBaseUrl = dgcBaseUrl;
        this.dgcRT = dgcRT;
        this.signingClient = signingClient;
    }

    /**
     * downloads rules for Switzerland
     *
     * @return rules for Switzerland
     */
    public Map<String, JsonNode> download() {
        logger.info("[DgcRulesClient] Downloading rules");
        Map<String, JsonNode> rules = new HashMap<>();
        ResponseEntity<String> response =
                this.dgcRT.exchange(
                        RequestEntity.get(dgcBaseUrl + String.format(DOWNLOAD_PATH, "CH"))
                                .headers(CmsUtil.createRuleExchangeHeaders())
                                .build(),
                        String.class);
        try {
            new ObjectMapper()
                    .readTree(response.getBody())
                    .fields()
                    .forEachRemaining(
                            field -> {
                                rules.put(field.getKey(), field.getValue());
                            });
        } catch (JsonProcessingException e) {
            logger.error("[DgcRulesClient] Failed to deserialize downloaded rules", e);
        }

        logger.info("[DgcRulesClient] downloaded {} rules", rules.size());
        return rules;
    }

    /**
     * deletes the rules with the given IDs
     *
     * @param identifiers IDs of the rules to delete
     * @return successfully deleted rule IDs
     */
    public RulesSyncResult delete(Collection<String> identifiers) {
        logger.info("Deleting {} rules", identifiers);
        List<String> deletedRuleIds = new ArrayList<>();
        List<String> failedRuleIds = new ArrayList<>();
        identifiers.forEach(
                ruleId -> {
                    String cms = null;
                    try {
                        // sign payload

                        logger.info("signing rule ID {}", ruleId);
                        SigningPayload payload =
                                CmsUtil.encodePayload(ruleId.getBytes(StandardCharsets.UTF_8));
                        cms = signingClient.sign(payload);

                        // upload to gateway
                        logger.info("Deleting rule {}", ruleId);
                        this.dgcRT.exchange(
                                RequestEntity.post(dgcBaseUrl + RULE_DELETE_PATH)
                                        .headers(CmsUtil.createRuleExchangeHeaders())
                                        .body(cms),
                                String.class);
                        logger.info("All versions of rule {} deleted", ruleId);
                        deletedRuleIds.add(ruleId);
                    } catch (SigningException e) {
                        logger.error("Failed to sign rule ID for {}", ruleId);
                    } catch (HttpStatusCodeException e) {
                        failedRuleIds.add(ruleId);
                        logger.error("[FAILED CMS] {}", cms);
                        logger.error("Deletion of rule {} failed", ruleId, e);
                    } catch (Exception ex) {
                        failedRuleIds.add(ruleId);
                        logger.error("Failed to delete rule {}", ruleId, ex);
                    }
                });
        logger.info(
                "Finished deleting rules. {} succeeded, {} failed",
                deletedRuleIds.size(),
                failedRuleIds.size());
        return new RulesSyncResult(deletedRuleIds, failedRuleIds);
    }

    /**
     * deletes all rules for Switzerland
     *
     * @return successfully deleted rule IDs
     */
    public RulesSyncResult deleteAll() {
        logger.info("Deleting all Swiss rules");
        Map<String, JsonNode> rules = download();
        Set<String> ruleIds = rules.keySet();
        return delete(ruleIds);
    }

    /**
     * uploads Swiss rules
     *
     * @return successfully uploaded rule ids
     */
    public RulesSyncResult upload(JsonNode rules) {
        // Download all remote rules and later remove the ones that should not be deleted
        Set<String> rulesToDelete = download().keySet();

        logger.info("Uploading Swiss rules");
        List<String> uploadedRuleIds = new ArrayList<>();
        List<String> failedRuleIds = new ArrayList<>();
        var mapper = new ObjectMapper();
        // load payload
        var fieldIterator = rules.fields();
        while (fieldIterator.hasNext()) {
            Entry<String, JsonNode> ruleArray = fieldIterator.next();
            for (var rule : ruleArray.getValue()) {
                String ruleId = ruleArray.getKey();
                String cms = null;
                try {
                    rulesToDelete.remove(ruleId);
                    // sign payload
                    logger.info("signing rule {}", ruleId);
                    var payloadObject = CmsUtil.encodePayload(mapper.writeValueAsBytes(rule));
                    cms = signingClient.sign(payloadObject);

                    // upload to gateway
                    logger.info("Uploading rule {} to {}", ruleId, dgcBaseUrl + RULE_UPLOAD_PATH);
                    this.dgcRT.exchange(
                            RequestEntity.post(dgcBaseUrl + RULE_UPLOAD_PATH)
                                    .headers(CmsUtil.createRuleExchangeHeaders())
                                    .body(cms),
                            String.class);
                    logger.info("New version of rule {} uploaded", ruleId);
                    uploadedRuleIds.add(ruleId);
                } catch (SigningException e) {
                    logger.error("Signing of rule {} failed", ruleId, e);
                } catch (HttpStatusCodeException e) {
                    failedRuleIds.add(ruleId);
                    if (e.getStatusCode().equals(HttpStatus.CONFLICT)) {
                        logger.info(">= version of rule {} has already been uploaded", ruleId, e);
                    } else {
                        logger.error("[FAILED CMS] {}", cms);
                        logger.error("Upload of rule {} failed", ruleId, e);
                    }
                } catch (Exception ex) {
                    failedRuleIds.add(ruleId);
                    logger.error("Failed to upload rule {}", ruleId, ex);
                }
            }
        }
        logger.info("Finished uploading Swiss rules");
        if (failedRuleIds.isEmpty()) {
            logger.info(
                    "Deleting {} remote rules that no longer exist locally", rulesToDelete.size());
            delete(rulesToDelete);
        } else {
            logger.warn("There were upload failures. Skipping rule deletion");
        }
        return new RulesSyncResult(uploadedRuleIds, failedRuleIds);
    }
}
