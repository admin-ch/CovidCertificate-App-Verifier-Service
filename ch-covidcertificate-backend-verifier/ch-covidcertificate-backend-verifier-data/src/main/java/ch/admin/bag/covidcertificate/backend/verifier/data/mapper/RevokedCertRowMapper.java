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

import ch.admin.bag.covidcertificate.backend.verifier.model.DbRevokedCert;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.jdbc.core.RowMapper;

public class RevokedCertRowMapper implements RowMapper<DbRevokedCert> {

    @Override
    public DbRevokedCert mapRow(ResultSet resultSet, int i) throws SQLException {
        var revokedCert = new DbRevokedCert();
        revokedCert.setPkId(resultSet.getLong("pk_revoked_cert_id"));
        revokedCert.setUvci(resultSet.getString("uvci"));
        return revokedCert;
    }
}
