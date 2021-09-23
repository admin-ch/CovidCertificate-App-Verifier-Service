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
import org.springframework.http.RequestEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

public class SigningClient {
    private static final Logger logger = LoggerFactory.getLogger(SigningClient.class);

    private final RestTemplate rt;
    private final String signBaseUrl;
    private static final String SIGNING_PATH = "/v1/cms/";

    public SigningClient(RestTemplate rt, String signBaseUrl) {
        this.rt = rt;
        this.signBaseUrl = signBaseUrl;
    }

    public CmsResponse getCms(SigningPayload toSign) {
        try {
            String url = signBaseUrl + SIGNING_PATH;
            logger.info("Requesting signed cms at {}", url);
            return rt.exchange(RequestEntity.post(url).body(toSign), CmsResponse.class)
                    .getBody();
        } catch (HttpStatusCodeException e) {
            logger.error("Signing request failed", e);
            return null;
        }
    }
}
