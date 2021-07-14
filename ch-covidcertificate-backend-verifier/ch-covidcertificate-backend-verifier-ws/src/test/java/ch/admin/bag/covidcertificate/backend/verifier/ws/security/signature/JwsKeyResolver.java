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

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.SigningKeyResolverAdapter;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import org.springframework.core.io.ClassPathResource;

public class JwsKeyResolver extends SigningKeyResolverAdapter {

    private final String pathToRootCert;

    public JwsKeyResolver(String pathToRootCert) {
        this.pathToRootCert = pathToRootCert;
    }

    @Override
    public Key resolveSigningKey(JwsHeader header, Claims claims) {
        try {
            var encodedCerts = (List<String>) header.get("x5c");
            var certificates = new ArrayList<X509Certificate>();
            for (String encodedCert : encodedCerts) {
                var certBytes = Base64.getDecoder().decode(encodedCert);
                X509Certificate x509 = getX509(certBytes);
                if (!certificates.isEmpty()) {
                    // verify last cert in chain with current cert
                    verifyCert(certificates.get(certificates.size() - 1), x509);
                }
                certificates.add(x509);
            }
            X509Certificate caX509 = getX509(pathToRootCert);

            // verify last cert in chain with root cert
            verifyCert(certificates.get(certificates.size() - 1), caX509);

            return certificates.get(0).getPublicKey();
        } catch (Exception e) {
            throw new InvalidSignatureException(e);
        }
    }

    private void verifyCert(X509Certificate certificateToVerify, X509Certificate x509)
            throws CertificateException, NoSuchAlgorithmException, SignatureException,
                    InvalidKeyException, NoSuchProviderException {
        certificateToVerify.verify(x509.getPublicKey());
    }

    private X509Certificate getX509(String pathToFile) throws IOException, CertificateException {
        String classpathPrefix = "classpath:";
        try (InputStream is =
                pathToFile.startsWith(classpathPrefix)
                        ? new ClassPathResource(pathToFile.replace(classpathPrefix, ""))
                                .getInputStream()
                        : new FileInputStream(pathToFile)) {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            return (X509Certificate) cf.generateCertificate(is);
        }
    }

    private X509Certificate getX509(byte[] bytes) throws IOException, CertificateException {
        try (InputStream is = new ByteArrayInputStream(bytes)) {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            return (X509Certificate) cf.generateCertificate(is);
        }
    }
}
