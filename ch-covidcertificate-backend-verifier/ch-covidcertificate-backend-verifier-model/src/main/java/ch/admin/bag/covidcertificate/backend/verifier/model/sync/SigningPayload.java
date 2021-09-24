// Copyright (c) 2021 Patrick Amrein <amrein@ubique.ch>
//
// This software is released under the MIT License.
// https://opensource.org/licenses/MIT

package ch.admin.bag.covidcertificate.backend.verifier.model.sync;

public class SigningPayload {
    private final String data;

    public SigningPayload(String data) {
        this.data = data;
    }

    public String getData() {
        return data;
    }
}
