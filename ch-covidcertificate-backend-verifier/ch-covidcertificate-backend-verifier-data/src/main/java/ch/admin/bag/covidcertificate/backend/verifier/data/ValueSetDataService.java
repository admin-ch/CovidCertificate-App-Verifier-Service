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

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;
import java.util.Map;

public interface ValueSetDataService {

    /**
     * inserts the given value sets into the db
     *
     * @param valueSetsById
     */
    void insertValueSets(Map<String, String> valueSetsById) throws JsonProcessingException;

    /**
     * returns the latest value set for the given id
     *
     * @param valueSetId
     */
    String findLatestValueSet(String valueSetId);

    /**
     * returns all known value-set ids
     *
     * @return valueSetIds
     */
    List<String> findAllValueSetIds();

    void deleteOldValueSets();
}
