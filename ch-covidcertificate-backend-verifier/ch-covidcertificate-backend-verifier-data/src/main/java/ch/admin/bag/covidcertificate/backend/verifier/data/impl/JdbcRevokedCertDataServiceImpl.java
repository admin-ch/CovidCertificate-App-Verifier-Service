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
import ch.admin.bag.covidcertificate.backend.verifier.data.util.CacheUtil;
import ch.admin.bag.covidcertificate.backend.verifier.model.DbRevokedCert;
import ch.admin.bag.covidcertificate.backend.verifier.model.cert.db.RevokedCertsUpdateResponse;
import java.time.Instant;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.apache.commons.collections4.ListUtils;
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
                        .usingGeneratedKeyColumns("pk_revoked_cert_id", "imported_at");
    }

    @Transactional(readOnly = false)
    @Override
    public RevokedCertsUpdateResponse replaceRevokedCerts(Set<String> revokedUvcis) {
        Set<String> existingUvcis = findAllRevokedUvcis();
        int insertCount = insertNewRevokedCerts(revokedUvcis, existingUvcis);
        int removeCount = removeRevokedCertsNotIn(revokedUvcis, existingUvcis);
        return new RevokedCertsUpdateResponse(insertCount, removeCount);
    }

    private int insertNewRevokedCerts(Set<String> revokedUvcis, Set<String> existingUvcis) {
        if (revokedUvcis != null && !revokedUvcis.isEmpty()) {
            List<String> toInsert =
                    revokedUvcis.stream()
                            .filter(uvci -> !existingUvcis.contains(uvci))
                            .collect(Collectors.toList());

            // insert in batches
            final int maxBatchSize = 10000;
            for (List<String> batchToInsert : ListUtils.partition(toInsert, maxBatchSize)) {
                revokedCertInsert.executeBatch(createParams(batchToInsert));
            }

            return toInsert.size();
        } else {
            return 0;
        }
    }

    private Set<String> findAllRevokedUvcis() {
        return new HashSet<>(
                jt.queryForList(
                        "select distinct uvci from t_revoked_cert",
                        new MapSqlParameterSource(),
                        String.class));
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

    private int removeRevokedCertsNotIn(Set<String> uvcisToKeep, Set<String> existingUvcis) {
        String sql = "delete from t_revoked_cert";
        if (uvcisToKeep != null && !uvcisToKeep.isEmpty()) {
            sql += " where uvci in (:to_delete)";

            List<String> toDelete =
                    existingUvcis.stream()
                            .filter(uvci -> !uvcisToKeep.contains(uvci))
                            .collect(Collectors.toList());

            // delete in batches
            int deleteCount = 0;
            final int maxBatchSize = 10000;
            for (List<String> batchToDelete : ListUtils.partition(toDelete, maxBatchSize)) {
                deleteCount +=
                        jt.update(sql, new MapSqlParameterSource("to_delete", batchToDelete));
            }
            return deleteCount;
        } else {
            return jt.update(sql, new MapSqlParameterSource());
        }
    }

    @Transactional(readOnly = true)
    @Override
    public List<DbRevokedCert> findReleasedRevokedCerts(Long since, Instant now) {
        if (since == null) {
            since = 0L;
        }
        String sql =
                "select pk_revoked_cert_id, uvci from t_revoked_cert"
                        + " where pk_revoked_cert_id > :since"
                        + " and imported_at <= :release_up_to"
                        + " order by pk_revoked_cert_id asc"
                        + " limit :batch_size";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("since", since);
        params.addValue("batch_size", revokedCertBatchSize);
        params.addValue(
                "release_up_to",
                Date.from(CacheUtil.roundToPreviousRevocationRetentionBucketStart(now)));
        return jt.query(sql, params, new RevokedCertRowMapper());
    }

    @Transactional(readOnly = true)
    @Override
    public long findMaxReleasedRevokedCertPkId(Instant now) {
        try {
            String sql =
                    "select pk_revoked_cert_id from t_revoked_cert"
                            + " where imported_at <= :release_up_to"
                            + " order by pk_revoked_cert_id desc"
                            + " limit 1";
            MapSqlParameterSource params =
                    new MapSqlParameterSource(
                            "release_up_to",
                            Date.from(
                                    CacheUtil.roundToPreviousRevocationRetentionBucketStart(now)));
            return jt.queryForObject(sql, params, Long.class);
        } catch (EmptyResultDataAccessException e) {
            return 0L;
        }
    }

    public int getRevokedCertBatchSize() {
        return revokedCertBatchSize;
    }
}
