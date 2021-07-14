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

import java.util.ArrayList;
import java.util.List;

public class CertsResponse {
    private List<ClientCert> certs = new ArrayList<>();

    public CertsResponse(List<ClientCert> certs) {
        if (certs == null) {
            certs = new ArrayList<>();
        }
        this.certs = certs;
    }

    public List<ClientCert> getCerts() {
        return certs;
    }

    public void setCerts(List<ClientCert> certs) {
        this.certs = certs;
    }
}
