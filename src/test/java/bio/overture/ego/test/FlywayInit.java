package bio.overture.ego.test;

import java.sql.Connection;
import java.sql.SQLException;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.junit.Test;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

@Slf4j
public class FlywayInit {

  public static void initTestContainers(Connection connection) throws SQLException {
    log.info("init test containers with flyway ******************************");

    Flyway flyway = new Flyway();
    flyway.setLocations("classpath:flyway/sql", "classpath:db/migration");
    flyway.setDataSource(new SingleConnectionDataSource(connection, true));
    flyway.migrate();
  }

}
