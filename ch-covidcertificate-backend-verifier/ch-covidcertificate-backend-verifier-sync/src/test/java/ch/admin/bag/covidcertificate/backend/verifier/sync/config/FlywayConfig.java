package ch.admin.bag.covidcertificate.backend.verifier.sync.config;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile("postgres")
@Configuration
public class FlywayConfig {

    @Bean
    public Flyway flyway(final DataSource dataSource) {
        final var flyway =
                Flyway.configure()
                        .dataSource(dataSource)
                        .locations("classpath:/db/migration/pgsql")
                        .validateOnMigrate(true)
                        .load();
        flyway.migrate();
        return flyway;
    }
}