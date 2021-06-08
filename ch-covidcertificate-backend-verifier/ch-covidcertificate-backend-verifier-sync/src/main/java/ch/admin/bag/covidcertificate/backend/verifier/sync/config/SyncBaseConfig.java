/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.backend.verifier.sync.config;

import ch.admin.bag.covidcertificate.backend.verifier.data.VerifierDataService;
import ch.admin.bag.covidcertificate.backend.verifier.data.impl.JdbcVerifierDataServiceImpl;
import ch.admin.bag.covidcertificate.backend.verifier.sync.syncer.DGCClient;
import ch.admin.bag.covidcertificate.backend.verifier.sync.syncer.DGCSyncer;
import ch.admin.bag.covidcertificate.backend.verifier.sync.utils.RestTemplateHelper;
import java.util.List;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Configuration
public abstract class SyncBaseConfig {

    @Value("${dgc.baseurl}")
    String baseurl;

    @Value("${dgc.clientcert}")
    String authClientCert;

    @Value("${dgc.clientcert.password}")
    String authClientCertPassword;

    public abstract DataSource dataSource();

    public abstract Flyway flyway();

    public abstract String getDbType();

    @Bean
    public RestTemplate restTemplate() {
        final var rt = RestTemplateHelper.getRestTemplateWithClientCerts(
            authClientCert,
            authClientCertPassword,
            List.of(UriComponentsBuilder.fromHttpUrl(baseurl).build().toUri().getHost()));
        return rt;
    }

    @Bean
    public VerifierDataService verifierDataService(DataSource dataSource) {
        return new JdbcVerifierDataServiceImpl(dataSource);
    }

    @Bean
    public DGCClient dgcClient(RestTemplate restTemplate) {
        return new DGCClient(baseurl, restTemplate);
    }

    @Bean
    public DGCSyncer dgcSyncer(DGCClient dgcClient, VerifierDataService verifierDataService) {
        return new DGCSyncer(dgcClient, verifierDataService);
    }
}
