package ch.admin.bag.covidcertificate.backend.verifier.model;

import ch.ubique.openapi.docannotations.Documentation;
import java.time.Duration;
import java.util.List;

public class RevocationResponse {
    @Documentation(
            description = "list of revoked covidcerts",
            example = "[\"urn:uvci:01:CH:F0FDABC1708A81BB1A843891\"]")
    private List<String> revokedCerts;

    @Documentation(
            description = "describes how long the list response is valid for in ms",
            example = "172800000")
    private Duration validDuration = Duration.ofHours(48);

    public RevocationResponse() {}

    public RevocationResponse(List<String> revokedCerts) {
        this.revokedCerts = revokedCerts;
    }

    public List<String> getRevokedCerts() {
        return revokedCerts;
    }

    public void setRevokedCerts(List<String> revokedCerts) {
        this.revokedCerts = revokedCerts;
    }

    public Long getValidDuration() {
        return validDuration.toMillis();
    }

    public void setValidDuration(Long durationInMs) {
        this.validDuration = Duration.ofMillis(durationInMs);
    }
}
