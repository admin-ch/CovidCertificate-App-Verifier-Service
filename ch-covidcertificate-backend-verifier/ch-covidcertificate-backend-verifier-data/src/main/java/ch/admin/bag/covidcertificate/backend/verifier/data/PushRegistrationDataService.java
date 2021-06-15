/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.backend.verifier.data;

import ch.admin.bag.covidcertificate.backend.verifier.model.push_registration.PushRegistration;
import ch.admin.bag.covidcertificate.backend.verifier.model.push_registration.PushType;
import java.util.List;

public interface PushRegistrationDataService {

    /**
     * Inserts the given pushRegistration into the db
     *
     * @param pushRegistration
     */
    void upsertPushRegistration(final PushRegistration pushRegistration);

    /**
     * retrieves all pushRegistrations for a given pushType
     *
     * @param pushType
     */
    List<PushRegistration> getPushRegistrationByType(final PushType pushType);

    /**
     * remove entries for given list of tokens
     *
     * @param tokensToRemove
     */
    void removeRegistrations(List<String> tokensToRemove);
}
