package ch.admin.bag.covidcertificate.backend.verifier.model.push_registration;

public class PushRegistration {
    private String pushToken;

    private PushType pushType;

    private String deviceId;

    public PushRegistration(String pushToken, PushType pushType, String deviceId) {
        this.pushToken = pushToken;
        this.pushType = pushType;
        this.deviceId = deviceId;
    }

    public String getPushToken() {
        return pushToken;
    }

    public PushType getPushType() {
        return pushType;
    }

    public String getDeviceId() {
        return deviceId;
    }
}
