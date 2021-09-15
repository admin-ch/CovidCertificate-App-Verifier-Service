/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.backend.verifier.sync.ws;

import ch.ubique.openapi.docannotations.Documentation;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

@Controller
@RequestMapping("dgcHub")
public class DgcHubProxy {

    private final String baseUrl;
    private final RestTemplate rt;

    public DgcHubProxy(String baseUrl, RestTemplate rt) {
        this.baseUrl = baseUrl;
        this.rt = rt;
    }

    @Documentation(
            description = "Echo endpoint",
            responses = {"200 => Hello from internal DGC hub proxy"})
    @CrossOrigin(origins = {"https://editor.swagger.io"})
    @GetMapping(value = "")
    public @ResponseBody String hello() {
        return "Hello from internal DGC hub proxy";
    }

    @Documentation(description = "internal endpoint for proxy to dgc hub")
    @GetMapping(value = "proxy", produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<String> proxyRequest(
            @RequestParam(required = true) String endpoint) {
        if (!endpoint.startsWith("/")) {
            endpoint = "/" + endpoint;
        }
        try {

            String body =
                    rt.exchange(
                                    baseUrl + endpoint,
                                    HttpMethod.GET,
                                    createHttpEntity(),
                                    String.class)
                            .getBody();
            return ResponseEntity.ok(body);
        } catch (HttpStatusCodeException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    private HttpEntity<Void> createHttpEntity() {
        var headers = new HttpHeaders();
        headers.add(HttpHeaders.ACCEPT, "application/json");
        return new HttpEntity<>(headers);
    }
}
