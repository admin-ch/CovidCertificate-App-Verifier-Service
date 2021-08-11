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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Objects;
import org.apache.commons.codec.binary.Hex;
import org.springframework.core.io.ClassPathResource;

public class EtagUtil {

    private EtagUtil() {}

    private static final String WEAK_PREFIX = "W/";
    private static final String SHA_1 = "SHA-1";

    /**
     * generates a weak etag for a list that does not depend on element order
     *
     * @param list
     * @return
     */
    public static String getUnsortedListEtag(List<String> list) {
        int hash = list != null ? list.stream().map(Objects::hash).reduce(0, (a, b) -> a ^ b) : 0;
        return WEAK_PREFIX + "\"" + hash + "\"";
    }

    public static String getSha1HashForFiles(String... pathToFiles)
            throws IOException, NoSuchAlgorithmException {
        MessageDigest sha1 = MessageDigest.getInstance(SHA_1);
        for (String pathToFile : pathToFiles) {
            final String classpathPrefix = "classpath:";
            try (InputStream is =
                    pathToFile.startsWith(classpathPrefix)
                            ? new ClassPathResource(pathToFile.replace(classpathPrefix, ""))
                                    .getInputStream()
                            : new FileInputStream(pathToFile)) {

                byte[] buffer = new byte[8192];
                int len = is.read(buffer);
                while (len != -1) {
                    sha1.update(buffer, 0, len);
                    len = is.read(buffer);
                }
            }
        }
        return WEAK_PREFIX + "\"" + Hex.encodeHexString(sha1.digest()) + "\"";
    }

    public static String getSha1HashForStrings(String... strings) throws NoSuchAlgorithmException {
        MessageDigest sha1 = MessageDigest.getInstance(SHA_1);
        for (String string : strings) {
            byte[] bytes = string.getBytes(StandardCharsets.UTF_8);
            sha1.update(bytes);
        }
        return WEAK_PREFIX + "\"" + Hex.encodeHexString(sha1.digest()) + "\"";
    }
}
