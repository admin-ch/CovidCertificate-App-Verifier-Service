package ch.admin.bag.covidcertificate.backend.verifier.model.push_registration;

public class PushRegistration {
    private final String pushToken;

    private final PushType pushType;

    public PushRegistration(String pushToken, PushType pushType) {
        this.pushToken = pushToken;
        this.pushType = pushType;
    }

    public String getPushToken() {
        return pushToken;
    }

    public PushType getPushType() {
        return pushType;
    }
}
