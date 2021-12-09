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

import ch.admin.bag.covidcertificate.backend.verifier.ws.security.signature.JwsMessageConverter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles({"actuator-security"})
@SpringBootTest(
        properties = {
            "ws.monitor.prometheus.user=prometheus",
            "ws.monitor.prometheus.password=prometheus",
            "management.endpoints.enabled-by-default=true",
            "management.endpoints.web.exposure.include=*",
            "testing.disabledModes=THREE_G"
        })
@TestInstance(Lifecycle.PER_CLASS)
public class VerificationRulesControllerV2JwsTest extends VerificationRulesControllerV2Test {

    @BeforeAll
    public void setup() {
        this.acceptMediaType = JwsMessageConverter.JWS_MEDIA_TYPE;
    }
}
