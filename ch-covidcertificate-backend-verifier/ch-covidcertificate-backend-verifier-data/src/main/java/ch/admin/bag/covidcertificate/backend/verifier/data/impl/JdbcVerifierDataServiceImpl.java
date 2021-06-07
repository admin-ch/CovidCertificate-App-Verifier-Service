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
import ch.admin.bag.covidcertificate.backend.verifier.model.cert.CertFormat;
import ch.admin.bag.covidcertificate.backend.verifier.model.cert.ClientCert;
import ch.admin.bag.covidcertificate.backend.verifier.model.cert.db.DbCsca;
import ch.admin.bag.covidcertificate.backend.verifier.model.cert.db.DbDsc;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;

public class JdbcVerifierDataServiceImpl implements VerifierDataService {

    private final NamedParameterJdbcTemplate jt;
    private final SimpleJdbcInsert cscaInsert;
    private final SimpleJdbcInsert dscInsert;

    private static final int MAX_DSC_BATCH_COUNT = 1000;

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
    public void insertCscas(List<DbCsca> cscas) {
        // TODO
    }

    @Override
    public int removeCscasNotIn(List<String> keyIdsToKeep) {
        // TODO
        return 0;
    }

    @Override
    public void insertDsc(DbDsc dsc) {
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
}
