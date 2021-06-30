package ch.admin.bag.covidcertificate.backend.verifier.model;

public class AppToken {

    private String apiKey;
    private String description;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
