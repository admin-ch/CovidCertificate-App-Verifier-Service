/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.backend.verifier.ws.controller.dev;

import ch.ubique.openapi.docannotations.Documentation;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("")
@Documentation(description = "mocks endpoints that are not available during local development")
public class DevController {

    @GetMapping(value = "/v1/revocation-list")
    public @ResponseBody ResponseEntity<List<String>> getMockRevokedCerts() {
        List<String> response = new ArrayList<>();
        for (int i = 0; i < 100010; i++) {
            response.add("urn:uvci:01:CH:MOCK" + i);
        }
        return ResponseEntity.ok(response);
    }
}
