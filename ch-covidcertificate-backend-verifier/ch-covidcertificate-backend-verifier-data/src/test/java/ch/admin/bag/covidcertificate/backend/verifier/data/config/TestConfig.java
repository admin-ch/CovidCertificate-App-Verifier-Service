/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.backend.verifier.data.config;

import ch.admin.bag.covidcertificate.backend.verifier.data.AppTokenDataService;
import ch.admin.bag.covidcertificate.backend.verifier.data.ForeignRulesDataService;
import ch.admin.bag.covidcertificate.backend.verifier.data.RevokedCertDataService;
import ch.admin.bag.covidcertificate.backend.verifier.data.VerifierDataService;
import ch.admin.bag.covidcertificate.backend.verifier.data.impl.JdbcAppTokenDataServiceImpl;
import ch.admin.bag.covidcertificate.backend.verifier.data.impl.JdbcForeignRulesDataServiceImpl;
import ch.admin.bag.covidcertificate.backend.verifier.data.impl.JdbcRevokedCertDataServiceImpl;
import ch.admin.bag.covidcertificate.backend.verifier.data.impl.JdbcVerifierDataServiceImpl;
import ch.admin.bag.covidcertificate.backend.verifier.data.util.CacheUtil;
import java.time.Duration;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

@Profile("test")
@TestConfiguration
public class TestConfig {

    @Value("${ws.keys.batch-size:1000}")
    protected Integer dscBatchSize;

    @Bean
    public VerifierDataService verifierDataService(
            DataSource dataSource,
            @Value("${dsc.deleted.keep.duration:P7D}") Duration keepDscsMarkedForDeletionDuration) {
        return new JdbcVerifierDataServiceImpl(
                dataSource, dscBatchSize, keepDscsMarkedForDeletionDuration);
    }

    @Bean
    public AppTokenDataService appTokenDataService(DataSource dataSource) {
        return new JdbcAppTokenDataServiceImpl(dataSource);
    }

    @Bean
    public RevokedCertDataService revokedCertDataService(
            DataSource dataSource,
            @Value("${revocationList.batch-size:20000}") Integer revokedCertBatchSize) {
        return new JdbcRevokedCertDataServiceImpl(dataSource, revokedCertBatchSize);
    }

    @Bean
    public ForeignRulesDataService foreignRulesDataService(
            DataSource dataSource
    ){
        return new JdbcForeignRulesDataServiceImpl(dataSource);
    }

    @Value("${ws.revocation-list.retention-bucket-duration:PT6H}")
    public void setRevocationRetentionBucketDuration(Duration bucketDuration) {
        CacheUtil.REVOCATION_RETENTION_BUCKET_DURATION = bucketDuration;
    }
}
