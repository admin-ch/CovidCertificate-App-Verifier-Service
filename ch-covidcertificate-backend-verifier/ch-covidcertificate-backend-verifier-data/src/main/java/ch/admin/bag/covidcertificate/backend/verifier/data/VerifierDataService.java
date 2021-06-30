/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.backend.verifier.data;

import ch.admin.bag.covidcertificate.backend.verifier.model.cert.CertFormat;
import ch.admin.bag.covidcertificate.backend.verifier.model.cert.ClientCert;
import ch.admin.bag.covidcertificate.backend.verifier.model.cert.db.DbCsca;
import ch.admin.bag.covidcertificate.backend.verifier.model.cert.db.DbDsc;
import java.util.List;

public interface VerifierDataService {

    /** inserts the given CSCAs into the db */
    public void insertCSCAs(List<DbCsca> cscas);

    /**
     * removes all csas with key ids in the given list
     *
     * @param keyIds
     * @return number of removed CSCAs
     */
    public int removeCSCAs(List<String> keyIds);

    /**
     * finds all CSCAs of the given origin country
     *
     * @param origin abbreviation for country of origin (e.g. "CH")
     * @return list of all CSCAs with the corresponding origin
     */
    public List<DbCsca> findCSCAs(String origin);

    /** returns a list of key ids of all active CSCAs */
    public List<String> findActiveCSCAKeyIds();

    /** inserts the given DSC into the db */
    public void insertDSCs(List<DbDsc> dsc);

    /** removes all DSCs with key ids not in the given list */
    public int removeDSCsNotIn(List<String> keyIdsToKeep);

    /** removes all DSCs signed by a CSCA in the given list */
    public int removeDSCsWithCSCAIn(List<String> cscaKidsToRemove);

    /** returns the next batch of DSCs after since in the requested format */
    public List<ClientCert> findDSCs(Long since, CertFormat certFormat);

    /** returns a list of key ids of all active DSCs */
    public List<String> findActiveDSCKeyIds();

    /** returns the highest DSC pk id */
    public long findMaxDSCPkId();

    public int getMaxDSCBatchCount();
}
