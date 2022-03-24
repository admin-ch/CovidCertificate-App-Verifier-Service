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

import static org.junit.jupiter.api.Assertions.assertEquals;

import ch.admin.bag.covidcertificate.backend.verifier.model.ForeignRule;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ForeignRulesDataServiceTest extends BaseDataServiceTest {

    @Autowired ForeignRulesDataService dataService;
    @Autowired DataSource dataSource;

    ForeignRule atRule;
    ForeignRule deRule1;
    ForeignRule deRule2;

    @BeforeAll
    void setup(){
        atRule = new ForeignRule();
        atRule.setId("GR-AT-0039");
        atRule.setVersion("0.0.1");
        atRule.setValidFrom(LocalDateTime.now().minus(1, ChronoUnit.DAYS));
        atRule.setValidUntil(LocalDateTime.now().plus(1, ChronoUnit.YEARS));
        atRule.setContent("{}");
        atRule.setCountry("AT");
        deRule1 = new ForeignRule();
        deRule1.setId("GR-DE-0039");
        deRule1.setVersion("0.0.1");
        deRule1.setValidFrom(LocalDateTime.now().minus(1, ChronoUnit.DAYS));
        deRule1.setValidUntil(LocalDateTime.now().plus(1, ChronoUnit.YEARS));
        deRule1.setContent("{}");
        deRule1.setCountry("DE");
        deRule2 = new ForeignRule();
        deRule2.setId("GR-DE-0039");
        deRule2.setVersion("0.0.2");
        deRule2.setValidFrom(LocalDateTime.now().minus(1, ChronoUnit.HOURS));
        deRule2.setValidUntil(LocalDateTime.now().plus(1, ChronoUnit.YEARS));
        deRule2.setContent("{}");
        deRule2.setCountry("DE");
        dataService.insertRule(atRule);
        dataService.insertRule(deRule1);
        dataService.insertRule(deRule2);
    }

    @Test
    void countryListTest() {
        var countries = dataService.getCountries();
        assertEquals(2, countries.size());
    }

    @Test
    void rulesForCountryTest(){
        var de = dataService.getRulesForCountry("DE");
        assertEquals(2, de.size());
        var fr = dataService.getRulesForCountry("FR");
        assertEquals(0, fr.size());
    }

    @Test
    void deletionTest(){
        var de = dataService.getRulesForCountry("DE");
        assertEquals(2, de.size());
        dataService.removeRuleSet("DE");
        de = dataService.getRulesForCountry("DE");
        assertEquals(0, de.size());
        var countries = dataService.getCountries();
        assertEquals(1, countries.size());
        var at = dataService.getRulesForCountry("AT");
        assertEquals(1, at.size());
        dataService.insertRule(deRule1);
        dataService.insertRule(deRule2);
    }
}
