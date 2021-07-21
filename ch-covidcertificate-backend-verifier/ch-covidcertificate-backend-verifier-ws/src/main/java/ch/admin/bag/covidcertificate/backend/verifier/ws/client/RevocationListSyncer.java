/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.backend.verifier.ws.client;

import ch.admin.bag.covidcertificate.backend.verifier.data.RevokedCertDataService;
import ch.admin.bag.covidcertificate.backend.verifier.model.cert.db.RevokedCertsUpdateResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.RequestEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

public class RevocationListSyncer {

    private static final Logger logger = LoggerFactory.getLogger(RevocationListSyncer.class);

    private final String baseurl;
    private final String endpoint = "/v1/revocation-list";
    private final RevokedCertDataService revokedCertDataService;
    @Autowired private RestTemplate rt;

    public RevocationListSyncer(
            String revokedCertsBaseUrl, RevokedCertDataService revokedCertDataService) {
        this.baseurl = revokedCertsBaseUrl;
        this.revokedCertDataService = revokedCertDataService;
    }

    public void updateRevokedCerts() {
        logger.info("updating revoked certs");

        try {
            List<String> revokedCerts = downloadRevokedCerts();
            logger.info("downloaded {} revoked certs", revokedCerts.size());

            RevokedCertsUpdateResponse updateResponse =
                    revokedCertDataService.replaceRevokedCerts(revokedCerts);

            logger.info(
                    "finished updating revoked certs. inserted {}, removed {}",
                    updateResponse.getInsertCount(),
                    updateResponse.getRemoveCount());
        } catch (Exception e) {
            logger.error("revoked certs update failed", e);
        }
    }

    private List<String> downloadRevokedCerts() {
        final var requestEndpoint = baseurl + endpoint;
        final var uri = UriComponentsBuilder.fromHttpUrl(requestEndpoint).build().toUri();
        final RequestEntity<Void> requestEntity =
                RequestEntity.get(uri).headers(createDownloadHeaders()).build();
        final var response = rt.exchange(requestEntity, String[].class).getBody();
        return new ArrayList<>(Arrays.asList(response));
    }

    private HttpHeaders createDownloadHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.ACCEPT, "application/json");
        return headers;
    }
}
