/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.backend.verifier.ws.model.valuesets;

import java.util.Map;

public class TestValueSets {
    private Map type;
    private Map manf;

    public Map getType() {
        return type;
    }

    public void setType(Map type) {
        this.type = type;
    }

    public Map getManf() {
        return manf;
    }

    public void setManf(Map manf) {
        this.manf = manf;
    }
}
