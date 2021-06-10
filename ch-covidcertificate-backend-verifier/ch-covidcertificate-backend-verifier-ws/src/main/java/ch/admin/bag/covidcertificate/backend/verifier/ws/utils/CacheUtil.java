package ch.admin.bag.covidcertificate.backend.verifier.ws.utils;

import java.time.Duration;

public class CacheUtil {
    public static Duration REVOCATION_LIST_MAX_AGE;
    public static Duration VERIFICATION_RULES_MAX_AGE;
    public static Duration KEYS_UPDATE_MAX_AGE;
    public static Duration KEYS_LIST_MAX_AGE;

    private CacheUtil() {}
}
