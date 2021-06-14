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

public class ValueSets {
    private TestValueSets test;
    private VaccineValueSets vaccine;

    public ValueSets() {}

    public ValueSets(TestValueSets test, VaccineValueSets vaccine) {
        this.test = test;
        this.vaccine = vaccine;
    }

    public TestValueSets getTest() {
        return test;
    }

    public void setTest(TestValueSets test) {
        this.test = test;
    }

    public VaccineValueSets getVaccine() {
        return vaccine;
    }

    public void setVaccine(VaccineValueSets vaccine) {
        this.vaccine = vaccine;
    }
}
