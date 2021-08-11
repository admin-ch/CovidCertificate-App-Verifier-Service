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
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.transaction.annotation.Transactional;

public class JdbcValueSetDataServiceImpl implements ValueSetDataService {

    private final NamedParameterJdbcTemplate jt;
    private final SimpleJdbcInsert valueSetInsert;

    public JdbcValueSetDataServiceImpl(DataSource dataSource) {
        this.jt = new NamedParameterJdbcTemplate(dataSource);
        this.valueSetInsert =
                new SimpleJdbcInsert(dataSource)
                        .withTableName("t_value_set_data")
                        .usingGeneratedKeyColumns("pk_value_set_data_id", "created_at");
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
        return jt.queryForObject(
                "select json_blob from t_value_set_data"
                        + " where value_set_id = :value_set_id"
                        + " order by created_at desc"
                        + " limit 1",
                new MapSqlParameterSource("value_set_id", valueSetId),
                String.class);
    }

    @Override
    @Transactional(readOnly = false)
    public void deleteOldValueSets() {
        // TODO
    }
}
