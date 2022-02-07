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

import java.time.Instant;

public class CertToUpload {
    private String alias;
    private String keyId;
    private Instant uploadedAt;
    private Boolean doUpload;
    private Instant insertedAt;
    private Boolean doInsert;

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getKeyId() {
        return keyId;
    }

    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(Instant uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public boolean wasUploaded() {
        return this.uploadedAt != null;
    }

    public Boolean doUpload() {
        return doUpload;
    }

    public void setDoUpload(Boolean doUpload) {
        this.doUpload = doUpload;
    }

    public Instant getInsertedAt() {
        return insertedAt;
    }

    public void setInsertedAt(Instant insertedAt) {
        this.insertedAt = insertedAt;
    }

    public boolean wasInserted() {
        return this.insertedAt != null;
    }

    public Boolean doInsert() {
        return doInsert;
    }

    public void setDoInsert(Boolean doInsert) {
        this.doInsert = doInsert;
    }
}
