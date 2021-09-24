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

import ch.admin.bag.covidcertificate.backend.verifier.data.CertUploadDataService;
import ch.admin.bag.covidcertificate.backend.verifier.data.mapper.CertToUploadRowMapper;
import ch.admin.bag.covidcertificate.backend.verifier.data.util.DateUtil;
import ch.admin.bag.covidcertificate.backend.verifier.model.cert.CertToUpload;
import java.util.List;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

public class JdbcCertUploadDataServiceImpl implements CertUploadDataService {

    private static final Logger logger =
            LoggerFactory.getLogger(JdbcCertUploadDataServiceImpl.class);

    private final NamedParameterJdbcTemplate jt;

    public JdbcCertUploadDataServiceImpl(DataSource dataSource) {
        this.jt = new NamedParameterJdbcTemplate(dataSource);
    }

    @Override
    public List<CertToUpload> findCertsToUpload() {
        return jt.query(
                "select * from t_cert_to_upload"
                        + " where inserted_at is null"
                        + " or uploaded_at is null",
                new MapSqlParameterSource(),
                new CertToUploadRowMapper());
    }

    @Override
    public void updateCertToUpload(CertToUpload certToUpload) {
        String sql =
                "update t_cert_to_upload"
                        + " set inserted_at = :inserted_at,"
                        + " uploaded_at = :uploaded_at"
                        + " where pk_alias = :pk_alias";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("pk_alias", certToUpload.getAlias());
        params.addValue("inserted_at", DateUtil.instantToDate(certToUpload.getInsertedAt()));
        params.addValue("uploaded_at", DateUtil.instantToDate(certToUpload.getUploadedAt()));
        jt.update(sql, params);
    }
}
