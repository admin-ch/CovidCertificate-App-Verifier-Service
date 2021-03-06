/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.backend.verifier.data.mapper;

import ch.admin.bag.covidcertificate.backend.verifier.model.cert.db.DbCsca;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.jdbc.core.RowMapper;

public class CscaRowMapper implements RowMapper<DbCsca> {

    @Override
    public DbCsca mapRow(ResultSet rs, int rowNum) throws SQLException {
        var dbCsca = new DbCsca();
        dbCsca.setId(rs.getLong("pk_csca_id"));
        dbCsca.setKeyId(rs.getString("key_id"));
        dbCsca.setCertificateRaw(rs.getString("certificate_raw"));
        dbCsca.setImportedAt(rs.getTimestamp("imported_at").toInstant());
        dbCsca.setOrigin(rs.getString("origin"));
        dbCsca.setSubjectPrincipalName(rs.getString("subject_principal_name"));
        return dbCsca;
    }
}
