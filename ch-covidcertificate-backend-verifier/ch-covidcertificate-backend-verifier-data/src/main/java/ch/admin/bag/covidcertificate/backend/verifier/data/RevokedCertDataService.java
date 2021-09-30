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

import ch.admin.bag.covidcertificate.backend.verifier.model.DbRevokedCert;
import ch.admin.bag.covidcertificate.backend.verifier.model.cert.db.RevokedCertsUpdateResponse;
import java.time.Instant;
import java.util.List;
import java.util.Set;

public interface RevokedCertDataService {

    /** upserts the given revoked uvcis into the db */
    public RevokedCertsUpdateResponse replaceRevokedCerts(Set<String> revokedUvcis);

    /**
     * returns the next batch of released revoked certs after `since`
     *
     * @param since
     * @param now
     */
    public List<DbRevokedCert> findReleasedRevokedCerts(Long since, Instant now);

    /**
     * returns the highest released revoked cert pk id
     *
     * @param now
     */
    public long findMaxReleasedRevokedCertPkId(Instant now);

    public int getRevokedCertBatchSize();
}
