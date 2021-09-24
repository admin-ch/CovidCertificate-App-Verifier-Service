/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.backend.verifier.sync.ws.model;

import java.util.ArrayList;
import java.util.List;

public class DscUploadResult {
    private final String alias;
    private String kid;
    private List<String> errors = new ArrayList<>();
    private List<String> infos = new ArrayList<>();

    public DscUploadResult(String alias) {
        this.alias = alias;
    }

    public String getAlias() {
        return alias;
    }

    public String getKid() {
        return kid;
    }

    public void setKid(String kid) {
        this.kid = kid;
    }

    public boolean isSuccess() {
        return errors.isEmpty();
    }

    public List<String> getErrors() {
        return errors;
    }

    public void addError(String error) {
        this.errors.add(error);
    }

    public List<String> getInfos() {
        return infos;
    }

    public void addInfo(String info) {
        this.infos.add(info);
    }
}
