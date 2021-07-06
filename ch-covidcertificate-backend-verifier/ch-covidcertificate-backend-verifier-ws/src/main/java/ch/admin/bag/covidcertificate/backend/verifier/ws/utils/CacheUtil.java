package ch.admin.bag.covidcertificate.backend.verifier.ws.utils;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.http.HttpHeaders;

public class CacheUtil {
    public static Duration REVOCATION_LIST_MAX_AGE;
    public static Duration VERIFICATION_RULES_MAX_AGE;
    public static Duration VALUE_SETS_MAX_AGE;
    public static Duration KEYS_BUCKET_DURATION;

    private CacheUtil() {}

    /**
     * Formats the date to a http timestamp.
     *
     * @param date
     * @return
     */
    private static String formatHeaderDate(OffsetDateTime date) {
        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z")
                        .withLocale(Locale.US)
                        .withZone(ZoneId.of("GMT"));
        return formatter.format(date);
    }

    public static HttpHeaders createExpiresHeader(OffsetDateTime expires) {
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
    public static OffsetDateTime roundToNextBucket(
            OffsetDateTime now, Duration releaseBucketDuration) {
        long rounding = releaseBucketDuration.toMillis();
        long timestamp = ((now.toInstant().toEpochMilli() / rounding) + 1) * rounding;
        return OffsetDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneOffset.UTC);
    }

    public static OffsetDateTime roundToNextBucket(OffsetDateTime now) {
        return roundToNextBucket(now, CacheUtil.KEYS_BUCKET_DURATION);
    }
}
