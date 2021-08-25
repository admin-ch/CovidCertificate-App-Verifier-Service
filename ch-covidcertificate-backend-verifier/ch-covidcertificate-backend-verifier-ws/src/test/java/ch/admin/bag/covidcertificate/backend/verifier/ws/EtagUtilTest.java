/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.backend.verifier.ws;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import ch.admin.bag.covidcertificate.backend.verifier.ws.controller.ValueSetsController;
import ch.admin.bag.covidcertificate.backend.verifier.ws.utils.EtagUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

public class EtagUtilTest {
    @Test
    public void testUnsortedListEtag() {
        String expected = "W/\"100548\"";
        List<String> list = new ArrayList<>(List.of("a", "bc", "def"));
        String actual = EtagUtil.getUnsortedListEtag(list);
        assertEquals(expected, actual);
        Collections.reverse(list);
        assertEquals(expected, EtagUtil.getUnsortedListEtag(list));
        list.remove(0);
        list.add("g");
        assertNotEquals(actual, EtagUtil.getUnsortedListEtag(list));
    }

    private static final String PATH_TO_VERIFICATION_RULES = "classpath:verificationRules.json";
    private static final String PATH_TO_TEST_VERIFICATION_RULES =
            "classpath:testVerificationRules.json";

    @Test
    public void testFileHash() throws Exception {
        String expected = "W/\"e7934ba39046b48c7ee97e069eefa482b71ea30e\"";
        String sha1 = EtagUtil.getSha1HashForFiles(PATH_TO_VERIFICATION_RULES);
        assertEquals(expected, sha1);
        assertNotEquals(expected, EtagUtil.getSha1HashForFiles(PATH_TO_TEST_VERIFICATION_RULES));
    }

    @Test
    public void testFileHashMultiple() throws Exception {
        String expected = "W/\"19d26e8cfc5a14fbdbaf107c933811e1988f1443\"";
        List<String> pathsToValueSets =
                ValueSetsController.PATHS_TO_VALUE_SETS.stream()
                        .map(p -> "classpath:" + p)
                        .collect(Collectors.toList());
        String sha1 =
                EtagUtil.getSha1HashForFiles(
                        pathsToValueSets.toArray(new String[pathsToValueSets.size()]));
        assertEquals(expected, sha1);
    }
}
