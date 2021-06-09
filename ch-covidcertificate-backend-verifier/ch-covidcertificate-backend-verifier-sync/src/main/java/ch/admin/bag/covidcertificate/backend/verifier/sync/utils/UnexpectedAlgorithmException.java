package ch.admin.bag.covidcertificate.backend.verifier.sync.utils;

public class UnexpectedAlgorithmException extends Exception {
    private static final long serialVersionUID = 298634479952096487L;

    public UnexpectedAlgorithmException(String algName) {
        super("unexpected algorithm: " + algName);
    }
}
