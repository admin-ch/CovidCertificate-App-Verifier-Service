package ch.admin.bag.covidcertificate.backend.verifier.model.cert;

public enum Algorithm {
    ES256,
    RS256;

    public static Algorithm forSigAlgName(String sigAlgName) {
        if (sigAlgName.contains("SHA256")){
            if (sigAlgName.contains("ECDSA")) {
                return ES256;
            } else if (sigAlgName.contains("RSA")) {
                return RS256;
            }
        }
        return null;
    }
}
