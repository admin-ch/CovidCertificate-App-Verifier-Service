/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.backend.verifier.sync.ws.model;

import java.util.ArrayList;
import java.util.List;

public class DscUploadResponse {
    private List<DscUploadResult> results = new ArrayList<>();

    public List<DscUploadResult> getResults() {
        return results;
    }

    public void addResult(DscUploadResult result) {
        this.results.add(result);
    }
}
