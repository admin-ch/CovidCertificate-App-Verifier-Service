/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.backend.verifier.sync.syncer.model;

import java.util.List;

public class RulesSyncResult {
    private final List<String> successsfullRules;
    private final List<String> failedRules;

    public RulesSyncResult(List<String> successfullRules, List<String> failedRules) {
        this.successsfullRules = successfullRules;
        this.failedRules = failedRules;
    }

    /**
     * @return the failedRules
     */
    public List<String> getFailedRules() {
        return failedRules;
    }

    /**
     * @return the successsfullRules
     */
    public List<String> getSuccesssfullRules() {
        return successsfullRules;
    }
}
