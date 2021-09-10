// Copyright (c) 2021 Patrick Amrein <amrein@ubique.ch>
//
// This software is released under the MIT License.
// https://opensource.org/licenses/MIT

package ch.admin.bag.covidcertificate.backend.verifier.sync.syncer;

import ch.admin.bag.covidcertificate.backend.verifier.model.sync.CmsResponse;
import ch.admin.bag.covidcertificate.backend.verifier.model.sync.SigningPayload;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Base64;
import java.util.Map;
import java.util.Map.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

public class DgcRulesClient {
    private static final Logger logger = LoggerFactory.getLogger(DgcRulesClient.class);
    private static final String RULE_UPLOAD_PATH = "/rules";
    private static final String SIGNING_PATH = "/v1/cms/";
    private static final String DOWNLOAD_PATH = "/rules/%s";
    private final String signBaseUrl;
    private final String dgcBaseUrl;
    private final RestTemplate dgcRT;
    private final RestTemplate signRT;

    public DgcRulesClient(
            String dgcBaseUrl, RestTemplate dgcRT, String signBaseUrl, RestTemplate signRT) {
        this.signBaseUrl = signBaseUrl;
        this.dgcBaseUrl = dgcBaseUrl;
        this.dgcRT = dgcRT;
        this.signRT = signRT;
    }

    /**
     * downloads rules for all countries
     *
     * @return rules per country
     */
    public Map<String, String> download() {
        logger.info("[DgcRulesClient] Downloading rules");

        logger.info("[DgcRulesClient] downloaded rules for: ");
        return null;
    }

    private RequestEntity<SigningPayload> postSignedContent(SigningPayload data) {
        logger.info("[DgcRulesClient] Try siging {}", signBaseUrl + SIGNING_PATH);
        return RequestEntity.post(signBaseUrl + SIGNING_PATH).body(data);
    }

    private RequestEntity<String> postCmsWithRule(ResponseEntity<CmsResponse> response) {
        var body = response.getBody();
        logger.info("[DgcRulesClient] Try upload {}", dgcBaseUrl + RULE_UPLOAD_PATH);
        if (response.getStatusCode().is2xxSuccessful() && body != null) {
            return RequestEntity.post(dgcBaseUrl + RULE_UPLOAD_PATH)
                    .headers(createCmsUploadHeaders())
                    .body(body.getCms());
        }
        return null;
    }

    /**
     * downloads rules for all countries
     *
     * @return rules per country
     */
    public void upload(JsonNode rules) {
        logger.info("[DgcRulesClient] Uploading Swiss rules");
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
                    ResponseEntity<CmsResponse> response = null;
                    try {
                        response =
                                this.signRT.exchange(
                                        postSignedContent(payloadObject), CmsResponse.class);
                    } catch (HttpStatusCodeException httpFailed) {
                        logger.error("[DgcRulesClient] Signing failed with error: {}", httpFailed);
                        continue;
                    }

                    // upload to gateway
                    var uploadRequest = postCmsWithRule(response);
                    if (uploadRequest == null) {
                        continue;
                    }
                    try {
                        this.dgcRT.exchange(uploadRequest, String.class);
                        logger.info(
                                "[DgcRulesClient] rule version for {} uploaded",
                                ruleArray.getKey());
                    } catch (HttpStatusCodeException httpFailed) {
                        logger.error(
                                "[DgcRulesClient] rule version {} had error {}",
                                ruleArray.getKey(),
                                httpFailed);
                        continue;
                    }
                } catch (JsonProcessingException ex) {
                    logger.error("[DgcRulesClient] Upload rule failed with error: {}", ex);
                }
            }
        }
        logger.info("[DgcRulesClient] Uploaded Swiss rules ");
    }

    private HttpHeaders createCmsUploadHeaders() {
        var headers = new HttpHeaders();
        headers.add(HttpHeaders.ACCEPT, "application/json");
        headers.add(HttpHeaders.CONTENT_TYPE, "application/cms-text");
        return headers;
    }
}
