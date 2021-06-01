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

import ch.admin.bag.covidcertificate.backend.verifier.model.cert.ActiveCertsResponse;
import ch.admin.bag.covidcertificate.backend.verifier.model.cert.CertFormat;
import ch.admin.bag.covidcertificate.backend.verifier.model.cert.CertsResponse;
import ch.ubique.openapi.docannotations.Documentation;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/v1")
public class VerifierController {

    private static final String NEXT_SINCE_HEADER = "X-Next-Since";
    private static final String ETAG_HEADER = "ETag";
    private static final int MAX_CERT_BATCH_SIZE = 1000;

    @Documentation(
            description = "Echo endpoint",
            responses = {"200 => Hello from CH Covidcertificate Verifier WS"})
    @CrossOrigin(origins = {"https://editor.swagger.io"})
    @GetMapping(value = "")
    public @ResponseBody String hello() {
        return "Hello from CH Covidcertificate Verifier WS";
    }

    @Documentation(
            description = "get signer certificates",
            responses = {
                "200 => next certificate batch after `since`. keep requesting until empty certs list is returned"
            },
            responseHeaders = {"X-Next-Since:`since` to set for next request:long"})
    @CrossOrigin(origins = {"https://editor.swagger.io"})
    @GetMapping(value = "certs")
    public @ResponseBody ResponseEntity<CertsResponse> getSignerCerts(
            @RequestParam(required = false) Long since, @RequestParam CertFormat certFormat) {
        return ResponseEntity.ok()
                .header(NEXT_SINCE_HEADER, "123")
                .body(new CertsResponse()); // TODO implement
    }

    @Documentation(
            description = "get all key IDs of active signer certs",
            responses = {
                "200 => list of Key IDs of all active signer certs",
                "304 => no changes since last request"
            },
            responseHeaders = {"ETag:etag to set for next request:string"})
    @CrossOrigin(origins = {"https://editor.swagger.io"})
    @GetMapping(value = "certs/active")
    public @ResponseBody ResponseEntity<ActiveCertsResponse> getActiveSignerCertKeyIds(
            @RequestHeader(value = "ETag", required = false) String etag) {
        return ResponseEntity.ok()
                .header(ETAG_HEADER, "a1b2c3")
                .body(new ActiveCertsResponse()); // TODO implement
    }
}
