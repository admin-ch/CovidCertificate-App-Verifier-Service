/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.backend.verifier.data.mapper;

import ch.admin.bag.covidcertificate.backend.verifier.model.AppToken;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.jdbc.core.RowMapper;

public class AppTokenRowMapper implements RowMapper<AppToken> {

    @Override
    public AppToken mapRow(ResultSet resultSet, int i) throws SQLException {
        var appToken = new AppToken();
        appToken.setApiKey(resultSet.getString("api_key"));
        appToken.setDescription(resultSet.getString("description"));
        return appToken;
    }
}
