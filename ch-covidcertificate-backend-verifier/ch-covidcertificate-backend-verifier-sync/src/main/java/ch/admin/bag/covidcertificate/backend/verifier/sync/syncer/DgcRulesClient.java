// Copyright (c) 2021 Patrick Amrein <amrein@ubique.ch>
// 
// This software is released under the MIT License.
// https://opensource.org/licenses/MIT

package ch.admin.bag.covidcertificate.backend.verifier.sync.syncer;

import java.util.Base64;
import java.util.Map;
import java.util.Map.Entry;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import ch.admin.bag.covidcertificate.backend.verifier.model.sync.CmsResponse;
import ch.admin.bag.covidcertificate.backend.verifier.model.sync.ProblemReport;
import ch.admin.bag.covidcertificate.backend.verifier.model.sync.SigningPayload;

public class DgcRulesClient {
    private static final Logger logger = LoggerFactory.getLogger(DgcRulesClient.class);
    private static final String RULE_UPLOAD_PATH = "/rules";
    private static final String SIGNING_PATH = "/v1/cms/";
    private static final String DOWNLOAD_PATH = "/rules/%s";
    private final String signBaseUrl;
    private final String dgcBaseUrl;
    private final RestTemplate dgcRT;
    private final RestTemplate signRT;

    public DgcRulesClient(String dgcBaseUrl, RestTemplate dgcRT, String signBaseUrl, RestTemplate signRT) {
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
        logger.info("Downloading rules");

        logger.info("downloaded rules for: ");
        return null;
    }

    private RequestEntity<SigningPayload> postSignedContent(SigningPayload data) {
        logger.info("Try siging {}", signBaseUrl + SIGNING_PATH);
        return RequestEntity.post(signBaseUrl + SIGNING_PATH).body(data);
    }

    private RequestEntity<String> postCmsWithRule(ResponseEntity<CmsResponse> response) {
        var body = response.getBody();
        logger.info("Try upload {}", dgcBaseUrl + RULE_UPLOAD_PATH);
        if (response.getStatusCode().is2xxSuccessful() && body != null) {
            return RequestEntity.post(dgcBaseUrl + RULE_UPLOAD_PATH).headers(createDownloadHeaders()).body(body.getCms());
        }
        return null;
    }

    /**
     * downloads rules for all countries
     *
     * @return rules per country
     */
    public void upload(JsonNode rules) {
        logger.info("Uploading Swiss rules");
        var mapper = new ObjectMapper();
        // load payload
        var fieldIterator = rules.fields();
        while ( fieldIterator.hasNext()) {
            Entry<String, JsonNode> ruleArray = fieldIterator.next();
            for (var rule : ruleArray.getValue()) {
                // make request to bit for signing
                try {
                    var serializeObject = mapper.writeValueAsBytes(rule);
                    var base64encoded = Base64.getEncoder().encodeToString(serializeObject);
                    var payloadObject = new SigningPayload();
                    payloadObject.setData(base64encoded);

                    ResponseEntity<CmsResponse> response = this.signRT.exchange(postSignedContent(payloadObject),
                            CmsResponse.class);
                    if (response.getStatusCode().isError()) {
                        logger.error("Signing failed {}", response.getStatusCode());
                        continue;
                    }
                    // upload to gateway
                    var uploadRequest = postCmsWithRule(response);
                    if (uploadRequest == null) {
                        continue;
                    }

                    ResponseEntity<String> result = this.dgcRT.exchange(uploadRequest, String.class);
                    // observe Response
                    if (result.getStatusCode().isError()) {
                        try {
                            var problemReport = mapper.readValue(result.getBody(), ProblemReport.class);
                            logger.error("rule version {} had error {}", ruleArray.getKey(), problemReport.getDetails());
                        } catch (JsonProcessingException processingException) {
                            logger.error("rule version {} was not successfully uploaded", ruleArray.getKey());
                        }

                    } else {
                        logger.info("rule version for {} uploaded", ruleArray.getKey());
                    }
                } catch (Exception ex) {
                    logger.error("Upload rule failed: {}", ex);
                }
            }
        }
        logger.info("Uploaded Swiss rules ");
    }

    private HttpHeaders createDownloadHeaders() {
        var headers = new HttpHeaders();
        headers.add(HttpHeaders.ACCEPT, "application/json");
        headers.add(HttpHeaders.CONTENT_TYPE, "application/cms-text");
        return headers;
    }
}
