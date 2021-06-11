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

import ch.admin.bag.covidcertificate.backend.verifier.ws.utils.EtagUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
        String expected = "f4915f1428328eb6c239693fc8e6541f68a3f72e";
        String sha1 = EtagUtil.getSha1HashForFile(PATH_TO_VERIFICATION_RULES);
        assertEquals(expected, sha1);
        assertNotEquals(expected, EtagUtil.getSha1HashForFile(PATH_TO_TEST_VERIFICATION_RULES));
    }
}
