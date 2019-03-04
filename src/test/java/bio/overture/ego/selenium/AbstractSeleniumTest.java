/*
 * Copyright (c) 2019. The Ontario Institute for Cancer Research. All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package bio.overture.ego.selenium;

import bio.overture.ego.AuthorizationServiceMain;
import bio.overture.ego.selenium.driver.WebDriverFactory;
import bio.overture.ego.selenium.rule.AssumingSeleniumEnvironment;
import bio.overture.ego.selenium.rule.SeleniumEnvironmentChecker;
import java.util.HashMap;
import java.util.Map;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.*;
import org.junit.runner.RunWith;
import org.openqa.selenium.WebDriver;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.testcontainers.containers.GenericContainer;

@Slf4j
@ActiveProfiles({"test", "secure", "auth"})
@RunWith(SpringRunner.class)
@SpringBootTest(
    classes = AuthorizationServiceMain.class,
    properties = {"server.port=19001"},
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@Ignore
public abstract class AbstractSeleniumTest {

  public int port = 19001;
  public static WebDriver driver;

  private static final WebDriverFactory FACTORY = new WebDriverFactory();

  @ClassRule
  public static AssumingSeleniumEnvironment seleniumEnvironment =
      new AssumingSeleniumEnvironment(new SeleniumEnvironmentChecker());

  @Rule public GenericContainer uiContainer = createGenericContainer();

  @BeforeClass
  public static void openBrowser() {
    driver = FACTORY.createDriver(seleniumEnvironment.getDriverType());
  }

  @AfterClass
  public static void tearDown() {
    if (driver != null) {
      driver.quit();
    }
  }

  @SneakyThrows
  private GenericContainer createGenericContainer() {
    return new GenericContainer("overture/ego-ui:260a87b-alpine")
        .withExposedPorts(80)
        .withEnv(createEnvMap());
  }

  private Map<String, String> createEnvMap() {
    val envs = new HashMap<String, String>();
    envs.put("REACT_APP_API", "http://localhost:" + port);
    envs.put("REACT_APP_EGO_CLIENT_ID", "seleniumClient");
    return envs;
  }
}
