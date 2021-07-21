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

import ch.admin.bag.covidcertificate.backend.verifier.data.RevokedCertDataService;
import ch.admin.bag.covidcertificate.backend.verifier.model.DbRevokedCert;
import ch.admin.bag.covidcertificate.backend.verifier.model.RevocationResponse;
import ch.admin.bag.covidcertificate.backend.verifier.ws.utils.CacheUtil;
import ch.ubique.openapi.docannotations.Documentation;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.HttpStatusCodeException;

@Controller
@RequestMapping("trust/v2")
@Documentation(description = "Endpoint to obtain the list of revoked certificates")
public class RevocationListControllerV2 {

    private static final Logger logger = LoggerFactory.getLogger(RevocationListControllerV2.class);

    private static final String NEXT_SINCE_HEADER = "X-Next-Since";
    private static final String UP_TO_DATE_HEADER = "up-to-date";

    private final RevokedCertDataService revokedCertDataService;

    public RevocationListControllerV2(RevokedCertDataService revokedCertDataService) {
        this.revokedCertDataService = revokedCertDataService;
    }

    @Documentation(
            description = "get list of revoked certificates",
            responses = {"200 => next batch of revoked certificates"},
            responseHeaders = {
                "X-Next-Since:`since` to set for next request:string",
                "up-to-date:set to 'true' when no more certs to fetch:string"
            })
    @CrossOrigin(origins = {"https://editor.swagger.io"})
    @GetMapping(value = "/revocationList")
    public @ResponseBody ResponseEntity<RevocationResponse> getRevokedCerts(
            @RequestParam(required = false, defaultValue = "0") Long since)
            throws HttpStatusCodeException {
        List<DbRevokedCert> revokedCerts = revokedCertDataService.findRevokedCerts(since);
        List<String> revokedUvcis =
                revokedCerts.stream().map(DbRevokedCert::getUvci).collect(Collectors.toList());
        return ResponseEntity.ok()
                .headers(getRevokedCertsHeaders(revokedCerts))
                .cacheControl(CacheControl.maxAge(CacheUtil.REVOCATION_LIST_MAX_AGE))
                .body(new RevocationResponse(revokedUvcis));
    }

    private HttpHeaders getRevokedCertsHeaders(List<DbRevokedCert> revokedCerts) {
        HttpHeaders headers = new HttpHeaders();
        long maxPkId = revokedCertDataService.findMaxRevokedCertPkId();
        Long nextSince =
                revokedCerts.stream().mapToLong(DbRevokedCert::getPkId).max().orElse(maxPkId);
        headers.add(NEXT_SINCE_HEADER, nextSince.toString());
        if (nextSince >= maxPkId) {
            headers.add(UP_TO_DATE_HEADER, "true");
        }
        return headers;
    }
}
