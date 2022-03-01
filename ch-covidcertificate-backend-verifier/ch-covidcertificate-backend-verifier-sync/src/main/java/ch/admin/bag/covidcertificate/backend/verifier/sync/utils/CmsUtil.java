/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.backend.verifier.sync.utils;

import ch.admin.bag.covidcertificate.backend.verifier.model.cert.db.DbDsc;
import ch.admin.bag.covidcertificate.backend.verifier.model.exception.InvalidSignatureException;
import ch.admin.bag.covidcertificate.backend.verifier.model.sync.SigningPayload;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Iterator;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.util.Store;
import org.springframework.http.HttpHeaders;

public class CmsUtil {

    private CmsUtil() {}

    public static HttpHeaders createRuleExchangeHeaders() {
        var headers = new HttpHeaders();
        headers.add(HttpHeaders.ACCEPT, "application/json");
        headers.add(HttpHeaders.CONTENT_TYPE, "application/cms-text");
        return headers;
    }

    public static SigningPayload encodePayload(byte[] payload){
        var base64encoded =
                Base64.getEncoder()
                        .encodeToString(payload);
        return new SigningPayload(base64encoded);
    }


    public static HttpHeaders createCmsUploadHeaders() {
        var headers = new HttpHeaders();
        headers.add(HttpHeaders.ACCEPT, "application/json");
        headers.add(HttpHeaders.CONTENT_TYPE, "application/cms");
        return headers;
    }

    public static DbDsc decodeDscCms(String cms)
            throws CMSException, CertificateException, IOException, OperatorCreationException,
                    UnexpectedAlgorithmException, NoSuchAlgorithmException {
        CMSSignedData cmsSignedData = new CMSSignedData(Base64.getDecoder().decode(cms));
        validateSignature(cmsSignedData);
        X509Certificate x509 = getPayload(cmsSignedData);
        return TrustListMapper.mapDsc(x509, "CH", getKeyId(x509));
    }

    private static void validateSignature(CMSSignedData cmsSignedData)
            throws CertificateException, IOException, CMSException, OperatorCreationException {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        Store<X509CertificateHolder> store = cmsSignedData.getCertificates();
        Iterator<SignerInformation> signerIt =
                cmsSignedData.getSignerInfos().getSigners().iterator();

        while (signerIt.hasNext()) {
            SignerInformation signer = signerIt.next();

            Collection<X509CertificateHolder> certCollection = store.getMatches(signer.getSID());
            X509CertificateHolder certHolder = certCollection.stream().findFirst().get();
            X509Certificate cert = x509FromBytes(certHolder.getEncoded());

            if (signer.verify(
                    new JcaSimpleSignerInfoVerifierBuilder().setProvider("BC").build(cert))) {
                return;
            }
        }
        throw new InvalidSignatureException();
    }

    private static X509Certificate getPayload(CMSSignedData cmsSignedData)
            throws CertificateException {
        return x509FromBytes((byte[]) cmsSignedData.getSignedContent().getContent());
    }

    private static X509Certificate x509FromBytes(byte[] bytes) throws CertificateException {
        var cf = CertificateFactory.getInstance("X.509");
        return (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(bytes));
    }

    private static String getKeyId(X509Certificate x509)
            throws NoSuchAlgorithmException, CertificateEncodingException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(x509.getEncoded());
        return Base64.getEncoder().encodeToString(Arrays.copyOfRange(hash, 0, 8));
    }
}
