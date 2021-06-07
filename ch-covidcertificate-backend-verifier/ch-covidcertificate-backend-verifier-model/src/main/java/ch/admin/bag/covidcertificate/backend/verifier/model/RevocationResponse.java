package ch.admin.bag.covidcertificate.backend.verifier.model;

import ch.ubique.openapi.docannotations.Documentation;
import java.util.List;

public class RevocationResponse {
    @Documentation(
            description = "list of revoked covidcerts",
            example = "[\"urn:uvci:01:CH:F0FDABC1708A81BB1A843891\"]")
    private List<String> revokedCerts;

    public List<String> getRevokedCerts() {
        return revokedCerts;
    }

    public void setRevokedCerts(List<String> revokedCerts) {
        this.revokedCerts = revokedCerts;
    }
}
