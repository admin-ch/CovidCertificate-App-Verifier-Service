/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.backend.verifier.model.cert.db;

import java.time.LocalDateTime;

public class DbCsca {
    private Long id;
    private String keyId;
    private String certificateRaw;
    private LocalDateTime importedAt;
    private String origin;
    private String subjectPrincipalName;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getKeyId() {
        return keyId;
    }

    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }

    public String getCertificateRaw() {
        return certificateRaw;
    }

    public void setCertificateRaw(String certificateRaw) {
        this.certificateRaw = certificateRaw;
    }

    public LocalDateTime getImportedAt() {
        return importedAt;
    }

    public void setImportedAt(LocalDateTime importedAt) {
        this.importedAt = importedAt;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getSubjectPrincipalName() {
        return subjectPrincipalName;
    }

    public void setSubjectPrincipalName(String subjectPrincipalName) {
        this.subjectPrincipalName = subjectPrincipalName;
    }
}
