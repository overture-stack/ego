package bio.overture.ego.swagger;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.file.Files.write;
import static java.util.Collections.singletonList;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import lombok.val;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.IfProfileValue;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.context.WebApplicationContext;

/** Based on: https://github.com/springfox/springfox/issues/1959 */
@RunWith(SpringRunner.class)
@IfProfileValue(name = "profile", value = "GenerateSwagger")
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(
    properties = {
      /**
       * Without this, PageDTO<<X>> is used, and the < and > are represented as non-ascii characters
       * in swagger.json, which cant be processed during java code generation. By enabling this
       * switch in the Docket, PageDTO<<X>> is represented as PageDTOOfX in the swagger.json making
       * it processable.
       */
      "swagger.enableCodeGeneration=true",
    })
public class SwaggerGenerator {

  @Autowired private WebApplicationContext context;

  /**
   * This test is intentionally skipped. You can use it to generate a swagger.json file using the
   * following command: mvn -Dtest=GenerateSwagger -Dtest-profile=GenerateSwagger test
   */
  @Test
  public void generateSwagger() throws Exception {
    val mockMvc = webAppContextSetup(context).build();
    mockMvc
        .perform(MockMvcRequestBuilders.get("/v2/api-docs").accept(APPLICATION_JSON))
        .andDo(this::saveToFile);
  }

  private Path saveToFile(MvcResult r) throws IOException {
    return write(
        Paths.get("./target/generated-test-sources/swagger.json"),
        singletonList(r.getResponse().getContentAsString()),
        US_ASCII);
  }
}
