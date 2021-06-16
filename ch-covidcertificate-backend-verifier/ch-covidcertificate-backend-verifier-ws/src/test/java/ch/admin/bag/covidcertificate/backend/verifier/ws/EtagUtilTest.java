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
    public void testUnsortedListHashcode() {
        int expected = 100548;
        List<String> list = new ArrayList<>(List.of("a", "bc", "def"));
        int hashcode = EtagUtil.getUnsortedListHashcode(list);
        assertEquals(expected, hashcode);
        Collections.reverse(list);
        assertEquals(expected, EtagUtil.getUnsortedListHashcode(list));
        list.remove(0);
        list.add("g");
        assertNotEquals(hashcode, EtagUtil.getUnsortedListHashcode(list));
    }

    private static final String PATH_TO_VERIFICATION_RULES = "classpath:verificationRules.json";
    private static final String PATH_TO_TEST_VERIFICATION_RULES =
            "classpath:testVerificationRules.json";

    @Test
    public void testFileHash() throws Exception {
        String expected = "d12ead8a2248f5c7251583564d6e51b9a02a9389";
        String sha1 = EtagUtil.getSha1HashForFiles(PATH_TO_VERIFICATION_RULES);
        assertEquals(expected, sha1);
        assertNotEquals(expected, EtagUtil.getSha1HashForFiles(PATH_TO_TEST_VERIFICATION_RULES));
    }

    @Test
    public void testFileHashMultiple() throws Exception {
        String expected = "e8832aef5f37843e52a5acb78c522fa36576a62e";
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
