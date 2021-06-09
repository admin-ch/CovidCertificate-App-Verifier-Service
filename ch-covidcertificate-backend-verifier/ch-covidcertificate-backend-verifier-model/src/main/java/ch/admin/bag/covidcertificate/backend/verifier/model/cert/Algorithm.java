package ch.admin.bag.covidcertificate.backend.verifier.model.cert;

public enum Algorithm {
    ES256,
    RS256,
    PS256,
    UNSUPPORTED;

    public static Algorithm forSigAlgName(String sigAlgName) {
        if (sigAlgName.contains("RSA")) {
            if (sigAlgName.contains("SHA256")) {
                return RS256;
            } else if (sigAlgName.contains("PSS")) {
                return PS256;
            }
        } else if (sigAlgName.contains("ECDSA") && sigAlgName.contains("SHA256")) {
            return ES256;
        }
        return UNSUPPORTED;
    }

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
