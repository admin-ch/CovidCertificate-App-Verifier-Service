/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.backend.verifier.model.cert;

public enum ExtendedKeyUsage {
    TEST("t", "1.3.6.1.4.1.0.1847.2021.1.1"),
    VACCINATIONS("v", "1.3.6.1.4.1.0.1847.2021.1.2"),
    RECOVERY("r", "1.3.6.1.4.1.0.1847.2021.1.3");

    private String code;
    private String oid;

    ExtendedKeyUsage(String code, String oid) {
        this.code = code;
        this.oid = oid;
    }

    public String getCode() {
        return code;
    }

    public String getOid() {
        return oid;
    }

    public static ExtendedKeyUsage forOid(String oid) {
        for (ExtendedKeyUsage extendedKeyUsage : ExtendedKeyUsage.values()) {
            if (extendedKeyUsage.getOid().equals(oid)) {
                return extendedKeyUsage;
            }
        }
        return null;
    }
}
