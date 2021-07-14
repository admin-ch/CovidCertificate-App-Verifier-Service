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

import ch.admin.bag.covidcertificate.backend.verifier.model.sync.CertificateType;
import ch.admin.bag.covidcertificate.backend.verifier.model.sync.ProblemReport;
import ch.admin.bag.covidcertificate.backend.verifier.model.sync.TrustList;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

public class DgcClient {

    private static final Logger logger = LoggerFactory.getLogger(DgcClient.class);
    private static final String UPLOAD_PATH = "/signerCertificate";
    private static final String DOWNLOAD_PATH = "/trustList/%s";
    private final String baseUrl;
    private final RestTemplate rt;

    public DgcClient(String baseUrl, RestTemplate rt) {
        this.baseUrl = baseUrl;
        this.rt = rt;
    }

    public TrustList[] download(CertificateType certType) {
        final var uri =
                UriComponentsBuilder.fromHttpUrl(
                                baseUrl + String.format(DOWNLOAD_PATH, certType.name()))
                        .build()
                        .toUri();
        final var request = RequestEntity.get(uri).headers(createDownloadHeaders()).build();
        final ResponseEntity<String> response;
        try {
            logger.debug("Downloading certificates of type {}", certType.name());
            response = rt.exchange(request, String.class);
        } catch (HttpStatusCodeException e) {
            var responseBody = e.getResponseBodyAsString();
            logger.error("Error downloading certificates of type {}", certType.name());
            if (e.getStatusCode().equals(HttpStatus.NOT_FOUND)
                    || e.getStatusCode().equals(HttpStatus.UNAUTHORIZED)) {
                final ProblemReport problemReport;
                try {
                    problemReport = new ObjectMapper().readValue(responseBody, ProblemReport.class);
                    logger.error("Problem: {}", problemReport.getProblem());
                    logger.error("Details: {}", problemReport.getDetails());
                } catch (IOException ioe) {
                    logger.error("Error parsing trustList error response: {}", responseBody);
                    return new TrustList[0];
                }
                return new TrustList[0];
            } else {
                logger.error("Download returned error code {}", e.getStatusCode());
                return new TrustList[0];
            }
        }
        final var body = response.getBody();
        if (body != null) {
            final TrustList[] trustList;
            try {
                trustList = new ObjectMapper().readValue(body, TrustList[].class);
            } catch (IOException e) {
                logger.error("Error parsing trustList response: {}...", body.substring(0, 100));
                return new TrustList[0];
            }
            return trustList;
        } else {
            logger.error("Response body was null");
            return new TrustList[0];
        }
    }

    public void upload(Object cms) {
        // TODO: Implement
    }

    private HttpHeaders createDownloadHeaders() {
        var headers = new HttpHeaders();
        headers.add(HttpHeaders.ACCEPT, "application/json");
        return headers;
    }
}
