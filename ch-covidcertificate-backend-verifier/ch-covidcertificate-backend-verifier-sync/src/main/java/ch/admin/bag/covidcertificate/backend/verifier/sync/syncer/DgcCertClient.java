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

import ch.admin.bag.covidcertificate.backend.verifier.model.exception.AlreadyUploadedException;
import ch.admin.bag.covidcertificate.backend.verifier.model.exception.UploadFailedException;
import ch.admin.bag.covidcertificate.backend.verifier.model.sync.CertificateType;
import ch.admin.bag.covidcertificate.backend.verifier.model.sync.ProblemReport;
import ch.admin.bag.covidcertificate.backend.verifier.model.sync.TrustList;
import ch.admin.bag.covidcertificate.backend.verifier.sync.exception.DgcSyncException;
import ch.admin.bag.covidcertificate.backend.verifier.sync.utils.CmsUtil;
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

public class DgcCertClient {

    private static final Logger logger = LoggerFactory.getLogger(DgcCertClient.class);
    private static final String UPLOAD_PATH = "/signerCertificate";
    private static final String DOWNLOAD_PATH = "/trustList/%s";
    private final String baseUrl;
    private final RestTemplate rt;

    public DgcCertClient(String baseUrl, RestTemplate rt) {
        this.baseUrl = baseUrl;
        this.rt = rt;
    }

    public TrustList[] download(CertificateType certType) throws DgcSyncException {
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
                }
            } else {
                logger.error("Download returned error code {}", e.getStatusCode());
            }
            // The status code does not indicate a success we bail!
            throw new DgcSyncException(e);
        }
        final var body = response.getBody();
        if (body != null) {
            final TrustList[] trustList;
            try {
                trustList = new ObjectMapper().readValue(body, TrustList[].class);
            } catch (IOException e) {
                logger.error("Error parsing trustList response: {}...", body.substring(0, 100));
                // Mapping the JSON failed, let's bail!
                throw new DgcSyncException(e);
            }
            return trustList;
        } else {
            logger.error("Response body was null");
            // The response body should not be null, probably the stream aborted, let's bail!
            throw new DgcSyncException(new Exception("Response body was null"));
        }
    }

    public void upload(String toUpload, String kid)
            throws AlreadyUploadedException, UploadFailedException {
        try {
            logger.info("uploading dsc with kid {}", kid);
            rt.exchange(
                    RequestEntity.post(baseUrl + UPLOAD_PATH)
                            .headers(CmsUtil.createCmsUploadHeaders())
                            .body(toUpload),
                    String.class);
            logger.info("done uploading dsc with kid {}", kid);
        } catch (HttpStatusCodeException e) {
            if (e.getStatusCode().equals(HttpStatus.CONFLICT)) {
                logger.info("dsc with kid {} has already been uploaded", kid, e);
                throw new AlreadyUploadedException();
            } else {
                logger.error("failed to upload dsc with kid {}", kid, e);
                throw new UploadFailedException(e);
            }
        }
    }

    private HttpHeaders createDownloadHeaders() {
        var headers = new HttpHeaders();
        headers.add(HttpHeaders.ACCEPT, "application/json");
        return headers;
    }
}
