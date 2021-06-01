package ch.admin.bag.covidcertificate.backend.verifier.model.cert;

import java.util.ArrayList;
import java.util.List;

public class ActiveCertsResponse {
    private List<String> activeKeyIds = new ArrayList<>();

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
}
