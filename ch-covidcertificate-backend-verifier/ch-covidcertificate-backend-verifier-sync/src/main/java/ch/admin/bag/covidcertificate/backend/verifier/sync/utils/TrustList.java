package ch.admin.bag.covidcertificate.backend.verifier.sync.utils;

// TODO: Add field annotations for json parsing
public class TrustList {

  private String kid;

  // TODO: Can this be directly parsed into data-time format?
  private String timestamp;

  private String country;

  // TODO: Can this be replaced with an enum?
  private String certificateType;

  private String thumbprint;

  private String signature;

  // TODO: String or byte array?
  private String rawData;

  public String getKid() {
    return kid;
  }

  public void setKid(String kid) {
    this.kid = kid;
  }

  public String getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(String timestamp) {
    this.timestamp = timestamp;
  }

  public String getCountry() {
    return country;
  }

  public void setCountry(String country) {
    this.country = country;
  }

  public String getCertificateType() {
    return certificateType;
  }

  public void setCertificateType(String certificateType) {
    this.certificateType = certificateType;
  }

  public String getThumbprint() {
    return thumbprint;
  }

  public void setThumbprint(String thumbprint) {
    this.thumbprint = thumbprint;
  }

  public String getSignature() {
    return signature;
  }

  public void setSignature(String signature) {
    this.signature = signature;
  }

  public String getRawData() {
    return rawData;
  }

  public void setRawData(String rawData) {
    this.rawData = rawData;
  }
}
