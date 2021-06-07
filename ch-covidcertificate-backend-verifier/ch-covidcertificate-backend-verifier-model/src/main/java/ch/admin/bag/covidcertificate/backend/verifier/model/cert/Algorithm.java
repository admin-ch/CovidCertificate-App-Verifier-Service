package ch.admin.bag.covidcertificate.backend.verifier.model.cert;

public enum Algorithm {
    ES256,
    RS256,
    PS256;

    public static Algorithm forSigAlgName(String sigAlgName) {
        if(sigAlgName.contains("RSA")) {
            if(sigAlgName.contains("SHA256")) {
                return RS256;
            } else if(sigAlgName.contains("PSS")) {
                return PS256;
            }
        } else if(sigAlgName.contains("ECDSA") && sigAlgName.contains("SHA256")) {
            return ES256;
        }
        return null;
    }
}
