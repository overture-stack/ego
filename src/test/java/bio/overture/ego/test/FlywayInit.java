package bio.overture.ego.test;

import bio.overture.ego.utils.Streams;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterators;
import com.google.common.io.Resources;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.flywaydb.core.Flyway;
import org.openqa.selenium.json.Json;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import static bio.overture.ego.utils.Streams.stream;
import static com.google.common.base.Preconditions.checkArgument;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toUnmodifiableMap;

@Slf4j
public class FlywayInit {
  private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

  public static void initTestContainers(Connection connection) throws SQLException {
    log.info("init test containers with flyway ******************************");
    val appYaml =readApplicationYaml();
    val placeholders = parsePlaceholders(appYaml);

    Flyway.configure()
        .locations("classpath:flyway/sql", "classpath:db/migration")
        .dataSource(new SingleConnectionDataSource(connection, true))
        .placeholderReplacement(true)
        .placeholders(placeholders)
        .load()
        .migrate();
  }

  private static JsonNode checkAndGet(@NonNull JsonNode root, @NonNull String pathname){
    checkArgument(root.has(pathname), "Could not find pathname '{}' in json", pathname);
    return root.path(pathname);
  }

  private static Map<String, String> parsePlaceholders(JsonNode applicationProps){
    val springNode = checkAndGet(applicationProps, "spring");
    val flywayNode = checkAndGet(springNode, "flyway");
    val placeholdersNode= checkAndGet(flywayNode, "placeholders");
    return stream(placeholdersNode.fieldNames())
        .collect(toUnmodifiableMap(identity(), x -> placeholdersNode.path(x).asText()));
  }

  @SneakyThrows
  private static JsonNode readApplicationYaml(){
    return YAML_MAPPER.readTree(Resources.getResource("application.yml"));
  }
}
