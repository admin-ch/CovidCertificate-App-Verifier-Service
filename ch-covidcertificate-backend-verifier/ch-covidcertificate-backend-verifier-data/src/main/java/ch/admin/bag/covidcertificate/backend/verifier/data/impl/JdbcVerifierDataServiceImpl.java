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

import ch.admin.bag.covidcertificate.backend.verifier.data.VerifierDataService;
import ch.admin.bag.covidcertificate.backend.verifier.data.mapper.CSCARowMapper;
import ch.admin.bag.covidcertificate.backend.verifier.data.mapper.ClientCertRowMapper;
import ch.admin.bag.covidcertificate.backend.verifier.model.cert.CertFormat;
import ch.admin.bag.covidcertificate.backend.verifier.model.cert.ClientCert;
import ch.admin.bag.covidcertificate.backend.verifier.model.cert.db.DbCsca;
import ch.admin.bag.covidcertificate.backend.verifier.model.cert.db.DbDsc;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.transaction.annotation.Transactional;

public class JdbcVerifierDataServiceImpl implements VerifierDataService {

    private static final int MAX_DSC_BATCH_COUNT = 1000;
    private final NamedParameterJdbcTemplate jt;
    private final SimpleJdbcInsert cscaInsert;
    private final SimpleJdbcInsert dscInsert;

    public JdbcVerifierDataServiceImpl(DataSource dataSource) {
        this.jt = new NamedParameterJdbcTemplate(dataSource);
        this.cscaInsert =
                new SimpleJdbcInsert(dataSource)
                        .withTableName("t_country_specific_certificate_authority")
                        .usingGeneratedKeyColumns("pk_csca_id", "imported_at");
        this.dscInsert =
                new SimpleJdbcInsert(dataSource)
                        .withTableName("t_document_signer_certificate")
                        .usingGeneratedKeyColumns("pk_dsc_id", "imported_at");
    }

    @Override
    @Transactional
    public void insertCscas(List<DbCsca> cscas) {
        List<SqlParameterSource> batchParams = new ArrayList<>();
        if (!cscas.isEmpty()) {
            for (DbCsca dbCsca : cscas) {
                batchParams.add(getCSCAParams(dbCsca));
            }
            cscaInsert.executeBatch(
                    batchParams.toArray(new SqlParameterSource[batchParams.size()]));
        }
    }

    @Override
    @Transactional
    public int removeCscasNotIn(List<String> keyIdsToKeep) {
        final var sql = "delete from t_country_specific_certificate_authority where key_id not in (:kids)";
        final var params = new MapSqlParameterSource();
        params.addValue("kids", keyIdsToKeep);
        return jt.update(sql, params);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DbCsca> findCscas(String origin) {
        final var sql = "select * from t_country_specific_certificate_authority where origin = :origin";
        final var params = new MapSqlParameterSource();
        params.addValue("origin", origin);
        return jt.query(sql, params, new CSCARowMapper());
    }

    @Override
    public void insertDsc(List<DbDsc> dsc) {
        // TODO
    }

    @Override
    public int removeDscsNotIn(List<String> keyIdsToKeep) {
        // TODO
        return 0;
    }

    @Override
    public List<ClientCert> findDscs(Long since, CertFormat certFormat) {
        List<String> formatSpecificSelectFields;
        switch (certFormat) {
            case IOS:
                formatSpecificSelectFields = List.of("subject_public_key_info");
                break;
            case ANDROID:
                formatSpecificSelectFields = List.of("n", "e");
                break;
            default:
                throw new RuntimeException("unexpected cert format received: " + certFormat);
        }

        String sql =
                "select pk_dsc_id,"
                        + " key_id,"
                        + " origin,"
                        + " use,"
                        + " alg,"
                        + " crv,"
                        + " x,"
                        + " y, "
                        + String.join(", ", formatSpecificSelectFields)
                        + " from t_document_signer_certificate"
                        + " where pk_dsc_id > :pk_dsc_id"
                        + " order by pk_dsc_id asc"
                        + " limit "
                        + MAX_DSC_BATCH_COUNT;
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("pk_dsc_id", since);
        return jt.query(sql, params, new ClientCertRowMapper(certFormat));
    }

    @Override
    public List<String> findActiveDscKeyIds() {
        String sql = "select key_id from t_document_signer_certificate order by pk_dsc_id";
        return jt.queryForList(sql, new MapSqlParameterSource(), String.class);
    }

    @Override
    @Transactional(readOnly = true)
    public long findMaxDscPkId() {
        try {
            String sql =
                    "select pk_dsc_id from t_document_signer_certificate"
                            + " order by pk_dsc_id desc"
                            + " limit 1";
            return jt.queryForObject(sql, new MapSqlParameterSource(), Long.class);
        } catch (EmptyResultDataAccessException e) {
            return 0L;
        }
    }

    @Override
    public int getMaxDscBatchCount() {
        return MAX_DSC_BATCH_COUNT;
    }

    private MapSqlParameterSource getCSCAParams(DbCsca dbCsca) {
        var params = new MapSqlParameterSource();
        params.addValue("key_id", dbCsca.getKeyId());
        params.addValue("certificate_raw", dbCsca.getCertificateRaw());
        params.addValue("origin", dbCsca.getOrigin());
        params.addValue("subject_principal_name", dbCsca.getSubjectPrincipalName());
        return params;
    }
}
