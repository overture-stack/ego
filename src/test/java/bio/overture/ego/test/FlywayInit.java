package bio.overture.ego.test;

import static bio.overture.ego.utils.Streams.stream;
import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.isNull;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toUnmodifiableMap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.io.Resources;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.flywaydb.core.Flyway;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import static org.junit.Assert.*;

@Slf4j
public class FlywayInit {
  private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

  // retrieving placeholders with yaml_mapper because this implementation of Flyway does not allow
  // access to placeholders
  // Other options found look hackier:
  // https://github.com/flyway/flyway/issues/1062#issuecomment-357265175
  // https://blog.8bitzen.com/posts/using-flyway-java-migrations-with-spring-boot/
  // **intending to remove this file in https://github.com/overture-stack/ego/issues/527**
  public static void initTestContainers(Connection connection) throws SQLException {
    log.info("init test containers with flyway ******************************");
    val appYaml = readApplicationYaml();
    val placeholders = parsePlaceholders(appYaml);

    assertTrue(placeholders.size() > 0);
    assertTrue(placeholders.containsKey("default_provider"));

    Flyway.configure()
        .locations("classpath:flyway/sql", "classpath:db/migration")
        .dataSource(new SingleConnectionDataSource(connection, true))
        .placeholderReplacement(true)
        .placeholders(placeholders)
        .load()
        .migrate();
  }

  private static JsonNode checkAndGet(@NonNull JsonNode root, @NonNull String pathname) {
    checkArgument(root.has(pathname), "Could not find pathname '{}' in json", pathname);
    return root.path(pathname);
  }

  private static Map<String, String> parsePlaceholders(JsonNode applicationProps) {
    val springNode = checkAndGet(applicationProps, "spring");
    val flywayNode = checkAndGet(springNode, "flyway");
    val placeholdersNode = checkAndGet(flywayNode, "placeholders");
    checkArgument(
        !isNull(placeholdersNode),
        "Placeholders node cannot be null. Check spring.flyway.placeholders configuration in app.properties.");
    return stream(placeholdersNode.fieldNames())
        .collect(toUnmodifiableMap(identity(), x -> placeholdersNode.path(x).asText()));
  }

  @SneakyThrows
  private static JsonNode readApplicationYaml() {
    return YAML_MAPPER.readTree(Resources.getResource("application.yml"));
  }
}
