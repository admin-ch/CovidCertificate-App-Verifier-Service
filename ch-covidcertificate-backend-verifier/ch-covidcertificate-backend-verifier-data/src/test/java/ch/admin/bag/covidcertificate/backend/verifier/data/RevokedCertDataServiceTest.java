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

import static org.junit.jupiter.api.Assertions.assertEquals;

import ch.admin.bag.covidcertificate.backend.verifier.data.util.CacheUtil;
import ch.admin.bag.covidcertificate.backend.verifier.data.util.TestUtil;
import java.time.Instant;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RevokedCertDataServiceTest extends BaseDataServiceTest {

    @Autowired RevokedCertDataService revokedCertDataService;
    @Autowired DataSource dataSource;

    @Test
    void getAppTokensTest() {
        Instant now = Instant.now();

        // import revoked certs
        int releasedRevokedCertCount = 20;
        revokedCertDataService.replaceRevokedCerts(
                TestUtil.getRevokedCertUvcis(releasedRevokedCertCount));

        // set imported_at back by one retention bucket period so revoked certs will be released
        TestUtil.releaseRevokedCerts(jt, now);

        // check released count and max pk id
        assertEquals(
                releasedRevokedCertCount,
                revokedCertDataService.findReleasedRevokedCerts(0L, now).size());
        long maxReleasedPkId = revokedCertDataService.findMaxReleasedRevokedCertPkId(now);

        // import additional revoked certs which should not be released yet
        int newRevokedCertCount = 10;
        revokedCertDataService.replaceRevokedCerts(
                TestUtil.getRevokedCertUvcis(releasedRevokedCertCount + newRevokedCertCount));

        // check released count and max pk id (unchanged)
        assertEquals(
                releasedRevokedCertCount,
                revokedCertDataService.findReleasedRevokedCerts(0L, now).size());
        assertEquals(maxReleasedPkId, revokedCertDataService.findMaxReleasedRevokedCertPkId(now));

        // check one revocation retention bucket duration later
        now = now.plus(CacheUtil.REVOCATION_RETENTION_BUCKET_DURATION);
        assertEquals(
                releasedRevokedCertCount + newRevokedCertCount,
                revokedCertDataService.findReleasedRevokedCerts(0L, now).size());
        assertEquals(
                maxReleasedPkId + newRevokedCertCount,
                revokedCertDataService.findMaxReleasedRevokedCertPkId(now));
    }
}
