/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.backend.verifier.ws.utils;

import java.util.ArrayList;
import java.util.List;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

public class RestTemplateHelper {

    private static final String COVIDCERT_VERIFIER = "covidcert-verifier";
    private static final int CONNECT_TIMEOUT = 20000;
    private static final int SOCKET_TIMEOUT = 20000;

    private RestTemplateHelper() {
        throw new IllegalStateException("Utility class");
    }

    public static RestTemplate getRestTemplate() {
        return buildRestTemplate();
    }

    private static RestTemplate buildRestTemplate() {
        var rt = new RestTemplate(new HttpComponentsClientHttpRequestFactory(httpClient()));
        List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();
        interceptors.add(new LoggingRequestInterceptor());
        rt.setInterceptors(interceptors);
        return rt;
    }

    private static CloseableHttpClient httpClient() {
        var manager = new PoolingHttpClientConnectionManager();
        manager.setMaxTotal(30);
        manager.setDefaultMaxPerRoute(20);

        HttpClientBuilder builder = HttpClients.custom();
        builder.useSystemProperties()
                .setUserAgent(COVIDCERT_VERIFIER)
                .setConnectionManager(manager)
                .disableCookieManagement()
                .setDefaultRequestConfig(
                        RequestConfig.custom()
                                .setConnectTimeout(CONNECT_TIMEOUT)
                                .setSocketTimeout(SOCKET_TIMEOUT)
                                .build());
        return builder.build();
    }
}
