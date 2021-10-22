package ch.admin.bag.covidcertificate.backend.verifier.model.sync;

public class DscRestoreResponse {
    private final int restoredCscaCount;
    private final int restoredDscCount;

    public DscRestoreResponse(int restoredCscaCount, int restoredDscCount) {
        this.restoredCscaCount = restoredCscaCount;
        this.restoredDscCount = restoredDscCount;
    }

    public int getRestoredCscaCount() {
        return restoredCscaCount;
    }

    public int getRestoredDscCount() {
        return restoredDscCount;
    }
}
