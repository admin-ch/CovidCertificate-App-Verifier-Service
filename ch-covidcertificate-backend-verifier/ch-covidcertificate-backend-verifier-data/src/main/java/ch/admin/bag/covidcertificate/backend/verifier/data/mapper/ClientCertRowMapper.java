package ch.admin.bag.covidcertificate.backend.verifier.data.mapper;

import ch.admin.bag.covidcertificate.backend.verifier.model.cert.Algorithm;
import ch.admin.bag.covidcertificate.backend.verifier.model.cert.CertFormat;
import ch.admin.bag.covidcertificate.backend.verifier.model.cert.ClientCert;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.jdbc.core.RowMapper;

public class ClientCertRowMapper implements RowMapper<ClientCert> {

    private CertFormat certFormat;

    public ClientCertRowMapper(CertFormat certFormat) {
        this.certFormat = certFormat;
    }

    @Override
    public ClientCert mapRow(ResultSet rs, int rowNum) throws SQLException {
        var clientCert = new ClientCert();
        clientCert.setKeyId(rs.getString("key_id"));
        clientCert.setUse(rs.getString("use"));
        clientCert.setAlg(Algorithm.valueOf(rs.getString("alg")));
        switch (certFormat) {
            case IOS:
                clientCert.setSubjectPublicKeyInfo(rs.getString("subject_public_key_info"));
                clientCert.setCrv(rs.getString("crv"));
                clientCert.setX(rs.getString("x"));
                clientCert.setY(rs.getString("y"));
                break;
            case ANDROID:
                clientCert.setN(rs.getString("n"));
                clientCert.setE(rs.getString("e"));
                break;
        }
        return clientCert;
    }
}
