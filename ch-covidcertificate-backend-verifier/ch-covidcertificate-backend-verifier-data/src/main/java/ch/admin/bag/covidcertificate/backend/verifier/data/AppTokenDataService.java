/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.backend.verifier.data;

import ch.admin.bag.covidcertificate.backend.verifier.model.AppToken;
import java.util.List;

/**
 * Dataservice allowing the fetching of stored app tokens. Insertion and removal are done by hand.
 */
public interface AppTokenDataService {

    /** Fetches all app tokens contained in the database */
    public List<AppToken> getAppTokens();
}
