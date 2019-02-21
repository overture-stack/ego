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

package bio.overture.ego.browser;

import bio.overture.ego.AuthorizationServiceMain;
import com.browserstack.local.Local;
import java.io.FileReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@Slf4j
@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@SpringBootTest(
    classes = AuthorizationServiceMain.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class BrowserStackJUnitTest {
  /** State */
  @LocalServerPort public int port;

  @Parameterized.Parameter(value = 0)
  public int taskID;

  private static JSONObject config;
  public WebDriver driver;
  private Local l;

  @Parameterized.Parameters
  public static Iterable<?> data() throws Exception {
    val parser = new JSONParser(JSONParser.MODE_PERMISSIVE);
    config = (JSONObject) parser.parse(new FileReader("src/test/resources/conf/bs.conf.json"));
    int envs = ((JSONArray) config.get("environments")).size();

    val taskIDs = new ArrayList<Integer>();
    for (int i = 0; i < envs; i++) {
      taskIDs.add(i);
    }

    return taskIDs;
  }

  @Before
  @SneakyThrows
  public void setUp() {
    val parser = new JSONParser(JSONParser.MODE_PERMISSIVE);
    config = (JSONObject) parser.parse(new FileReader("src/test/resources/conf/bs.conf.json"));

    val envs = (JSONArray) config.get("environments");
    val capabilities = new DesiredCapabilities();

    val envCapabilities = (Map<String, String>) envs.get(taskID);
    val it1 = envCapabilities.entrySet().iterator();
    while (it1.hasNext()) {
      val pair = it1.next();
      capabilities.setCapability(pair.getKey().toString(), pair.getValue().toString());
    }

    Map<String, String> commonCapabilities = (Map<String, String>) config.get("capabilities");
    val it2 = commonCapabilities.entrySet().iterator();
    while (it2.hasNext()) {
      Map.Entry pair = it2.next();
      if (capabilities.getCapability(pair.getKey().toString()) == null) {
        capabilities.setCapability(pair.getKey().toString(), pair.getValue().toString());
      }
    }

    String username = System.getenv("BROWSERSTACK_USERNAME");
    if (username == null) {
      username = (String) config.get("user");
    }

    String accessKey = System.getenv("BROWSERSTACK_ACCESS_KEY");
    if (accessKey == null) {
      accessKey = (String) config.get("key");
    }

    String app = System.getenv("BROWSERSTACK_APP_ID");
    if (app != null && !app.isEmpty()) {
      capabilities.setCapability("app", app);
    }

    if (capabilities.getCapability("browserstack.local") != null
        && capabilities.getCapability("browserstack.local") == "true") {
      l = new Local();
      val options = new HashMap<String, String>();
      options.put("key", accessKey);
      l.start(options);
    }

    driver =
        new RemoteWebDriver(
            new URL(
                "http://" + username + ":" + accessKey + "@" + config.get("server") + "/wd/hub"),
            capabilities);
  }

  @After
  public void tearDown() throws Exception {
    driver.quit();
    if (l != null) l.stop();
  }
}
