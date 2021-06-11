/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.backend.verifier.ws.utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Objects;
import org.apache.commons.codec.binary.Hex;
import org.springframework.core.io.ClassPathResource;

public class EtagUtil {

    private EtagUtil() {}

    /**
     * generates a hashcode for a list that does not depend on element order
     *
     * @param list
     * @return
     */
    public static int getUnsortedListHashcode(List<String> list) {
        return list != null ? list.stream().map(Objects::hash).reduce(0, (a, b) -> a ^ b) : 0;
    }

    public static String getSha1HashForFile(String pathToFile)
            throws IOException, NoSuchAlgorithmException {
        String classpathPrefix = "classpath:";
        try (InputStream is =
                pathToFile.startsWith(classpathPrefix)
                        ? new ClassPathResource(pathToFile.replace(classpathPrefix, ""))
                                .getInputStream()
                        : new FileInputStream(pathToFile)) {

            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            byte[] buffer = new byte[8192];
            int len = is.read(buffer);

            while (len != -1) {
                sha1.update(buffer, 0, len);
                len = is.read(buffer);
            }

            return Hex.encodeHexString(sha1.digest());
        }
    }
}
