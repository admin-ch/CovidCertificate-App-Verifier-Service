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

import ch.admin.bag.covidcertificate.backend.verifier.model.ForeignRule;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.jdbc.core.RowMapper;

public class ForeignRulesRowMapper implements RowMapper<ForeignRule> {

    @Override
    public ForeignRule mapRow(ResultSet resultSet, int i) throws SQLException {
        var rule = new ForeignRule();
        rule.setCountry(resultSet.getString("country"));
        rule.setId(resultSet.getString("rule_id"));
        rule.setVersion(resultSet.getString("rule_version"));
        rule.setContent(resultSet.getString("rule_content"));
        rule.setValidFrom(resultSet.getTimestamp("valid_from").toLocalDateTime());
        rule.setValidUntil(resultSet.getTimestamp("valid_until").toLocalDateTime());
        return rule;
    }
}
