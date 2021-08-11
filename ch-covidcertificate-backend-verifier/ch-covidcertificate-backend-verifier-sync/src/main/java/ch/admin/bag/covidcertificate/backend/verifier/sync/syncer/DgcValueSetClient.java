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

import ch.admin.bag.covidcertificate.backend.verifier.sync.exception.ValueSetFormatException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

public class DgcValueSetClient {

    private static final Logger logger = LoggerFactory.getLogger(DgcValueSetClient.class);
    private static final String VALUESETS_ENDPOINT = "/valuesets";
    private final String baseUrl;
    private final RestTemplate rt;

    public DgcValueSetClient(String baseUrl, RestTemplate rt) {
        this.baseUrl = baseUrl;
        this.rt = rt;
    }

    /**
     * downloads value sets
     *
     * @return valueSetId to json response string
     */
    public Map<String, String> download() throws ValueSetFormatException, JsonProcessingException {
        logger.info("Downloading value sets");
        Map<String, String> valueSets = new HashMap<>();

        List<String> valueSetIds =
                rt.exchange(
                                baseUrl + VALUESETS_ENDPOINT,
                                HttpMethod.GET,
                                createHttpEntity(),
                                new ParameterizedTypeReference<List<String>>() {})
                        .getBody();

        for (String valueSetId : valueSetIds) {
            logger.debug("Downloading value set details for id: {}", valueSetId);
            try {
                String responseString = downloadValueSet(valueSetId);
                validateResponse(responseString, valueSetId);
                valueSets.put(valueSetId, responseString);
            } catch (Exception e) {
                logger.error("download or validation failed for: {}", valueSetId, e);
            }
        }

        logger.info("downloaded value sets for: {}", valueSetIds);
        return valueSets;
    }

    private String downloadValueSet(String valueSetId) {
        return rt.exchange(
                        getValueSetDetailEndpoint(valueSetId),
                        HttpMethod.GET,
                        createHttpEntity(),
                        String.class)
                .getBody();
    }

    private void validateResponse(String responseString, String valueSetId)
            throws ValueSetFormatException, JsonProcessingException {
        Map response = new ObjectMapper().readValue(responseString, Map.class);

        String receivedValueSetId = (String) response.get("valueSetId");
        if (receivedValueSetId == null || !receivedValueSetId.equals(valueSetId)) {
            throw new ValueSetFormatException(
                    "unexpected valueSetId. expected: "
                            + valueSetId
                            + ". received: "
                            + receivedValueSetId,
                    responseString);
        }

        String valueSetDate = (String) response.get("valueSetDate");
        if (valueSetDate == null) {
            throw new ValueSetFormatException("valueSetDate is null", responseString);
        }
    }

    private String getValueSetDetailEndpoint(String valueSetId) {
        return baseUrl + VALUESETS_ENDPOINT + "/" + valueSetId;
    }

    private HttpEntity<Void> createHttpEntity() {
        var headers = new HttpHeaders();
        headers.add(HttpHeaders.ACCEPT, "application/json");
        return new HttpEntity<>(headers);
    }
}
