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

import ch.admin.bag.covidcertificate.backend.verifier.model.cert.CertToUpload;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import org.springframework.jdbc.core.RowMapper;

public class CertToUploadRowMapper implements RowMapper<CertToUpload> {

    @Override
    public CertToUpload mapRow(ResultSet rs, int i) throws SQLException {
        CertToUpload certToUpload = new CertToUpload();
        certToUpload.setAlias(rs.getString("pk_alias"));
        certToUpload.setSlot(rs.getInt("pk_slot"));
        certToUpload.setKeyId(rs.getString("key_id"));

        Timestamp uploadedAt = rs.getTimestamp("uploaded_at");
        if (uploadedAt != null) {
            certToUpload.setUploadedAt(uploadedAt.toInstant());
        }

        certToUpload.setDoUpload(rs.getBoolean("do_upload"));

        Timestamp insertedAt = rs.getTimestamp("inserted_at");
        if (insertedAt != null) {
            certToUpload.setInsertedAt(insertedAt.toInstant());
        }

        certToUpload.setDoInsert(rs.getBoolean("do_insert"));

        return certToUpload;
    }
}
