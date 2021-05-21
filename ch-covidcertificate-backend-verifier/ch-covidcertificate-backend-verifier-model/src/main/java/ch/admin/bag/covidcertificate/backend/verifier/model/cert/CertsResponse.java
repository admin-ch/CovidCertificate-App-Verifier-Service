package ch.admin.bag.covidcertificate.backend.verifier.model.cert;

import java.util.ArrayList;
import java.util.List;

public class CertsResponse {
    private List<ClientCert> certs = new ArrayList<>();

    public List<ClientCert> getCerts() {
        return certs;
    }

    public void setCerts(List<ClientCert> certs) {
        this.certs = certs;
    }
}
