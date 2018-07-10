package org.overture.ego.test;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import java.sql.Connection;
import java.sql.SQLException;

@Slf4j
public class FlywayInit {

  public static void initTestContainers(Connection connection) throws SQLException {
    log.info("init test containers with flyway ******************************");

    Flyway flyway = new Flyway();
    flyway.setLocations("classpath:flyway/sql");
    flyway.setDataSource(new SingleConnectionDataSource(connection, true));
    flyway.migrate();
  }

}
