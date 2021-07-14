/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.backend.verifier.model.cert;

import ch.ubique.openapi.docannotations.Documentation;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class ClientCert {

    @JsonIgnore private Long pkId;

    @Documentation(description = "base64 encoded. shasum of x509")
    private String keyId;

    @Documentation(
            description =
                    "either 'sig' (all) or one or more of: 'r' (recovery), 't' (test), 'v' (vaccine), 'l' (light)",
            example = "sig")
    private String use;

    @Documentation(
            description = "ES256 or RS256. key type can be derived from algorithm",
            example = "ES256")
    private Algorithm alg;

    // RSA ONLY
    @Documentation(description = "base64 encoded. RSA only (android)")
    private String n;

    @Documentation(description = "base64 encoded. RSA only (android)")
    private String e;

    @Documentation(description = "base64 encoded. RSA only (ios)")
    private String subjectPublicKeyInfo;

    // EC ONLY
    @Documentation(
            description = "EC only. only 'P-256' is supported at this time",
            example = "P-256")
    private String crv;

    @Documentation(description = "base64 encoded. EC only")
    private String x;

    @Documentation(description = "base64 encoded. EC only")
    private String y;

    public Long getPkId() {
        return pkId;
    }

    public void setPkId(Long pkId) {
        this.pkId = pkId;
    }

    public String getKeyId() {
        return keyId;
    }

    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }

    public String getUse() {
        return use;
    }

    public void setUse(String use) {
        this.use = use;
    }

    public Algorithm getAlg() {
        return alg;
    }

    public void setAlg(Algorithm alg) {
        this.alg = alg;
    }

    public String getN() {
        return n;
    }

    public void setN(String n) {
        this.n = n;
    }

    public String getE() {
        return e;
    }

    public void setE(String e) {
        this.e = e;
    }

    public String getSubjectPublicKeyInfo() {
        return subjectPublicKeyInfo;
    }

    public void setSubjectPublicKeyInfo(String subjectPublicKeyInfo) {
        this.subjectPublicKeyInfo = subjectPublicKeyInfo;
    }

    public String getCrv() {
        return crv;
    }

    public void setCrv(String crv) {
        this.crv = crv;
    }

    public String getX() {
        return x;
    }

    public void setX(String x) {
        this.x = x;
    }

    public String getY() {
        return y;
    }

    public void setY(String y) {
        this.y = y;
    }
}
