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
    private TestValueSets testValueSets;
    private VaccineValueSets vaccineValueSets;

    public ValueSets(TestValueSets testValueSets, VaccineValueSets vaccineValueSets) {
        this.testValueSets = testValueSets;
        this.vaccineValueSets = vaccineValueSets;
    }

    public TestValueSets getTestValueSet() {
        return testValueSets;
    }

    public VaccineValueSets getVaccineValueSet() {
        return vaccineValueSets;
    }
}
