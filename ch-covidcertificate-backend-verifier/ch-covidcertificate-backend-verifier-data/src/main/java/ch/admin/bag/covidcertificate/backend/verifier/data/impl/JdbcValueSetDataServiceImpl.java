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

import ch.admin.bag.covidcertificate.backend.verifier.data.ValueSetDataService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.transaction.annotation.Transactional;

public class JdbcValueSetDataServiceImpl implements ValueSetDataService {

    private static final Logger logger = LoggerFactory.getLogger(JdbcValueSetDataServiceImpl.class);

    private final NamedParameterJdbcTemplate jt;
    private final SimpleJdbcInsert valueSetInsert;
    private final int maxHistory;

    public JdbcValueSetDataServiceImpl(DataSource dataSource, int maxHistory) {
        this.jt = new NamedParameterJdbcTemplate(dataSource);
        this.valueSetInsert =
                new SimpleJdbcInsert(dataSource)
                        .withTableName("t_value_set_data")
                        .usingGeneratedKeyColumns("pk_value_set_data_id", "created_at");
        this.maxHistory = maxHistory;
    }

    @Override
    @Transactional(readOnly = false)
    public void insertValueSets(Map<String, String> valueSetsById) throws JsonProcessingException {
        if (valueSetsById != null && !valueSetsById.isEmpty()) {
            valueSetInsert.executeBatch(createValueSetBatchParams(valueSetsById));
        }
    }

    private SqlParameterSource[] createValueSetBatchParams(Map<String, String> valueSetsById)
            throws JsonProcessingException {
        List<SqlParameterSource> result = new ArrayList<>();
        for (Entry<String, String> valueSetIdToJsonBlob : valueSetsById.entrySet()) {
            result.add(
                    createValueSetParams(
                            valueSetIdToJsonBlob.getKey(), valueSetIdToJsonBlob.getValue()));
        }
        return result.toArray(new SqlParameterSource[result.size()]);
    }

    private SqlParameterSource createValueSetParams(String valueSetId, String jsonBlob)
            throws JsonProcessingException {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("value_set_id", valueSetId);
        Map valueSets = new ObjectMapper().readValue(jsonBlob, Map.class);
        params.addValue("value_set_date", Date.valueOf(((String) valueSets.get("valueSetDate"))));
        params.addValue("json_blob", jsonBlob);
        return params;
    }

    @Override
    @Transactional(readOnly = true)
    public String findLatestValueSet(String valueSetId) {
        try {
            return jt.queryForObject(
                    "select json_blob from t_value_set_data"
                            + " where value_set_id = :value_set_id"
                            + " order by created_at desc"
                            + " limit 1",
                    new MapSqlParameterSource("value_set_id", valueSetId),
                    String.class);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    @Override
    @Transactional(readOnly = false)
    public void deleteOldValueSets() {
        List<String> valueSetIds = findAllValueSetIds();
        for (String valueSetId : valueSetIds) {
            String pksToKeepSubquery =
                    "select pk_value_set_data_id from t_value_set_data"
                            + " where value_set_id = :value_set_id"
                            + " order by created_at desc"
                            + " limit :max_history";
            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue("value_set_id", valueSetId);
            params.addValue("max_history", maxHistory);
            int removed =
                    jt.update(
                            "delete from t_value_set_data"
                                    + " where value_set_id = :value_set_id and pk_value_set_data_id not in ("
                                    + pksToKeepSubquery
                                    + ")",
                            params);
            logger.debug("removed {} old entries for valueSetId {}", removed, valueSetId);
        }
    }

    private List<String> findAllValueSetIds() {
        return jt.queryForList(
                "select distinct value_set_id from t_value_set_data",
                new MapSqlParameterSource(),
                String.class);
    }
}
