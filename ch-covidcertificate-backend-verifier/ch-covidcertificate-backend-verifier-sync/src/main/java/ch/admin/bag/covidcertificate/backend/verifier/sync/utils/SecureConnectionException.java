package ch.admin.bag.covidcertificate.backend.verifier.sync.utils;

public class SecureConnectionException extends RuntimeException {
    private static final long serialVersionUID = 298634479952096488L;

    public SecureConnectionException(String msg) {
        super(msg);
    }
}
