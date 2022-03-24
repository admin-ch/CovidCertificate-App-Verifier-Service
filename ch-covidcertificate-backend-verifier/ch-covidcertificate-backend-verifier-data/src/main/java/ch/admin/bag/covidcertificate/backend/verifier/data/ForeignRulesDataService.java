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

import ch.admin.bag.covidcertificate.backend.verifier.model.ForeignRule;
import java.util.List;

/**
 * Dataservice to store and retrieve validation rules obtained from the DGC gateway
 */
public interface ForeignRulesDataService {

    /**
     *
     * @return A list of all countries that we have rules for
     */
    public List<String> getCountries();

    /**
     *
     * @param country the two-letter country code
     * @return All available versions of all rules for that country
     */
    public List<ForeignRule> getRulesForCountry(String country);

    /**
     * Insert a rule into the DB
     * @param rule the rule
     */
    public void insertRule(ForeignRule rule);

    /**
     * Remove all rules for a given country (in order to insert a new set)
     * @param country the country
     */
    public void removeRuleSet(String country);
}
