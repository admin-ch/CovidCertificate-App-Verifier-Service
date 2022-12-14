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

import java.util.Map;
import javax.sql.DataSource;
import javax.xml.crypto.Data;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.pivotal.cfenv.jdbc.CfJdbcEnv;
import io.pivotal.cfenv.jdbc.CfJdbcService;

@Configuration
public abstract class SyncCloudBaseConfig extends SyncBaseConfig {

    @Value("${datasource.maximumPoolSize:5}")
    int dataSourceMaximumPoolSize;

    @Value("${datasource.connectionTimeout:30000}")
    int dataSourceConnectionTimeout;

    @Value("${datasource.leakDetectionThreshold:0}")
    int dataSourceLeakDetectionThreshold;
/*
    @Bean
    @Override
    public DataSource dataSource() {
        CfJdbcEnv cfJdbcEnv = new CfJdbcEnv();
        CfJdbcService cfJdbcService = cfJdbcEnv.findJdbcService();

        String jdbcUrl = cfJdbcService.getJdbcUrl();
        String username = cfJdbcService.getUsername();
        String password = cfJdbcService.getPassword();
        String driverClassName = cfJdbcService.getDriverClassName();
        
    }
*/
    @Bean
    @Override
    public Flyway flyway(DataSource dataSource) {
        Flyway flyWay =
                Flyway.configure()
                        .dataSource(dataSource)
                        .locations("classpath:/db/migration/pgsql_cluster")
                        .load();
        flyWay.migrate();
        return flyWay;
    }

    @Override
    public String getDbType() {
        return "pgsql";
    }
}
