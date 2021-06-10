/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.backend.verifier.ws.util;

import ch.admin.bag.covidcertificate.backend.verifier.ws.security.signature.JwsKeyResolver;
import ch.admin.bag.covidcertificate.backend.verifier.ws.security.signature.JwsMessageConverter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;

public class TestHelper {
    public static final Map<String, String> SECURITY_HEADERS =
            Map.of(
                    "X-Content-Type-Options",
                    "nosniff",
                    "X-Frame-Options",
                    "DENY",
                    "X-Xss-Protection",
                    "1; mode=block");

    public static final String PATH_TO_CA_PEM = "classpath:certs/test_ca.pem";
    public static final String PATH_TO_WRONG_CA_PEM = "classpath:certs/wrong_test_ca.pem";

    public final ObjectMapper objectMapper;

    public TestHelper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public <T> T verifyAndReadValue(
            MockHttpServletResponse result, MediaType mediaType, String pathToCaPem, Class<T> clazz)
            throws JsonProcessingException, UnsupportedEncodingException {
        String responseStr = result.getContentAsString(StandardCharsets.UTF_8);
        if (MediaType.APPLICATION_JSON.equalsTypeAndSubtype(mediaType)) {
            return objectMapper.readValue(responseStr, clazz);
        } else if (JwsMessageConverter.JWS_MEDIA_TYPE.equalsTypeAndSubtype(mediaType)) {
            // verify cert chain
            Jws<Claims> claimsJws =
                    Jwts.parserBuilder()
                            .setSigningKeyResolver(new JwsKeyResolver(pathToCaPem))
                            .build()
                            .parseClaimsJws(responseStr);
            return objectMapper.convertValue(claimsJws.getBody(), clazz);
        } else {
            throw new RuntimeException("unexpected media type: " + mediaType);
        }
    }
}
