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

    /**
     * inserts the given cscas into the db
     *
     * @param cscas
     */
    public void insertCscas(List<DbCsca> cscas);

    /**
     * removes all csas with key ids not in the given list
     *
     * @param keyIdsToKeep
     * @return number of removed cscas
     */
    public int removeCscasNotIn(List<String> keyIdsToKeep);

    /**
     * inserts the given dsc into the db
     *
     * @param dsc
     */
    public void insertDsc(List<DbDsc> dsc);

    /**
     * removes all dscs with key ids not in the given list
     *
     * @param keyIdsToKeep
     * @return number of removed dscs
     */
    public int removeDscsNotIn(List<String> keyIdsToKeep);

    /**
     * returns the next batch of dscs after since in the requested format
     *
     * @param since
     * @param certFormat
     * @return
     */
    public List<ClientCert> findDscs(Long since, CertFormat certFormat);

    /**
     * returns a list of key ids of all active dscs
     *
     * @return
     */
    public List<String> findActiveDscKeyIds();

    /**
     * returns the highest dsc pk id
     *
     * @return
     */
    public long findMaxDscPkId();

    public int getMaxDscBatchCount();
}
