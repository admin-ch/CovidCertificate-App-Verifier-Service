package ch.admin.bag.covidcertificate.backend.verifier.model.cert;

import ch.ubique.openapi.docannotations.Documentation;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class ActiveCertsResponse {
    @Documentation(description = "list of active key ids")
    private List<String> activeKeyIds = new ArrayList<>();

    @Documentation(
            description = "describes how long the list response is valid for in ms",
            example = "172800000")
    private Duration validDuration = Duration.ofHours(48);

    public ActiveCertsResponse(List<String> activeKeyIds) {
        if (activeKeyIds == null) {
            activeKeyIds = new ArrayList<>();
        }
        this.activeKeyIds = activeKeyIds;
    }

    public List<String> getActiveKeyIds() {
        return activeKeyIds;
    }

    public void setActiveKeyIds(List<String> activeKeyIds) {
        this.activeKeyIds = activeKeyIds;
    }

    public Long getValidDuration() {
        return validDuration.toMillis();
    }

    public void setValidDuration(Long durationInMs) {
        this.validDuration = Duration.ofMillis(durationInMs);
    }
}
