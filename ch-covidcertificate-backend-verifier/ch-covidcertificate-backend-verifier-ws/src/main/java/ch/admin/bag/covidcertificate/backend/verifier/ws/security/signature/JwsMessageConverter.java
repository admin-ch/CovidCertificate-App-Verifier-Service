/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.backend.verifier.ws.security.signature;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateEncodingException;
import java.util.ArrayList;
import java.util.Map;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractGenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.util.StreamUtils;

/**
 * Implementation of {@link HttpMessageConverter} that can write any object as JWS. <br>
 * <b>Note:</b> Reading is not supported.
 *
 * @author alig
 */
public class JwsMessageConverter extends AbstractGenericHttpMessageConverter<Object> {

    public static final MediaType JWS_MEDIA_TYPE = new MediaType("application", "json+jws");
    private final KeyStore keyStore;
    private final ObjectMapper objectMapper;
    private final String alias;
    private final char[] password;
    private final ArrayList<byte[]> certificateChain;
    private final Key privateKey;

    public JwsMessageConverter(KeyStore keyStore, char[] password)
            throws KeyStoreException, CertificateEncodingException, UnrecoverableKeyException,
                    NoSuchAlgorithmException {
        super(JWS_MEDIA_TYPE);
        this.keyStore = keyStore;
        this.objectMapper = new ObjectMapper();
        this.alias = this.keyStore.aliases().nextElement();
        this.password = password;
        this.certificateChain =
                new ArrayList<>(this.keyStore.getCertificateChain(this.alias).length);
        for (var cert : this.keyStore.getCertificateChain(this.alias)) {
            this.certificateChain.add(cert.getEncoded());
        }

        this.privateKey = keyStore.getKey(this.alias, this.password);
    }

    @Override
    public Object read(Type type, Class<?> contextClass, HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {
        throw new UnsupportedOperationException("This converter does not support reading");
    }

    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
        // any clazz can be written
        return JWS_MEDIA_TYPE.equalsTypeAndSubtype(mediaType) || mediaType == null;
    }

    @Override
    protected Object readInternal(Class<? extends Object> clazz, HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {
        throw new UnsupportedOperationException("This converter does not support reading");
    }

    @Override
    protected void writeInternal(Object t, Type type, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
        @SuppressWarnings("unchecked")
        Map<String, Object> claims = objectMapper.convertValue(t, Map.class);
        String signature;
        signature =
                Jwts.builder()
                        .setHeaderParam("x5c", this.certificateChain)
                        .setClaims(claims)
                        .signWith(this.privateKey)
                        .compact();

        StreamUtils.copy(signature, StandardCharsets.UTF_8, outputMessage.getBody());
    }
}
