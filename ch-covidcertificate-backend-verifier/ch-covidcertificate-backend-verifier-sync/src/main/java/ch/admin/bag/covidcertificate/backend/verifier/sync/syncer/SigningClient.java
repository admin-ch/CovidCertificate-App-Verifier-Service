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

import ch.admin.bag.covidcertificate.backend.verifier.model.sync.CmsResponse;
import ch.admin.bag.covidcertificate.backend.verifier.model.sync.SigningPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.web.client.RestTemplate;

public class SigningClient {
    private static final Logger logger = LoggerFactory.getLogger(SigningClient.class);

    private final RestTemplate rt;
    private final String signBaseUrl;
    private static final String SIGNING_PATH = "/v1/cms/";
    private static final String ALIAS_PATH = "/v1/cms/%s";

    public SigningClient(RestTemplate rt, String signBaseUrl) {
        this.rt = rt;
        this.signBaseUrl = signBaseUrl;
    }

    public String sign(SigningPayload toSign) {
        String url = signBaseUrl + SIGNING_PATH;
        logger.info("Requesting signed cms at {}", url);
        return rt.exchange(RequestEntity.post(url).body(toSign), CmsResponse.class)
                .getBody()
                .getCms();
    }

    public String getCmsForAlias(String alias) {
        String url = signBaseUrl + String.format(ALIAS_PATH, alias);
        logger.info("Requesting cms alias {} at {}", alias, url);
        return rt.exchange(
                        RequestEntity.get(url).headers(acceptJsonHeaders()).build(),
                        CmsResponse.class)
                .getBody()
                .getCms();
    }

    public static HttpHeaders acceptJsonHeaders() {
        var headers = new HttpHeaders();
        headers.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        return headers;
    }
}
