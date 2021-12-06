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
import ch.admin.bag.covidcertificate.backend.verifier.sync.syncer.model.RulesSyncResult;
import ch.admin.bag.covidcertificate.backend.verifier.sync.utils.CmsUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

public class DgcRulesClient {
    private static final Logger logger = LoggerFactory.getLogger(DgcRulesClient.class);
    private static final String RULE_UPLOAD_PATH = "/rules";
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
     * downloads rules for all countries
     *
     * @return rules per country
     */
    public Map<String, String> download() { // TODO
        logger.info("[DgcRulesClient] Downloading rules");

        logger.info("[DgcRulesClient] downloaded rules for: ");
        return null;
    }

    /**
     * downloads rules for all countries
     *
     * @return successfully uploaded rule ids
     */
    public RulesSyncResult upload(JsonNode rules) {
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
                try {
                    // sign payload
                    String cms = null;
                    try {
                        logger.info("signing rule {}", ruleId);
                        var serializeObject = mapper.writeValueAsBytes(rule);
                        var base64encoded = Base64.getEncoder().encodeToString(serializeObject);
                        var payloadObject = new SigningPayload(base64encoded);
                        cms = signingClient.sign(payloadObject);
                    } catch (Exception e) {
                        logger.error("Signing rule {} failed", ruleId, e);
                        continue;
                    }

                    // upload to gateway
                    logger.info("Uploading rule {} to {}", ruleId, dgcBaseUrl + RULE_UPLOAD_PATH);
                    try {
                        this.dgcRT.exchange(
                                RequestEntity.post(dgcBaseUrl + RULE_UPLOAD_PATH)
                                        .headers(CmsUtil.createCmsTextUploadHeaders())
                                        .body(cms),
                                String.class);
                        logger.info("New version of rule {} uploaded", ruleId);
                        uploadedRuleIds.add(ruleId);
                    } catch (HttpStatusCodeException e) {
                        failedRuleIds.add(ruleId);
                        if (e.getStatusCode().equals(HttpStatus.CONFLICT)) {
                            logger.info(
                                    ">= version of rule {} has already been uploaded", ruleId, e);
                        } else {
                            logger.error("[FAILED CMS] {}", cms);
                            logger.error("Upload of rule {} failed", ruleId, e);
                        }
                        continue;
                    }
                } catch (Exception ex) {
                    failedRuleIds.add(ruleId);
                    logger.error("Failed to upload rule {}", ruleId, ex);
                }
            }
        }
        logger.info("Finished uploading Swiss rules");
        return new RulesSyncResult(uploadedRuleIds, failedRuleIds);
    }
}
