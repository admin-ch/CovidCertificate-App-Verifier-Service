// Copyright (c) 2021 Patrick Amrein <amrein@ubique.ch>
//
// This software is released under the MIT License.
// https://opensource.org/licenses/MIT

package ch.admin.bag.covidcertificate.backend.verifier.sync.syncer;

import ch.admin.bag.covidcertificate.backend.verifier.model.sync.CmsResponse;
import ch.admin.bag.covidcertificate.backend.verifier.model.sync.SigningPayload;
import ch.admin.bag.covidcertificate.backend.verifier.sync.utils.CmsUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
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
    public List<String> upload(JsonNode rules) {
        logger.info("Uploading Swiss rules");
        List<String> uploadedRuleIds = new ArrayList<>();
        var mapper = new ObjectMapper();
        // load payload
        var fieldIterator = rules.fields();
        while (fieldIterator.hasNext()) {
            Entry<String, JsonNode> ruleArray = fieldIterator.next();
            for (var rule : ruleArray.getValue()) {
                // make request to bit for signing
                try {
                    var serializeObject = mapper.writeValueAsBytes(rule);
                    var base64encoded = Base64.getEncoder().encodeToString(serializeObject);
                    var payloadObject = new SigningPayload();
                    payloadObject.setData(base64encoded);
                    CmsResponse cms = signingClient.getCms(payloadObject);
                    if (cms == null) {
                        continue;
                    }

                    // upload to gateway
                    String ruleId = ruleArray.getKey();
                    logger.info("Uploading rule {} to {}", ruleId, dgcBaseUrl + RULE_UPLOAD_PATH);
                    try {
                        this.dgcRT.exchange(
                                RequestEntity.post(dgcBaseUrl + RULE_UPLOAD_PATH)
                                        .headers(CmsUtil.createCmsUploadHeaders())
                                        .body(cms),
                                String.class);
                        logger.info("New version of rule {} uploaded", ruleId);
                        uploadedRuleIds.add(ruleId);
                    } catch (HttpStatusCodeException e) {
                        if (e.getStatusCode().equals(HttpStatus.CONFLICT)) {
                            logger.info(
                                    ">= version of rule {} has already been uploaded", ruleId, e);
                        } else {
                            logger.error("Upload of rule {} failed", ruleId, e);
                        }
                        continue;
                    }
                } catch (JsonProcessingException ex) {
                    logger.error("Upload rule failed with error: {}", ex);
                }
            }
        }
        logger.info("Finished uploading Swiss rules");
        return uploadedRuleIds;
    }
}
