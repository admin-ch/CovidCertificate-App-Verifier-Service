/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.backend.verifier.data.impl;

import ch.admin.bag.covidcertificate.backend.verifier.data.ForeignRulesDataService;
import ch.admin.bag.covidcertificate.backend.verifier.data.mapper.ForeignRulesRowMapper;
import ch.admin.bag.covidcertificate.backend.verifier.model.ForeignRule;
import java.time.LocalDateTime;
import java.util.List;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

public class JdbcForeignRulesDataServiceImpl implements ForeignRulesDataService {

    private static final Logger logger =
            LoggerFactory.getLogger(JdbcForeignRulesDataServiceImpl.class);

    private final NamedParameterJdbcTemplate jt;

    public JdbcForeignRulesDataServiceImpl(DataSource dataSource) {
        this.jt = new NamedParameterJdbcTemplate(dataSource);
    }

    @Override
    public List<String> getCountries() {
        String sql = "select distinct country from t_foreign_rules";
        return jt.queryForList(sql, new MapSqlParameterSource(), String.class);
    }

    @Override
    public List<ForeignRule> getRulesForCountry(String country) {
        String sql =
                "select country, rule_id, rule_version, rule_content, valid_from, valid_until from t_foreign_rules where country=:country";
        var params = new MapSqlParameterSource();
        params.addValue("country", country);
        return jt.query(sql, params, new ForeignRulesRowMapper());
    }

    @Override
    public void insertRule(ForeignRule rule) {
        String sql =
                "insert into t_foreign_rules"
                        + "(country, rule_id, rule_version, rule_content, valid_from, valid_until, inserted_at) "
                        + "values(:country, :rule_id, :rule_version, :rule_content, :valid_from, :valid_until, :inserted_at)";
        var params = new MapSqlParameterSource();
        params.addValue("country", rule.getCountry());
        params.addValue("rule_id", rule.getId());
        params.addValue("rule_version", rule.getVersion());
        params.addValue("rule_content", rule.getContent());
        params.addValue("valid_from", rule.getValidFrom());
        params.addValue("valid_until", rule.getValidUntil());
        params.addValue("inserted_at", LocalDateTime.now());
        jt.update(sql, params);
    }

    @Override
    public void removeRuleSet(String country) {
        String sql = "delete from t_foreign_rules where country=:country";
        var params = new MapSqlParameterSource();
        params.addValue("country", country);
        jt.update(sql, params);
    }
}
