package ch.admin.bag.covidcertificate.backend.verifier.model.sync;

/** Possible certificate types returned by /trustList */
public enum CertificateType {
    AUTHENTICATION,
    UPLOAD,
    CSCA,
    DSC;
}
