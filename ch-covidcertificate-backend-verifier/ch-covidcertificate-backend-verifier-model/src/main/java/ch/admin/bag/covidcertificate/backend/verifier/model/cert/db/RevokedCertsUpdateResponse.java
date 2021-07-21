/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.backend.verifier.model.cert.db;

public class RevokedCertsUpdateResponse {
    private final int insertCount;
    private final int removeCount;

    public RevokedCertsUpdateResponse(int insertCount, int removeCount) {
        this.insertCount = insertCount;
        this.removeCount = removeCount;
    }

    public int getInsertCount() {
        return insertCount;
    }

    public int getRemoveCount() {
        return removeCount;
    }
}
