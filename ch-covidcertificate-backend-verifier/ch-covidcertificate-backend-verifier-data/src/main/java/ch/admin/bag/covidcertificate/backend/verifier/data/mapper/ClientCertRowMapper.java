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
import ch.admin.bag.covidcertificate.backend.verifier.model.cert.CertFormat;
import ch.admin.bag.covidcertificate.backend.verifier.model.cert.ClientCert;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.jdbc.core.RowMapper;

public class ClientCertRowMapper implements RowMapper<ClientCert> {

    private CertFormat certFormat;

    public ClientCertRowMapper(CertFormat certFormat) {
        this.certFormat = certFormat;
    }

    @Override
    public ClientCert mapRow(ResultSet rs, int rowNum) throws SQLException {
        var clientCert = new ClientCert();
        clientCert.setPkId(rs.getLong("pk_dsc_id"));
        clientCert.setKeyId(rs.getString("key_id"));
        clientCert.setUse(rs.getString("use"));
        Algorithm alg = Algorithm.valueOf(rs.getString("alg"));
        clientCert.setAlg(alg);
        switch (alg) {
            case ES256:
                clientCert.setCrv(rs.getString("crv"));
                clientCert.setX(rs.getString("x"));
                clientCert.setY(rs.getString("y"));
                break;
            case RS256:
                switch (certFormat) {
                    case IOS:
                        clientCert.setSubjectPublicKeyInfo(rs.getString("subject_public_key_info"));
                        break;
                    case ANDROID:
                        clientCert.setN(rs.getString("n"));
                        clientCert.setE(rs.getString("e"));
                        break;
                }
                break;
        }
        return clientCert;
    }
}
