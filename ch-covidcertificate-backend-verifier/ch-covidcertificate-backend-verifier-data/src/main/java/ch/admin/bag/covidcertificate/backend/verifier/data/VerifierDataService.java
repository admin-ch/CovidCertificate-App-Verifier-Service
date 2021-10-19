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

import ch.admin.bag.covidcertificate.backend.verifier.model.cert.CertFormat;
import ch.admin.bag.covidcertificate.backend.verifier.model.cert.ClientCert;
import ch.admin.bag.covidcertificate.backend.verifier.model.cert.db.DbCsca;
import ch.admin.bag.covidcertificate.backend.verifier.model.cert.db.DbDsc;
import java.time.Duration;
import java.util.Date;
import java.util.List;

public interface VerifierDataService {

    /** inserts the given CSCAs into the db */
    public void insertCscas(List<DbCsca> cscas);

    /**
     * removes all csas with key ids in the given list that haven't been added manually
     *
     * @param keyIds
     * @return number of removed CSCAs
     */
    public int removeCscas(List<String> keyIds);

    /**
     * finds all CSCAs of the given origin country
     *
     * @param origin abbreviation for country of origin (e.g. "CH")
     * @return list of all CSCAs with the corresponding origin
     */
    public List<DbCsca> findCscas(String origin);

    /** returns a list of key ids of all active CSCAs */
    public List<String> findActiveCscaKeyIds();

    /** inserts the given DSCs into the db */
    public void insertDscs(List<DbDsc> dsc);

    /** inserts the given manual DSC into the db */
    void insertManualDsc(DbDsc dsc);

    /** removes all DSCs with key ids not in the given list that haven't been added manually */
    public int removeDscsNotIn(List<String> keyIdsToKeep);

    /** removes all DSCs signed by a CSCA in the given list that haven't been added manually */
    public int removeDscsWithCscaIn(List<String> cscaKidsToRemove);

    public List<DbDsc> findDscsMarkedForDeletion();

    /** returns the next batch of DSCs after `since` up to `upTo` in the requested format */
    public List<ClientCert> findDscs(Long since, CertFormat certFormat, Long upTo);

    /**
     * returns the next batch of DSCs after `since` but before `importedBefore` in the requested
     * format
     *
     * @deprecated only used in KeyController V1
     */
    @Deprecated(since = "KeyControllerV2", forRemoval = true)
    public List<ClientCert> findDscsBefore(Long since, CertFormat certFormat, Date importedBefore);

    /** returns a list of key ids of all active DSCs */
    public List<String> findActiveDscKeyIds();

    /**
     * returns a list of key ids of all active DSCs before a certain timestamp
     *
     * @deprecated only used in KeyController V1
     */
    @Deprecated(since = "KeyControllerV2", forRemoval = true)
    public List<String> findActiveDscKeyIdsBefore(Date importedBefore);

    /** returns the highest DSC pk id */
    public long findMaxDscPkId();

    public int getDscBatchSize();

    public long findChCscaPkId();

    public int cleanUpDscsMarkedForDeletion();

    public Duration getKeepDscsMarkedForDeletionDuration();
}
