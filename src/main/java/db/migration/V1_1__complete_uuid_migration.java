package db.migration;

import bio.overture.ego.model.entity.Application;
import bio.overture.ego.model.entity.Group;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.flywaydb.core.api.migration.spring.SpringJdbcMigration;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@Slf4j
public class V1_1__complete_uuid_migration implements SpringJdbcMigration {
  public void migrate(JdbcTemplate jdbcTemplate) throws Exception {
    log.info(
        "Flyway java migration: V1_1__complete_uuid_migration running ******************************");

    // Development tests for migration left in for future use potentially
    // This whole class can be refactored into a SQL based migration
    boolean runWithTest = false;
    UUID userOneId = UUID.randomUUID();
    UUID userTwoId = UUID.randomUUID();

    // Test data (if set to true)
    if (runWithTest) {
      createTestData(jdbcTemplate, userOneId, userTwoId);
    }

    jdbcTemplate.execute("CREATE EXTENSION \"uuid-ossp\"");

    // Add temporary UUID column to applications and groups
    jdbcTemplate.execute("ALTER TABLE EGOAPPLICATION ADD uuid UUID DEFAULT uuid_generate_v4()");
    jdbcTemplate.execute("ALTER TABLE EGOGROUP ADD uuid UUID DEFAULT uuid_generate_v4()");

    // Add temporary UUID column for mapping tables or tables with fk relationships
    jdbcTemplate.execute("ALTER TABLE GROUPAPPLICATION ADD grpUuid UUID");
    jdbcTemplate.execute("ALTER TABLE GROUPAPPLICATION ADD appUuid UUID");
    jdbcTemplate.execute("ALTER TABLE USERGROUP ADD grpUuid UUID");
    jdbcTemplate.execute("ALTER TABLE USERAPPLICATION ADD appUuid UUID");

    // Drop fk contrainsts
    jdbcTemplate.execute(
        "ALTER TABLE GROUPAPPLICATION DROP CONSTRAINT groupapplication_grpid_fkey");
    jdbcTemplate.execute(
        "ALTER TABLE GROUPAPPLICATION DROP CONSTRAINT groupapplication_appid_fkey");
    jdbcTemplate.execute("ALTER TABLE USERGROUP DROP CONSTRAINT usergroup_grpid_fkey");
    jdbcTemplate.execute("ALTER TABLE USERAPPLICATION DROP CONSTRAINT userapplication_appid_fkey");

    // Update fk mapping columns for applications and groups
    jdbcTemplate.execute(
        "UPDATE GROUPAPPLICATION SET grpUuid = EGOGROUP.uuid FROM EGOGROUP WHERE EGOGROUP.id = GROUPAPPLICATION.grpId");
    jdbcTemplate.execute(
        "UPDATE GROUPAPPLICATION SET appUuid = EGOAPPLICATION.uuid FROM EGOAPPLICATION WHERE EGOAPPLICATION.id = GROUPAPPLICATION.appId");
    jdbcTemplate.execute(
        "UPDATE USERGROUP SET grpUuid = EGOGROUP.uuid FROM EGOGROUP WHERE EGOGROUP.id = USERGROUP.grpId");
    jdbcTemplate.execute(
        "UPDATE USERAPPLICATION SET appUuid = EGOAPPLICATION.uuid FROM EGOAPPLICATION WHERE EGOAPPLICATION.id = USERAPPLICATION.appId");

    // Clean up temporary columns for EGOAPPLICATION and re-add PK contraints
    jdbcTemplate.execute("ALTER TABLE EGOAPPLICATION DROP CONSTRAINT EGOAPPLICATION_pkey");
    jdbcTemplate.execute("ALTER TABLE EGOAPPLICATION DROP COLUMN id");
    jdbcTemplate.execute("ALTER TABLE EGOAPPLICATION RENAME COLUMN uuid TO id");
    jdbcTemplate.execute("ALTER TABLE EGOAPPLICATION ADD PRIMARY KEY (id)");

    // Clean up temporary columns for EGOGROUP and re-add PK contraints
    jdbcTemplate.execute("ALTER TABLE EGOGROUP DROP CONSTRAINT EGOGROUP_pkey");
    jdbcTemplate.execute("ALTER TABLE EGOGROUP DROP COLUMN id");
    jdbcTemplate.execute("ALTER TABLE EGOGROUP RENAME COLUMN uuid TO id");
    jdbcTemplate.execute("ALTER TABLE EGOGROUP ADD PRIMARY KEY (id)");

    // Clean up temporary columns for GROUPAPPLICATION and re-add FK contraints
    jdbcTemplate.execute("ALTER TABLE GROUPAPPLICATION DROP COLUMN grpId");
    jdbcTemplate.execute("ALTER TABLE GROUPAPPLICATION DROP COLUMN appId");
    jdbcTemplate.execute("ALTER TABLE USERGROUP DROP COLUMN grpId");
    jdbcTemplate.execute("ALTER TABLE USERAPPLICATION DROP COLUMN appId");

    jdbcTemplate.execute("ALTER TABLE GROUPAPPLICATION RENAME COLUMN grpUuid TO grpId");
    jdbcTemplate.execute("ALTER TABLE GROUPAPPLICATION RENAME COLUMN appUuid TO appId");
    jdbcTemplate.execute("ALTER TABLE USERGROUP RENAME COLUMN grpUuid TO grpId");
    jdbcTemplate.execute("ALTER TABLE USERAPPLICATION RENAME COLUMN appUuid TO appId");

    jdbcTemplate.execute(
        "ALTER TABLE GROUPAPPLICATION ADD FOREIGN KEY (grpId) REFERENCES EGOGROUP (id)");
    jdbcTemplate.execute(
        "ALTER TABLE GROUPAPPLICATION ADD FOREIGN KEY (appId) REFERENCES EGOAPPLICATION (id)");
    jdbcTemplate.execute("ALTER TABLE USERGROUP ADD FOREIGN KEY (grpId) REFERENCES EGOGROUP (id)");
    jdbcTemplate.execute(
        "ALTER TABLE USERAPPLICATION ADD FOREIGN KEY (appId) REFERENCES EGOAPPLICATION (id)");

    // Test queries to ensure all is good (if flag set to true)
    if (runWithTest) {
      testUuidMigration(jdbcTemplate, userOneId, userTwoId);
    }

    log.info(
        "****************************** Flyway java migration: V1_1__complete_uuid_migration complete");
  }

  private void createTestData(JdbcTemplate jdbcTemplate, UUID userOneId, UUID userTwoId) {
    jdbcTemplate.update(
        "INSERT INTO EGOUSER (id, name, email, status) VALUES (?, 'userOne', 'userOne@email.com', 'Pending')",
        userOneId);
    jdbcTemplate.update(
        "INSERT INTO EGOUSER (id, name, email, status) VALUES (?, 'userTwo', 'userTwo@email.com', 'Pending')",
        userTwoId);

    jdbcTemplate.execute(
        "INSERT INTO EGOAPPLICATION (id, name, clientid, clientsecret, status) VALUES (1, 'appOne', '123', '321', 'Pending')");
    jdbcTemplate.execute(
        "INSERT INTO EGOAPPLICATION (id, name, clientid, clientsecret, status) VALUES (2, 'appTwo', '456', '654', 'Pending')");
    jdbcTemplate.execute(
        "INSERT INTO EGOAPPLICATION (id, name, clientid, clientsecret, status) VALUES (3, 'appThree', '789', '987', 'Pending')");

    jdbcTemplate.execute(
        "INSERT INTO EGOGROUP (id, name, status) VALUES (1, 'groupOne', 'Pending')");
    jdbcTemplate.execute(
        "INSERT INTO EGOGROUP (id, name, status) VALUES (2, 'groupTwo', 'Pending')");

    jdbcTemplate.update("INSERT INTO USERGROUP (userid, grpid) VALUES (?, 1)", userOneId);
    jdbcTemplate.update("INSERT INTO USERGROUP (userid, grpid) VALUES (?, 2)", userTwoId);

    jdbcTemplate.update("INSERT INTO USERAPPLICATION (userid, appid) VALUES (?, 1)", userOneId);
    jdbcTemplate.update("INSERT INTO USERAPPLICATION (userid, appid) VALUES (?, 2)", userTwoId);

    jdbcTemplate.execute("INSERT INTO GROUPAPPLICATION (grpid, appid) VALUES (1, 1)");
    jdbcTemplate.execute("INSERT INTO GROUPAPPLICATION (grpid, appid) VALUES (1, 2)");
    jdbcTemplate.execute("INSERT INTO GROUPAPPLICATION (grpid, appid) VALUES (2, 3)");
  }

  private void testUuidMigration(JdbcTemplate jdbcTemplate, UUID userOneId, UUID userTwoId) {
    val egoGroups =
        jdbcTemplate.query("SELECT * FROM EGOGROUP", new BeanPropertyRowMapper(Group.class));
    val egoApplications =
        jdbcTemplate.query(
            "SELECT * FROM EGOAPPLICATION", new BeanPropertyRowMapper(Application.class));
    val userGroups = jdbcTemplate.queryForList("SELECT * FROM USERGROUP");
    val userApplications = jdbcTemplate.queryForList("SELECT * FROM USERAPPLICATION");
    val groupApplications = jdbcTemplate.queryForList("SELECT * FROM GROUPAPPLICATION");

    val groupOneId = ((Group) egoGroups.get(0)).getId();
    val groupTwoId = ((Group) egoGroups.get(1)).getId();

    val appOneId = ((Application) egoApplications.get(0)).getId();
    val appTwoId = ((Application) egoApplications.get(1)).getId();
    val appThreeId = ((Application) egoApplications.get(2)).getId();

    assertThat(groupApplications.get(0).get("grpId").toString(), is(groupOneId.toString()));
    assertThat(groupApplications.get(0).get("appId").toString(), is(appOneId.toString()));

    assertThat(groupApplications.get(1).get("grpId").toString(), is(groupOneId.toString()));
    assertThat(groupApplications.get(1).get("appId").toString(), is(appTwoId.toString()));

    assertThat(groupApplications.get(2).get("grpId").toString(), is(groupTwoId.toString()));
    assertThat(groupApplications.get(2).get("appId").toString(), is(appThreeId.toString()));

    assertThat(userGroups.get(0).get("userid").toString(), is(userOneId.toString()));
    assertThat(userGroups.get(1).get("userid").toString(), is(userTwoId.toString()));

    assertThat(userApplications.get(0).get("userid").toString(), is(userOneId.toString()));
    assertThat(userApplications.get(1).get("userid").toString(), is(userTwoId.toString()));
  }
}
