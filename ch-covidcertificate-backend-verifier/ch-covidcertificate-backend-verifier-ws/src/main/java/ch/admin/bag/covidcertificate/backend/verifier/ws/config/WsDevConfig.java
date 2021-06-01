package ch.admin.bag.covidcertificate.backend.verifier.ws.config;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;

public class WsDevConfig extends
    WsBaseConfig {

  @Override
  public DataSource dataSource() {
    return null;
  }

  @Override
  public Flyway flyway() {
    return null;
  }

  @Override
  public String getDbType() {
    return null;
  }
}
