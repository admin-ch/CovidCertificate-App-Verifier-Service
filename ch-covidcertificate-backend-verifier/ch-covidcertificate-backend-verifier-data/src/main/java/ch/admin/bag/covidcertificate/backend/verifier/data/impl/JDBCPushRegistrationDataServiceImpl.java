package ch.admin.bag.covidcertificate.backend.verifier.data.impl;

import ch.admin.bag.covidcertificate.backend.verifier.data.PushRegistrationDataService;
import ch.admin.bag.covidcertificate.backend.verifier.data.mapper.PushRegistrationRowMapper;
import ch.admin.bag.covidcertificate.backend.verifier.model.push_registration.PushRegistration;
import ch.admin.bag.covidcertificate.backend.verifier.model.push_registration.PushType;
import java.util.List;
import javax.sql.DataSource;
import org.apache.logging.log4j.util.Strings;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.transaction.annotation.Transactional;

// TODO: Copy unit tests from swisscovid-cn (after merge of sync branch)

public class JDBCPushRegistrationDataServiceImpl implements PushRegistrationDataService {

    private final NamedParameterJdbcTemplate jt;
    private final SimpleJdbcInsert pushRegistrationInsert;

    public JDBCPushRegistrationDataServiceImpl(final DataSource dataSource) {
        this.jt = new NamedParameterJdbcTemplate(dataSource);
        this.pushRegistrationInsert =
                new SimpleJdbcInsert(dataSource)
                        .withTableName("t_push_registration")
                        .usingGeneratedKeyColumns("pk_push_registration_id");
    }

    @Override
    @Transactional
    public void upsertPushRegistration(final PushRegistration pushRegistration) {
        if (Strings.isBlank(pushRegistration.getPushToken())) {
            deletePushRegistration(pushRegistration.getPushToken());
        }

        deletePushRegistration(pushRegistration.getPushToken());
        final var pushRegistrationParams = getPushRegistrationParams(pushRegistration);
        pushRegistrationInsert.execute(pushRegistrationParams);
    }

    private void deletePushRegistration(final String pushToken) {
        final var sql = "delete from t_push_registration where push_token = :push_token";
        jt.update(sql, new MapSqlParameterSource("push_token", pushToken));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PushRegistration> getPushRegistrationByType(final PushType pushType) {
        final String sql = "select * from t_push_registration where push_type = :push_type";
        final var params = new MapSqlParameterSource("push_type", pushType.name());
        return jt.query(sql, params, new PushRegistrationRowMapper());
    }

    private MapSqlParameterSource getPushRegistrationParams(PushRegistration pushRegistration) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("push_type", pushRegistration.getPushType().name());
        params.addValue("push_token", pushRegistration.getPushToken());
        return params;
    }

    @Override
    @Transactional(readOnly = false)
    public void removeRegistrations(List<String> tokensToRemove) {
        if (tokensToRemove != null && !tokensToRemove.isEmpty()) {
            jt.update(
                    "delete from t_push_registration where push_token in (:tokensToRemove)",
                    new MapSqlParameterSource("tokensToRemove", tokensToRemove));
        }
    }
}
