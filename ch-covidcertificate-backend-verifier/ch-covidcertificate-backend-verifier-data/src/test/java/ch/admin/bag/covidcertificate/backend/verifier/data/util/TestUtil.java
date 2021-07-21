/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.backend.verifier.data.util;

import ch.admin.bag.covidcertificate.backend.verifier.model.cert.Algorithm;
import ch.admin.bag.covidcertificate.backend.verifier.model.cert.db.DbCsca;
import ch.admin.bag.covidcertificate.backend.verifier.model.cert.db.DbDsc;

public class TestUtil {

    private TestUtil() {}

    public static DbCsca getDefaultCsca(int idSuffix, String origin) {
        var dbCsca = new DbCsca();
        dbCsca.setKeyId("keyid_" + idSuffix);
        dbCsca.setCertificateRaw("cert");
        dbCsca.setOrigin(origin);
        dbCsca.setSubjectPrincipalName("admin_ch");
        return dbCsca;
    }

    public static DbDsc getRsaDsc(int idSuffix, String origin, long fkCsca) {
        final var dbDsc = new DbDsc();
        dbDsc.setKeyId("keyid_" + idSuffix);
        dbDsc.setFkCsca(fkCsca);
        dbDsc.setCertificateRaw("cert");
        dbDsc.setOrigin(origin);
        dbDsc.setUse("sig");
        dbDsc.setAlg(Algorithm.RS256);
        dbDsc.setN("n");
        dbDsc.setE("e");
        dbDsc.setSubjectPublicKeyInfo("pk");
        return dbDsc;
    }

    public static DbDsc getEcDsc(int idSuffix, String origin, long fkCsca) {
        final var dbDsc = new DbDsc();
        dbDsc.setKeyId("keyid_" + idSuffix);
        dbDsc.setFkCsca(fkCsca);
        dbDsc.setCertificateRaw("cert");
        dbDsc.setOrigin(origin);
        dbDsc.setUse("sig");
        dbDsc.setAlg(Algorithm.ES256);
        dbDsc.setCrv("crv");
        dbDsc.setX("x");
        dbDsc.setY("y");
        return dbDsc;
    }
}
