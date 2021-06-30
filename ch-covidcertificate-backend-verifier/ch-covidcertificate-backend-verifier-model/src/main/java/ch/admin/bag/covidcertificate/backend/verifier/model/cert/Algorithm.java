package ch.admin.bag.covidcertificate.backend.verifier.model.cert;

public enum Algorithm {
    ES256,
    RS256,
    UNSUPPORTED;

    public static Algorithm forPubKeyType(String keyType) {
        if ("RSA".equals(keyType)) {
            return RS256;
        } else if ("EC".equals(keyType)) {
            return ES256;
        } else {
            return UNSUPPORTED;
        }
    }
}
