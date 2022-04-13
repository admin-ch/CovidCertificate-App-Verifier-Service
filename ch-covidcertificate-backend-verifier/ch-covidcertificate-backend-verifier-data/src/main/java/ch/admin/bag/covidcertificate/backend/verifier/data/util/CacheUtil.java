/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.backend.verifier.data.util;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.http.HttpHeaders;

public class CacheUtil {
    public static Duration REVOCATION_LIST_V1_MAX_AGE;
    public static Duration VERIFICATION_RULES_MAX_AGE;
    public static Duration VALUE_SETS_MAX_AGE;
    public static Duration KEYS_UPDATES_MAX_AGE;
    public static Duration KEYS_LIST_MAX_AGE;
    public static Duration FOREIGN_RULES_MAX_AGE;

    public static Duration KEYS_BUCKET_DURATION;
    public static Duration REVOCATION_RETENTION_BUCKET_DURATION;
    public static Duration VERIFICATION_RULES_BUCKET_DURATION;
    public static Duration FOREIGN_RULES_BUCKET_DURATION;


    private CacheUtil() {}

    /**
     * Formats the date to a http timestamp.
     *
     * @param instant
     * @return
     */
    public static String formatHeaderDate(Instant instant) {
        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z")
                        .withLocale(Locale.US)
                        .withZone(ZoneId.of("GMT"));
        return formatter.format(instant);
    }

    public static HttpHeaders createExpiresHeader(Instant expires) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Expires", formatHeaderDate(expires));
        return headers;
    }

    /**
     * @param now
     * @param releaseBucketDuration
     * @return the start of the next bucket. If the `now` is at the beginning of a bucket, the next
     *     bucket will be returned.
     */
    private static Instant roundToNextBucketStart(Instant now, Duration releaseBucketDuration) {
        long rounding = releaseBucketDuration.toMillis();
        long timestamp = ((now.toEpochMilli() / rounding) + 1) * rounding;
        return Instant.ofEpochMilli(timestamp);
    }

    private static Instant roundToPreviousBucketStart(Instant now, Duration releaseBucketDuration) {
        return roundToNextBucketStart(now, releaseBucketDuration).minus(releaseBucketDuration);
    }

    public static Instant roundToNextKeysBucketStart(Instant now) {
        return roundToNextBucketStart(now, CacheUtil.KEYS_BUCKET_DURATION);
    }

    public static Instant roundToNextRevocationRetentionBucketStart(Instant now) {
        return roundToNextBucketStart(now, CacheUtil.REVOCATION_RETENTION_BUCKET_DURATION);
    }

    public static Instant roundToPreviousRevocationRetentionBucketStart(Instant now) {
        return roundToPreviousBucketStart(now, CacheUtil.REVOCATION_RETENTION_BUCKET_DURATION);
    }

    public static Instant roundToNextVerificationRulesBucketStart(Instant now) {
        return roundToNextBucketStart(now, CacheUtil.VERIFICATION_RULES_BUCKET_DURATION);
    }

    public static Instant roundToNextForeignRulesBucketStart(Instant now) {
        return roundToNextBucketStart(now, CacheUtil.FOREIGN_RULES_BUCKET_DURATION);
    }
}
