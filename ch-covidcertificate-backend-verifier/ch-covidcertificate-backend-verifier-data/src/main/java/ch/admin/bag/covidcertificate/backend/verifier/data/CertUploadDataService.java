/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.backend.verifier.data;

import ch.admin.bag.covidcertificate.backend.verifier.model.cert.CertToUpload;
import java.util.List;

public interface CertUploadDataService {

    /**
     * returns dscs to upload that have not been uploaded to the dgc hub or inserted into the dsc
     * table yet
     *
     * @return
     */
    List<CertToUpload> findCertsToUpload();

    void updateCertToUpload(CertToUpload certToUpload);
}
