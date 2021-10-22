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
import ch.admin.bag.covidcertificate.backend.verifier.data.mapper.DscRowMapper;
import ch.admin.bag.covidcertificate.backend.verifier.model.CertSource;
import ch.admin.bag.covidcertificate.backend.verifier.model.cert.CertFormat;
import ch.admin.bag.covidcertificate.backend.verifier.model.cert.ClientCert;
import ch.admin.bag.covidcertificate.backend.verifier.model.cert.db.DbCsca;
import ch.admin.bag.covidcertificate.backend.verifier.model.cert.db.DbDsc;
import ch.admin.bag.covidcertificate.backend.verifier.model.exception.DgcSyncException;
import ch.admin.bag.covidcertificate.backend.verifier.model.sync.DscRestoreResponse;
import java.time.Duration;
import java.time.Instant;
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
    private final Duration keepDscsMarkedForDeletionDuration;

    public JdbcVerifierDataServiceImpl(
            DataSource dataSource, int dscBatchSize, Duration keepDscsMarkedForDeletionDuration) {
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
        this.keepDscsMarkedForDeletionDuration = keepDscsMarkedForDeletionDuration;
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
    public int removeCscasNotIn(List<String> keyIdsToKeep) throws DgcSyncException {
        if (!keyIdsToKeep.isEmpty()) {
            var sql =
                    "update t_country_specific_certificate_authority"
                            + " set deleted_at = :now"
                            + " where key_id not in (:kids) and source != :manual and deleted_at is null";
            final var params = new MapSqlParameterSource();
            params.addValue("kids", keyIdsToKeep);
            params.addValue("manual", CertSource.MANUAL.name());
            params.addValue("now", Date.from(Instant.now()));
            return jt.update(sql, params);
        } else {
            throw new DgcSyncException(new Exception("empty CSCA list to keep"));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<DbCsca> findCscas(String origin) {
        final var sql =
                "select * from t_country_specific_certificate_authority"
                        + " where origin = :origin and deleted_at is null";
        final var params = new MapSqlParameterSource();
        params.addValue("origin", origin);
        return jt.query(sql, params, new CscaRowMapper());
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> findActiveCscaKeyIds() {
        final var sql =
                "select key_id from t_country_specific_certificate_authority"
                        + " where deleted_at is null";
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
    public int removeDscsNotIn(List<String> keyIdsToKeep) throws DgcSyncException {
        if (!keyIdsToKeep.isEmpty()) {
            var sql =
                    "update t_document_signer_certificate set deleted_at = :now"
                            + " where key_id not in (:kids)"
                            + " and source != :manual"
                            + " and deleted_at is null";
            final var params = new MapSqlParameterSource();
            params.addValue("manual", CertSource.MANUAL.name());
            params.addValue("now", Date.from(Instant.now()));
            params.addValue("kids", keyIdsToKeep);
            return jt.update(sql, params);
        } else {
            throw new DgcSyncException(new Exception("empty DSC list to keep"));
        }
    }

    @Override
    @Transactional
    public int removeDscsWithCscaNotIn(List<String> cscaKidsToKeep) throws DgcSyncException {
        if (!cscaKidsToKeep.isEmpty()) {
            var fkSubQuery =
                    "select pk_csca_id from t_country_specific_certificate_authority"
                            + " where key_id in (:csca_key_ids) and deleted_at is null";
            var sql =
                    "update t_document_signer_certificate"
                            + " set deleted_at = :now"
                            + " where fk_csca_id not in ("
                            + fkSubQuery
                            + ")"
                            + " and source != :manual"
                            + " and deleted_at is null";
            final var params = new MapSqlParameterSource();
            params.addValue("csca_key_ids", cscaKidsToKeep);
            params.addValue("manual", CertSource.MANUAL.name());
            params.addValue("now", Date.from(Instant.now()));
            return jt.update(sql, params);
        } else {
            throw new DgcSyncException(new Exception("empty CSCA list to keep"));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<DbDsc> findDscsMarkedForDeletion() {
        String sql =
                "select distinct on (key_id) * from t_document_signer_certificate"
                        + " where deleted_at is not null"
                        + " and key_id not in"
                        + " (select key_id from t_document_signer_certificate where deleted_at is null)";
        return jt.query(sql, new MapSqlParameterSource(), new DscRowMapper());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DbCsca> findCscasMarkedForDeletion() {
        String sql =
                "select distinct on (key_id) * from t_country_specific_certificate_authority"
                        + " where deleted_at is not null"
                        + " and key_id not in"
                        + " (select key_id from t_country_specific_certificate_authority where deleted_at is null)";
        return jt.query(sql, new MapSqlParameterSource(), new CscaRowMapper());
    }

    @Override
    @Transactional(readOnly = false)
    public DscRestoreResponse restoreDeletedDscs() {
        List<DbDsc> dscsToResurrect = findDscsMarkedForDeletion();
        List<DbCsca> cscasToResurrect = findCscasMarkedForDeletion();
        cscasToResurrect =
                cscasToResurrect.stream()
                        .filter(
                                c ->
                                        dscsToResurrect.stream()
                                                .anyMatch(d -> d.getFkCsca().equals(c.getId())))
                        .collect(Collectors.toList());
        insertDscs(dscsToResurrect);
        insertCscas(cscasToResurrect);
        return new DscRestoreResponse(cscasToResurrect.size(), dscsToResurrect.size());
    }

    @Override
    @Transactional(readOnly = true)
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
                        + " and deleted_at is null"
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
                        + " and deleted_at is null"
                        + " order by pk_dsc_id asc"
                        + " limit :batch_size";

        var params = new MapSqlParameterSource();
        params.addValue("pk_dsc_id", since);
        params.addValue("batch_size", dscBatchSize);
        params.addValue("before", importedBefore);

        return jt.query(sql, params, new ClientCertRowMapper(certFormat));
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> findActiveDscKeyIds() {
        return jt.queryForList(
                "select key_id from t_document_signer_certificate"
                        + " where deleted_at is null"
                        + " order by pk_dsc_id",
                new MapSqlParameterSource(),
                String.class);
    }

    /** @deprecated only used in KeyController V1 */
    @Override
    @Transactional(readOnly = true)
    @Deprecated(since = "KeyControllerV2", forRemoval = true)
    public List<String> findActiveDscKeyIdsBefore(Date importedBefore) {
        String sql =
                "select key_id from t_document_signer_certificate"
                        + " where imported_at < :before and deleted_at is null"
                        + " order by pk_dsc_id";
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
                            + " where deleted_at is null"
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
        params.addValue("deleted_at", null);
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
        params.addValue("deleted_at", null);
        return params;
    }

    @Override
    @Transactional(readOnly = true)
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

    @Override
    @Transactional(readOnly = false)
    public int cleanUpDscsMarkedForDeletion() {
        String sql =
                "delete from t_document_signer_certificate"
                        + " where deleted_at < :before and source != :manual";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue(
                "before", Date.from(Instant.now().minus(keepDscsMarkedForDeletionDuration)));
        params.addValue("manual", CertSource.MANUAL.name());
        return jt.update(sql, params);
    }

    @Override
    public Duration getKeepDscsMarkedForDeletionDuration() {
        return keepDscsMarkedForDeletionDuration;
    }
}
