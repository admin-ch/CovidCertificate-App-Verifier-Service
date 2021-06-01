/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.backend.verifier.ws.controller;

import ch.admin.bag.covidcertificate.backend.verifier.ws.utils.RestTemplateHelper;
import ch.ubique.openapi.docannotations.Documentation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Controller
@RequestMapping("/v1")
public class RevocationListController {

    private static final Logger logger = LoggerFactory.getLogger(RevocationListController.class);

    private final String baseurl;
    private final RestTemplate rt;

    public RevocationListController(String revokedCertsBaseUrl) {
        this.baseurl = revokedCertsBaseUrl;
        // TODO: Implement
        this.rt = RestTemplateHelper.getRestTemplate();
    }

    @Documentation(
            description = "get list of revoked certificates",
            responses = {"200 => full list of revoked certificates"})
    @CrossOrigin(origins = {"https://editor.swagger.io"})
    @GetMapping(value = "/revocation-list")
    public @ResponseBody ResponseEntity<RevocationResponse> getCerts() {
        final var response = new RevocationResponse();
        final List<String> certs = new ArrayList<>();
        final var requestEndpoint = baseurl + "/v1/revocation-list";
        final var uri = UriComponentsBuilder.fromHttpUrl(requestEndpoint).build().toUri();
        final RequestEntity<Void> requestEntity =
                RequestEntity.get(uri).headers(createDownloadHeaders()).build();
        try {
            final var responseEntity = rt.exchange(requestEntity, String[].class);
            final var body = responseEntity.getBody();
            if (body != null) {
                certs.addAll(Arrays.asList(body));
            }
        } catch (HttpStatusCodeException e) {
            logger.info(
                    "Request returned error code: {} {}",
                    e.getStatusCode(),
                    e.getStatusCode().getReasonPhrase());
            throw e;
        }
        response.setRevokedCerts(certs);
        return ResponseEntity.ok().body(response);
    }

    private HttpHeaders createDownloadHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.ACCEPT, "application/json");
        return headers;
    }

    public static class RevocationResponse {
        private List<String> revokedCerts;

        public List<String> getRevokedCerts() {
            return revokedCerts;
        }

        public void setRevokedCerts(List<String> revokedCerts) {
            this.revokedCerts = revokedCerts;
        }
    }
}
