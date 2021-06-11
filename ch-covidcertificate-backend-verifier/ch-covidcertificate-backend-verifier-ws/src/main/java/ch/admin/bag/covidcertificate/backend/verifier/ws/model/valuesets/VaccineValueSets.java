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

public class VaccineValueSets {
    private Map mahManf;
    private Map medicinalProduct;
    private Map prophylaxis;

    public Map getMahManf() {
        return mahManf;
    }

    public void setMahManf(Map mahManf) {
        this.mahManf = mahManf;
    }

    public Map getMedicinalProduct() {
        return medicinalProduct;
    }

    public void setMedicinalProduct(Map medicinalProduct) {
        this.medicinalProduct = medicinalProduct;
    }

    public Map getProphylaxis() {
        return prophylaxis;
    }

    public void setProphylaxis(Map prophylaxis) {
        this.prophylaxis = prophylaxis;
    }
}
