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

import ch.admin.bag.covidcertificate.backend.verifier.model.cert.Algorithm;
import ch.admin.bag.covidcertificate.backend.verifier.model.cert.db.DbDsc;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.jdbc.core.RowMapper;

public class DscRowMapper implements RowMapper<DbDsc> {

    @Override
    public DbDsc mapRow(ResultSet rs, int rowNum) throws SQLException {
        var dbDsc = new DbDsc();
        dbDsc.setId(rs.getLong("pk_dsc_id"));
        dbDsc.setKeyId(rs.getString("key_id"));
        dbDsc.setFkCsca(rs.getLong("fk_csca_id"));
        dbDsc.setCertificateRaw(rs.getString("certificate_raw"));
        dbDsc.setImportedAt(rs.getTimestamp("imported_at").toInstant());
        dbDsc.setOrigin(rs.getString("origin"));
        dbDsc.setUse(rs.getString("use"));
        dbDsc.setAlg(Algorithm.valueOf(rs.getString("alg")));
        dbDsc.setN(rs.getString("n"));
        dbDsc.setE(rs.getString("e"));
        dbDsc.setSubjectPublicKeyInfo(rs.getString("subject_public_key_info"));
        dbDsc.setCrv(rs.getString("crv"));
        dbDsc.setX(rs.getString("x"));
        dbDsc.setY(rs.getString("y"));
        return dbDsc;
    }
}