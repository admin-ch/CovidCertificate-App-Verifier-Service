package ch.admin.bag.covidcertificate.backend.verifier.data.mapper;

import ch.admin.bag.covidcertificate.backend.verifier.model.AppToken;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.jdbc.core.RowMapper;

public class AppTokenRowMapper implements RowMapper<AppToken> {

    @Override
    public AppToken mapRow(ResultSet resultSet, int i) throws SQLException {
        var appToken = new AppToken();
        appToken.setApiKey(resultSet.getString("api_key"));
        appToken.setDescription(resultSet.getString("description"));
        return appToken;
    }
}
