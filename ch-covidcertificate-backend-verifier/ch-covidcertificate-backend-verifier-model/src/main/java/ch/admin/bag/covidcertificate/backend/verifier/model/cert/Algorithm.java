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

public enum Algorithm {
    ES256,
    RS256,
    UNSUPPORTED;

    public static Algorithm forPubKeyType(String keyType) {
        if ("RSA".equals(keyType)) {
            return RS256;
        } else if ("EC".equals(keyType)) {
            return ES256;
        } else {
            return UNSUPPORTED;
        }
    }
}
