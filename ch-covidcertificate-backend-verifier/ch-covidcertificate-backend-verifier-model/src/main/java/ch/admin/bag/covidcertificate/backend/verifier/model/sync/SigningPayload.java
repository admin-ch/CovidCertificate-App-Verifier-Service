// Copyright (c) 2021 Patrick Amrein <amrein@ubique.ch>
//
// This software is released under the MIT License.
// https://opensource.org/licenses/MIT

package ch.admin.bag.covidcertificate.backend.verifier.model.sync;

public class SigningPayload {
    private String data;

    /** @return the data */
    public String getData() {
        return data;
    }

    /** @param data the data to set */
    public void setData(String data) {
        this.data = data;
    }
}
