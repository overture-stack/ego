package db.migration;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.flywaydb.core.api.migration.spring.SpringJdbcMigration;
import bio.overture.ego.model.entity.User;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Date;
import java.util.UUID;

import static org.junit.Assert.assertTrue;

@Slf4j
public class V1_3__string_to_date implements SpringJdbcMigration {

    @Override
    public void migrate(JdbcTemplate jdbcTemplate) throws Exception {
        log.info("Flyway java migration: V1_3__string_to_date running ******************************");

        boolean runWithTest = false;
        UUID userOneId = UUID.randomUUID();
        UUID userTwoId = UUID.randomUUID();

        if (runWithTest) {
            createTestData(jdbcTemplate, userOneId, userTwoId);
        }

        jdbcTemplate.execute("ALTER TABLE EGOUSER ALTER CREATEDAT DROP DEFAULT, ALTER CREATEDAT TYPE TIMESTAMP WITHOUT TIME ZONE USING DATE(CREATEDAT);");

        jdbcTemplate.execute("ALTER TABLE EGOUSER ALTER LASTLOGIN DROP DEFAULT, ALTER LASTLOGIN TYPE TIMESTAMP WITHOUT TIME ZONE USING DATE(LASTLOGIN);");

        if (runWithTest) {
            testDateType(jdbcTemplate);
        }

        log.info("****************************** Flyway java migration: V1_3__string_to_date complete");
    }

    private void createTestData(JdbcTemplate jdbcTemplate, UUID userOneId, UUID userTwoId) {
        jdbcTemplate.update("INSERT INTO EGOUSER (id, name, email, role, status, createdAt, lastlogin) " +
                                "VALUES (?, 'userOne', 'userOne@email.com', 'user', 'Pending', '2017-01-15 04:35:55', '2016-12-15 23:20:51')", userOneId);

        jdbcTemplate.update("INSERT INTO EGOUSER (id, name, email, role, status, createdAt, lastlogin) " +
                                "VALUES (?, 'userTwo', 'userTwo@email.com', 'user', 'Pending', '2017-04-05 05:05:50', '2017-06-16 02:44:19')", userTwoId);
    }

    private void testDateType(JdbcTemplate jdbcTemplate) {
        val egoUsers = jdbcTemplate.query("SELECT * FROM EGOUSER", new BeanPropertyRowMapper(User.class));

        val createdAtOne = ((User) egoUsers.get(0)).getCreatedAt();
        val createdAtTwo = ((User) egoUsers.get(1)).getCreatedAt();

        val lastloginOne = ((User) egoUsers.get(0)).getLastLogin();
        val lastloginTwo = ((User) egoUsers.get(1)).getLastLogin();

        assertTrue(createdAtOne instanceof Date);
        assertTrue(createdAtTwo instanceof Date);
        assertTrue(lastloginOne instanceof Date);
        assertTrue(lastloginTwo instanceof Date);

    }
}
