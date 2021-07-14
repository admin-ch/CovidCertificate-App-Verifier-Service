/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.backend.verifier.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;

class AppTokenDataServiceTest extends BaseDataServiceTest {

    @Autowired AppTokenDataService appTokenDataService;
    @Autowired DataSource dataSource;

    @Test
    void getAppTokensTest() {
        var appTokens = appTokenDataService.getAppTokens();
        assertTrue(appTokens.isEmpty());
        final var insert = new SimpleJdbcInsert(dataSource).withTableName("t_app_tokens");
        final var params = new MapSqlParameterSource();
        params.addValue("api_key", "4d1d5663-b4ef-46a5-85b6-3d1d376429da");
        params.addValue("description", "local");
        insert.execute(params);
        appTokens = appTokenDataService.getAppTokens();
        assertEquals(1, appTokens.size());
    }
}
