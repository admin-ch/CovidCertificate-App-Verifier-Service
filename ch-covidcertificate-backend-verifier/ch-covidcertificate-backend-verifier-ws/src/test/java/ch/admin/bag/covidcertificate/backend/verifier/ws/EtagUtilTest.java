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

    private static final String[] PATHS_TO_VALUE_SETS =
            new String[] {
                "classpath:valuesets/test-manf.json",
                "classpath:valuesets/test-type.json",
                "classpath:valuesets/vaccine-mah-manf.json",
                "classpath:valuesets/vaccine-medicinal-product.json",
                "classpath:valuesets/vaccine-prophylaxis.json"
            };

    @Test
    public void testFileHash() throws Exception {
        String expected = "011ec25ca7a4d0c95fe8fd7c33cdeff3654d7bf9";
        String sha1 = EtagUtil.getSha1HashForFiles(PATH_TO_VERIFICATION_RULES);
        assertEquals(expected, sha1);
        assertNotEquals(expected, EtagUtil.getSha1HashForFiles(PATH_TO_TEST_VERIFICATION_RULES));
    }

    @Test
    public void testFileHashMultiple() throws Exception {
        String expected = "e8832aef5f37843e52a5acb78c522fa36576a62e";
        String sha1 = EtagUtil.getSha1HashForFiles(PATHS_TO_VALUE_SETS);
        assertEquals(expected, sha1);
    }
}
