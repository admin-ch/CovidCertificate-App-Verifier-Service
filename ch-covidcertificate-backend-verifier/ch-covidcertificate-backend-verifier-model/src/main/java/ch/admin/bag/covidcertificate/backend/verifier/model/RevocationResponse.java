package ch.admin.bag.covidcertificate.backend.verifier.model;

import java.util.List;

public class RevocationResponse {
    private List<String> revokedCerts;

    public List<String> getRevokedCerts() {
        return revokedCerts;
    }

    public void setRevokedCerts(List<String> revokedCerts) {
        this.revokedCerts = revokedCerts;
    }
}
