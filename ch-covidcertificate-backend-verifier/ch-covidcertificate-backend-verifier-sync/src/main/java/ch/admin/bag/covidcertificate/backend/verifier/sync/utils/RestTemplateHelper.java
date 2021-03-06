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

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContexts;
import org.springframework.core.io.ClassPathResource;
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
        return buildRestTemplate(null, null);
    }

    public static RestTemplate getRestTemplateWithClientCerts(
            String authClientCert, String authClientCertPassword) {
        return buildRestTemplate(authClientCert, authClientCertPassword);
    }

    private static RestTemplate buildRestTemplate(
            String authClientCert, String authClientCertPassword) {
        RestTemplate rt = null;
        try {
            rt =
                    new RestTemplate(
                            new HttpComponentsClientHttpRequestFactory(
                                    httpClient(authClientCert, authClientCertPassword)));
        } catch (IOException
                | KeyManagementException
                | UnrecoverableKeyException
                | NoSuchAlgorithmException
                | KeyStoreException
                | CertificateException e) {
            throw new SecureConnectionException(
                    "Encountered exception while trying to setup mTLS connection: "
                            + e.getMessage());
        }
        List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();
        interceptors.add(new LoggingRequestInterceptor());
        rt.setInterceptors(interceptors);
        return rt;
    }

    private static CloseableHttpClient httpClient(String clientCert, String clientCertPassword)
            throws IOException, KeyManagementException, UnrecoverableKeyException,
                    NoSuchAlgorithmException, KeyStoreException, CertificateException {
        var manager = new PoolingHttpClientConnectionManager();

        HttpClientBuilder builder = HttpClients.custom();
        builder.useSystemProperties().setUserAgent(COVIDCERT_VERIFIER);

        if (clientCert != null && clientCertPassword != null) {
            InputStream clientCertStream = getInputStream(clientCert);
            var cf = KeyStore.getInstance("pkcs12");
            cf.load(clientCertStream, clientCertPassword.toCharArray());
            final var alias = cf.aliases().nextElement();
            var sslContext =
                    SSLContexts.custom()
                            .loadKeyMaterial(
                                    cf,
                                    clientCertPassword.toCharArray(),
                                    (map, socket) -> alias)
                            .build();
            builder.setSSLContext(sslContext);

            var sslsf = new SSLConnectionSocketFactory(sslContext);
            Registry<ConnectionSocketFactory> socketFactoryRegistry =
                    RegistryBuilder.<ConnectionSocketFactory>create()
                            .register("https", sslsf)
                            .register("http", PlainConnectionSocketFactory.INSTANCE)
                            .build();
            manager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
        }
        manager.setDefaultMaxPerRoute(20);
        manager.setMaxTotal(30);

        builder.setConnectionManager(manager)
                .disableCookieManagement()
                .setDefaultRequestConfig(
                        RequestConfig.custom()
                                .setConnectTimeout(CONNECT_TIMEOUT)
                                .setSocketTimeout(SOCKET_TIMEOUT)
                                .build());
        return builder.build();
    }

    public static InputStream getInputStream(String path) throws IOException {
        InputStream inputStream = null;
        final var base64Protocol = "base64:/";
        if (path.startsWith(base64Protocol)) {
            byte[] decodedBytes = Base64.getDecoder().decode(path.replace(base64Protocol, ""));
            inputStream = new ByteArrayInputStream(decodedBytes);
        } else if (path.startsWith("classpath:/")) {
            inputStream = classPathInputStream(path.substring(11));
        } else {
            inputStream = new FileInputStream(Paths.get(path).toFile());
        }
        return inputStream;
    }

    private static InputStream classPathInputStream(String src) throws IOException {
        return new ClassPathResource(src).getInputStream();
    }
}
