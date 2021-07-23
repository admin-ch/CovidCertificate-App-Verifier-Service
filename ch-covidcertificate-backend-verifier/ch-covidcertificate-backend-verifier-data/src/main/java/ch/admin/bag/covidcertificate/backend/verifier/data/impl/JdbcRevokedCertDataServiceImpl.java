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

import ch.admin.bag.covidcertificate.backend.verifier.data.RevokedCertDataService;
import ch.admin.bag.covidcertificate.backend.verifier.data.mapper.RevokedCertRowMapper;
import ch.admin.bag.covidcertificate.backend.verifier.model.DbRevokedCert;
import ch.admin.bag.covidcertificate.backend.verifier.model.cert.db.RevokedCertsUpdateResponse;
import java.util.List;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.transaction.annotation.Transactional;

public class JdbcRevokedCertDataServiceImpl implements RevokedCertDataService {

    private static final Logger logger =
            LoggerFactory.getLogger(JdbcRevokedCertDataServiceImpl.class);

    private final int revokedCertBatchSize;
    private final NamedParameterJdbcTemplate jt;
    private final SimpleJdbcInsert revokedCertInsert;

    public JdbcRevokedCertDataServiceImpl(DataSource dataSource, int revokedCertBatchSize) {
        this.jt = new NamedParameterJdbcTemplate(dataSource);
        this.revokedCertBatchSize = revokedCertBatchSize;
        this.revokedCertInsert =
                new SimpleJdbcInsert(dataSource)
                        .withTableName("t_revoked_cert")
                        .usingGeneratedKeyColumns("pk_revoked_cert_id");
    }

    @Transactional(readOnly = false)
    @Override
    public RevokedCertsUpdateResponse replaceRevokedCerts(List<String> revokedUvcis) {
        int insertCount = insertNewRevokedCerts(revokedUvcis);
        int removeCount = removeRevokedCertsNotIn(revokedUvcis);
        return new RevokedCertsUpdateResponse(insertCount, removeCount);
    }

    private int insertNewRevokedCerts(List<String> revokedUvcis) {
        if (revokedUvcis != null && !revokedUvcis.isEmpty()) {
            List<String> existingUvcis =
                    jt.queryForList(
                            "select uvci from t_revoked_cert",
                            new MapSqlParameterSource(),
                            String.class);
            List<String> toInsert =
                    revokedUvcis.stream()
                            .filter(uvci -> !existingUvcis.contains(uvci))
                            .collect(Collectors.toList());
            revokedCertInsert.executeBatch(createParams(toInsert));
            return toInsert.size();
        } else {
            return 0;
        }
    }

    private MapSqlParameterSource[] createParams(List<String> revokedUvcis) {
        if (revokedUvcis == null) {
            return null;
        }

        int size = revokedUvcis.size();
        MapSqlParameterSource[] params = new MapSqlParameterSource[size];
        for (int i = 0; i < size; i++) {
            params[i] = new MapSqlParameterSource("uvci", revokedUvcis.get(i));
        }
        return params;
    }

    private int removeRevokedCertsNotIn(List<String> revokedUvcis) {
        String sql = "delete from t_revoked_cert";
        if (revokedUvcis != null && !revokedUvcis.isEmpty()) {
            sql += " where uvci not in (:to_keep)";
        }
        return jt.update(sql, new MapSqlParameterSource("to_keep", revokedUvcis));
    }

    @Transactional(readOnly = true)
    @Override
    public List<DbRevokedCert> findRevokedCerts(Long since) {
        if (since == null) {
            since = 0L;
        }
        String sql =
                "select pk_revoked_cert_id, uvci from t_revoked_cert"
                        + " where pk_revoked_cert_id > :since"
                        + " order by pk_revoked_cert_id asc"
                        + " limit :batch_size";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("since", since);
        params.addValue("batch_size", revokedCertBatchSize);
        return jt.query(sql, params, new RevokedCertRowMapper());
    }

    @Transactional(readOnly = true)
    @Override
    public long findMaxRevokedCertPkId() {
        try {
            String sql =
                    "select pk_revoked_cert_id from t_revoked_cert"
                            + " order by pk_revoked_cert_id desc"
                            + " limit 1";
            return jt.queryForObject(sql, new MapSqlParameterSource(), Long.class);
        } catch (EmptyResultDataAccessException e) {
            return 0L;
        }
    }
}
