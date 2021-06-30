package ch.admin.bag.covidcertificate.backend.verifier.data.mapper;

import ch.admin.bag.covidcertificate.backend.verifier.model.cert.db.DbCsca;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.jdbc.core.RowMapper;

public class CSCARowMapper implements RowMapper<DbCsca> {

    @Override
    public DbCsca mapRow(ResultSet rs, int rowNum) throws SQLException {
        var dbCsca = new DbCsca();
        dbCsca.setId(rs.getLong("pk_csca_id"));
        dbCsca.setKeyId(rs.getString("key_id"));
        dbCsca.setCertificateRaw(rs.getString("certificate_raw"));
        dbCsca.setImportedAt(rs.getTimestamp("imported_at").toInstant());
        dbCsca.setOrigin(rs.getString("origin"));
        dbCsca.setSubjectPrincipalName(rs.getString("subject_principal_name"));
        return dbCsca;
    }
}
