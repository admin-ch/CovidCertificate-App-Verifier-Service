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

import ch.admin.bag.covidcertificate.backend.verifier.data.VerifierDataService;
import ch.admin.bag.covidcertificate.backend.verifier.model.cert.ActiveCertsResponse;
import ch.admin.bag.covidcertificate.backend.verifier.model.cert.CertFormat;
import ch.admin.bag.covidcertificate.backend.verifier.model.cert.CertsResponse;
import ch.admin.bag.covidcertificate.backend.verifier.model.cert.ClientCert;
import ch.admin.bag.covidcertificate.backend.verifier.ws.utils.CacheUtil;
import ch.admin.bag.covidcertificate.backend.verifier.ws.utils.EtagUtil;
import ch.ubique.openapi.docannotations.Documentation;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.WebRequest;

@Controller
@RequestMapping("trust/v2/keys")
public class KeyControllerV2 {

    private static final String NEXT_SINCE_HEADER = "X-Next-Since";
    private static final String UP_TO_DATE_HEADER = "up-to-date";
    private static final String UP_TO_HEADER = "up-to";

    private final VerifierDataService verifierDataService;

    public KeyControllerV2(VerifierDataService verifierDataService) {
        this.verifierDataService = verifierDataService;
    }

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
                "200 => next certificate batch after `since` up to `upTo` (optional). keep requesting until `up-to-date` header is `true`"
            },
            responseHeaders = {
                "X-Next-Since:`since` to set for next request:string",
                "up-to-date:set to 'true' when no more certs to fetch:string"
            })
    @CrossOrigin(origins = {"https://editor.swagger.io"})
    @GetMapping(value = "updates")
    public @ResponseBody ResponseEntity<CertsResponse> getSignerCerts(
            @RequestParam(required = false, defaultValue = "0") Long since,
            @RequestParam Long upTo,
            @RequestParam CertFormat certFormat) {
        List<ClientCert> dscs = verifierDataService.findDscs(since, certFormat, upTo);
        return ResponseEntity.ok()
                .headers(getKeysUpdatesHeaders(dscs, upTo))
                .cacheControl(CacheControl.maxAge(CacheUtil.KEYS_UPDATES_MAX_AGE))
                .body(new CertsResponse(dscs));
    }

    private HttpHeaders getKeysUpdatesHeaders(List<ClientCert> dscs, Long upTo) {
        HttpHeaders headers = new HttpHeaders();
        long maxDscPkId = upTo != null ? upTo : verifierDataService.findMaxDscPkId();
        Long nextSince = dscs.stream().mapToLong(ClientCert::getPkId).max().orElse(maxDscPkId);
        headers.add(NEXT_SINCE_HEADER, nextSince.toString());
        if (nextSince >= maxDscPkId) {
            headers.add(UP_TO_DATE_HEADER, "true");
        }
        return headers;
    }

    @Documentation(
            description = "get all key IDs of active signer certs",
            responses = {
                "200 => list of Key IDs of all active signer certs",
                "304 => no changes since last request"
            },
            responseHeaders = {
                "ETag:etag to set for next request:string",
                "up-to: `upTo` to set for next keys/update request:string"
            })
    @CrossOrigin(origins = {"https://editor.swagger.io"})
    @GetMapping(value = "list")
    public @ResponseBody ResponseEntity<ActiveCertsResponse> getActiveSignerCertKeyIds(
            WebRequest request) {
        long maxDscPkId = verifierDataService.findMaxDscPkId();
        List<String> activeKeyIds = verifierDataService.findActiveDscKeyIds();

        // check etag
        String currentEtag = String.valueOf(EtagUtil.getUnsortedListHashcode(activeKeyIds));
        if (request.checkNotModified(currentEtag)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
        }

        return ResponseEntity.ok()
                .headers(getKeysListHeaders(maxDscPkId))
                .cacheControl(CacheControl.maxAge(CacheUtil.KEYS_LIST_MAX_AGE))
                .body(new ActiveCertsResponse(activeKeyIds));
    }

    private HttpHeaders getKeysListHeaders(Long upTo) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(UP_TO_HEADER, upTo.toString());
        return headers;
    }
}