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

import ch.ubique.openapi.docannotations.Documentation;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class ActiveCertsResponse {
    @Documentation(description = "list of active key ids")
    private List<String> activeKeyIds = new ArrayList<>();

    @Documentation(
            description = "describes how long the list response is valid for in ms",
            example = "172800000")
    private Duration validDuration = Duration.ofHours(48);

    @Documentation(description = "up to which key id can be requested")
    private Long upTo;

    public ActiveCertsResponse() {}

    public ActiveCertsResponse(List<String> activeKeyIds, Long upTo) {
        if (activeKeyIds == null) {
            activeKeyIds = new ArrayList<>();
        }
        this.activeKeyIds = activeKeyIds;
        this.upTo = upTo;
    }

    public List<String> getActiveKeyIds() {
        return activeKeyIds;
    }

    public void setActiveKeyIds(List<String> activeKeyIds) {
        this.activeKeyIds = activeKeyIds;
    }

    public Long getValidDuration() {
        return validDuration.toMillis();
    }

    public void setValidDuration(Long durationInMs) {
        this.validDuration = Duration.ofMillis(durationInMs);
    }

    public Long getUpTo() {
        return upTo;
    }

    public void setUpTo(Long upTo) {
        this.upTo = upTo;
    }
}
