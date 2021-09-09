// Copyright (c) 2021 Patrick Amrein <amrein@ubique.ch>
//
// This software is released under the MIT License.
// https://opensource.org/licenses/MIT

package ch.admin.bag.covidcertificate.backend.verifier.model.sync;

public class CmsResponse {
    private String cms;

    /** @return the cms */
    public String getCms() {
        return cms;
    }

    /** @param cms the cms to set */
    public void setCms(String cms) {
        this.cms = cms;
    }
}
