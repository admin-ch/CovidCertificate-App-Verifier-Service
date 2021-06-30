/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.backend.verifier.sync.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

public class LoggingRequestInterceptor implements ClientHttpRequestInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingRequestInterceptor.class);

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        traceRequest(request, body);
        ClientHttpResponse response = execution.execute(request, body);
        return traceResponse(response);
    }

    private void traceRequest(HttpRequest request, byte[] body) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(
                    "==========================request begin==============================================");
            LOGGER.debug("{} {}", request.getMethod(), request.getURI());
            LOGGER.debug("Headers: {}", request.getHeaders());
            LOGGER.debug("Body:    {}", new String(body, StandardCharsets.UTF_8));
            LOGGER.debug(
                    "==========================request end================================================");
        }
    }

    private ClientHttpResponse traceResponse(ClientHttpResponse response) throws IOException {
        if (LOGGER.isDebugEnabled()) {
            final ClientHttpResponse responseWrapper =
                    new BufferingClientHttpResponseWrapper(response);
            var inputStringBuilder = new StringBuilder();
            var bufferedReader =
                    new BufferedReader(
                            new InputStreamReader(
                                    responseWrapper.getBody(), StandardCharsets.UTF_8));
            String line = bufferedReader.readLine();
            while (line != null) {
                inputStringBuilder.append(line);
                inputStringBuilder.append('\n');
                line = bufferedReader.readLine();
            }
            LOGGER.debug(
                    "==========================response begin=============================================");
            LOGGER.debug("Status:  {}", responseWrapper.getStatusCode());
            LOGGER.debug("Headers: {}", responseWrapper.getHeaders());
            LOGGER.debug(
                    "Body:    {}",
                    inputStringBuilder
                            .toString()
                            .replaceAll("\"thumbprint\":\".*?(?=\")\"", "\"thumbprint\":\"...\"")
                            .replaceAll("\"signature\":\".*?(?=\")\"", "\"signature\":\"...\"")
                            .replaceAll("\"rawData\":\".*?(?=\")\"", "\"rawData\":\"...\"")
                            .replace("{", "\n{"));
            LOGGER.debug(
                    "==========================response end===============================================");
            return responseWrapper;
        } else {
            return response;
        }
    }
}
