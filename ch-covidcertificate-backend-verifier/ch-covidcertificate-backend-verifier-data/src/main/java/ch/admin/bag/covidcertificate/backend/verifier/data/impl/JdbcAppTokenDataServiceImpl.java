package ch.admin.bag.covidcertificate.backend.verifier.data.impl;

import ch.admin.bag.covidcertificate.backend.verifier.data.AppTokenDataService;
import ch.admin.bag.covidcertificate.backend.verifier.data.mapper.AppTokenRowMapper;
import ch.admin.bag.covidcertificate.backend.verifier.model.AppToken;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

public class JdbcAppTokenDataServiceImpl implements AppTokenDataService {

    private final NamedParameterJdbcTemplate jt;

    public JdbcAppTokenDataServiceImpl(DataSource dataSource) {
        this.jt = new NamedParameterJdbcTemplate(dataSource);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppToken> getAppTokens() {
        final var sql = "select * from t_app_tokens";
        return jt.query(sql, new AppTokenRowMapper());
    }
}
