// Copyright (c) 2021 Patrick Amrein <amrein@ubique.ch>
// 
// This software is released under the MIT License.
// https://opensource.org/licenses/MIT

package ch.admin.bag.covidcertificate.backend.verifier.sync.syncer;

import java.util.Base64;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import ch.admin.bag.covidcertificate.backend.verifier.model.sync.CmsResponse;
import ch.admin.bag.covidcertificate.backend.verifier.model.sync.ProblemReport;
import ch.admin.bag.covidcertificate.backend.verifier.model.sync.SigningPayload;

public class DgcRulesClient {
    private static final Logger logger = LoggerFactory.getLogger(DgcRulesClient.class);
    private static final String RULE_UPLOAD_PATH = "/rules";
    private static final String SIGNING_PATH = "/v1/cms";
    private static final String DOWNLOAD_PATH = "/rules/%s";
    private final String bitBaseUrl;
    private final String dgcBaseUrl;
    private final RestTemplate dgcRT;
    private final RestTemplate bitRT;

    public DgcRulesClient(String dgcBaseUrl, String bitBaseUrl, RestTemplate bitRT, RestTemplate dgcRT) {
        this.bitBaseUrl = bitBaseUrl;
        this.dgcBaseUrl = dgcBaseUrl;
        this.dgcRT = dgcRT;
        this.bitRT = bitRT;
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
        return RequestEntity.post(bitBaseUrl + SIGNING_PATH).body(data);
    }

    private RequestEntity<String> postCmsWithRule(ResponseEntity<CmsResponse> response) {
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            return RequestEntity.post(dgcBaseUrl + RULE_UPLOAD_PATH).body(response.getBody().getCms());
        }
        return null;
    }

    /**
     * downloads rules for all countries
     *
     * @return rules per country
     */
    public void upload(Map<String, List<Object>> rules) {
        logger.info("Uploading Swiss rules");
        var mapper = new ObjectMapper();
        // load payload
        for (var ruleArray : rules.keySet()) {
            for (var rule : rules.get(ruleArray)) {
                // make request to bit for signing
                try {
                    var serializeObject = mapper.writeValueAsBytes(rule);
                    var base64encoded = Base64.getEncoder().encodeToString(serializeObject);
                    var payloadObject = new SigningPayload();
                    payloadObject.setData(base64encoded);

                    ResponseEntity<CmsResponse> response = this.bitRT.exchange(postSignedContent(payloadObject),
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
                            logger.error("rule version {} had error {}", ruleArray, problemReport.getDetails());
                        } catch (JsonProcessingException processingException) {
                            logger.error("rule version {} was not successfully uploaded", ruleArray);
                        }

                    } else {
                        logger.info("rule version for {} uploaded", ruleArray);
                    }
                } catch (JsonProcessingException ex) {
                    logger.error("Serializing rule failed: {}", ex);
                }
            }
        }
        logger.info("Uploaded Swiss rules ");
    }
}
