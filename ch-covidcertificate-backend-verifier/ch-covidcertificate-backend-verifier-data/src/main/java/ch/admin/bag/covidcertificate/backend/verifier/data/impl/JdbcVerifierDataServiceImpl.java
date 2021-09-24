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
import ch.admin.bag.covidcertificate.backend.verifier.data.mapper.ClientCertRowMapper;
import ch.admin.bag.covidcertificate.backend.verifier.data.mapper.CscaRowMapper;
import ch.admin.bag.covidcertificate.backend.verifier.model.CertSource;
import ch.admin.bag.covidcertificate.backend.verifier.model.cert.CertFormat;
import ch.admin.bag.covidcertificate.backend.verifier.model.cert.ClientCert;
import ch.admin.bag.covidcertificate.backend.verifier.model.cert.db.DbCsca;
import ch.admin.bag.covidcertificate.backend.verifier.model.cert.db.DbDsc;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.transaction.annotation.Transactional;

public class JdbcVerifierDataServiceImpl implements VerifierDataService {

    private static final Logger logger = LoggerFactory.getLogger(JdbcVerifierDataServiceImpl.class);

    private final int dscBatchSize;
    private final NamedParameterJdbcTemplate jt;
    private final SimpleJdbcInsert cscaInsert;
    private final SimpleJdbcInsert dscInsert;

    public JdbcVerifierDataServiceImpl(DataSource dataSource, int dscBatchSize) {
        this.dscBatchSize = dscBatchSize;
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
        logger.debug(
                "Inserting CSCA certificates with kid's: {}",
                cscas.stream().map(DbCsca::getKeyId).collect(Collectors.toList()));
        List<SqlParameterSource> batchParams = new ArrayList<>();
        if (!cscas.isEmpty()) {
            for (DbCsca dbCsca : cscas) {
                batchParams.add(getCscaParams(dbCsca));
            }
            cscaInsert.executeBatch(
                    batchParams.toArray(new SqlParameterSource[batchParams.size()]));
        }
    }

    @Override
    @Transactional
    public int removeCscas(List<String> keyIds) {
        if (!keyIds.isEmpty()) {
            var sql =
                    "delete from t_country_specific_certificate_authority"
                            + " where  key_id in (:kids)"
                            + " and source != :manual";
            final var params = new MapSqlParameterSource();
            params.addValue("kids", keyIds);
            params.addValue("manual", CertSource.MANUAL.name());
            return jt.update(sql, params);
        } else {
            return 0;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<DbCsca> findCscas(String origin) {
        final var sql =
                "select * from t_country_specific_certificate_authority where origin = :origin";
        final var params = new MapSqlParameterSource();
        params.addValue("origin", origin);
        return jt.query(sql, params, new CscaRowMapper());
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> findActiveCscaKeyIds() {
        final var sql = "select key_id from t_country_specific_certificate_authority";
        return jt.queryForList(sql, new MapSqlParameterSource(), String.class);
    }

    @Override
    @Transactional
    public void insertDscs(List<DbDsc> dsc) {
        List<SqlParameterSource> batchParams = new ArrayList<>();
        if (!dsc.isEmpty()) {
            for (DbDsc dbDsc : dsc) {
                batchParams.add(getDscParams(dbDsc, CertSource.SYNC));
            }
            dscInsert.executeBatch(batchParams.toArray(new SqlParameterSource[batchParams.size()]));
        }
    }

    @Override
    @Transactional(readOnly = false)
    public void insertManualDsc(DbDsc dsc) {
        dscInsert.execute(getDscParams(dsc, CertSource.MANUAL));
    }

    @Override
    @Transactional
    public int removeDscsNotIn(List<String> keyIdsToKeep) {
        var sql = "delete from t_document_signer_certificate";
        final var params = new MapSqlParameterSource();
        if (!keyIdsToKeep.isEmpty()) {
            sql += " where key_id not in (:kids) and source != :manual";
            params.addValue("kids", keyIdsToKeep);
            params.addValue("manual", CertSource.MANUAL.name());
        }
        return jt.update(sql, params);
    }

    @Override
    @Transactional
    public int removeDscsWithCscaIn(List<String> cscaKidsToRemove) {
        if (!cscaKidsToRemove.isEmpty()) {
            var sql =
                    "delete from t_document_signer_certificate"
                            + " where fk_csca_id in (:fk_csca_id)"
                            + " and source != :manual";
            final var params = new MapSqlParameterSource();
            params.addValue("fk_csca_id", findCscaPksForKids(cscaKidsToRemove));
            params.addValue("manual", CertSource.MANUAL.name());
            return jt.update(sql, params);
        } else {
            return 0;
        }
    }

    private List<Long> findCscaPksForKids(List<String> cscaKids) {
        if (!cscaKids.isEmpty()) {
            final var sql =
                    "select pk_csca_id from t_country_specific_certificate_authority where key_id in (:kids)";
            final var params = new MapSqlParameterSource();
            params.addValue("kids", cscaKids);
            return jt.queryForList(sql, params, Long.class);
        } else {
            return new ArrayList<>();
        }
    }

    @Override
    public List<ClientCert> findDscs(Long since, CertFormat certFormat, Long upTo) {
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
                        + " where pk_dsc_id > :since"
                        + (upTo != null ? " and pk_dsc_id <= :up_to" : "")
                        + " order by pk_dsc_id asc"
                        + " limit :batch_size";

        var params = new MapSqlParameterSource();
        params.addValue("since", since);
        params.addValue("up_to", upTo);
        params.addValue("batch_size", dscBatchSize);

        return jt.query(sql, params, new ClientCertRowMapper(certFormat));
    }

    /** @deprecated only used in KeyController V1 */
    @Override
    @Transactional(readOnly = true)
    @Deprecated(since = "KeyControllerV2", forRemoval = true)
    public List<ClientCert> findDscsBefore(Long since, CertFormat certFormat, Date importedBefore) {
        String sql =
                "select pk_dsc_id,"
                        + " key_id,"
                        + " origin,"
                        + " use,"
                        + " alg,"
                        + " crv,"
                        + " x,"
                        + " y, "
                        + "subject_public_key_info, "
                        + "n, e"
                        + " from t_document_signer_certificate"
                        + " where pk_dsc_id > :pk_dsc_id"
                        + " and imported_at < :before"
                        + " order by pk_dsc_id asc"
                        + " limit :batch_size";

        var params = new MapSqlParameterSource();
        params.addValue("pk_dsc_id", since);
        params.addValue("batch_size", dscBatchSize);
        params.addValue("before", importedBefore);

        return jt.query(sql, params, new ClientCertRowMapper(certFormat));
    }

    @Override
    public List<String> findActiveDscKeyIds() {
        return jt.queryForList(
                "select key_id from t_document_signer_certificate order by pk_dsc_id",
                new MapSqlParameterSource(),
                String.class);
    }

    /** @deprecated only used in KeyController V1 */
    @Override
    @Transactional(readOnly = true)
    @Deprecated(since = "KeyControllerV2", forRemoval = true)
    public List<String> findActiveDscKeyIdsBefore(Date importedBefore) {
        String sql =
                "select key_id from t_document_signer_certificate where imported_at < :before order by pk_dsc_id";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("before", importedBefore);
        return jt.queryForList(sql, params, String.class);
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
    public int getDscBatchSize() {
        return dscBatchSize;
    }

    private MapSqlParameterSource getCscaParams(DbCsca dbCsca) {
        var params = new MapSqlParameterSource();
        params.addValue("key_id", dbCsca.getKeyId());
        params.addValue("certificate_raw", dbCsca.getCertificateRaw());
        params.addValue("origin", dbCsca.getOrigin());
        params.addValue("subject_principal_name", dbCsca.getSubjectPrincipalName());
        params.addValue("source", CertSource.SYNC.name());
        return params;
    }

    private MapSqlParameterSource getDscParams(DbDsc dbDsc, CertSource source) {
        var params = new MapSqlParameterSource();
        params.addValue("key_id", dbDsc.getKeyId());
        params.addValue("fk_csca_id", dbDsc.getFkCsca());
        params.addValue("certificate_raw", dbDsc.getCertificateRaw());
        params.addValue("origin", dbDsc.getOrigin());
        params.addValue("use", dbDsc.getUse());
        params.addValue("alg", dbDsc.getAlg().name());
        params.addValue("n", dbDsc.getN());
        params.addValue("e", dbDsc.getE());
        params.addValue("subject_public_key_info", dbDsc.getSubjectPublicKeyInfo());
        params.addValue("crv", dbDsc.getCrv());
        params.addValue("x", dbDsc.getX());
        params.addValue("y", dbDsc.getY());
        params.addValue("source", source.name());
        return params;
    }

    @Override
    public long findChCscaPkId() {
        String sql =
                "select pk_csca_id from t_country_specific_certificate_authority"
                        + " where source = :source"
                        + " and origin = :origin"
                        + " limit 1";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("source", CertSource.MANUAL.name());
        params.addValue("origin", "CH");
        return jt.queryForObject(sql, params, Long.class);
    }
}
