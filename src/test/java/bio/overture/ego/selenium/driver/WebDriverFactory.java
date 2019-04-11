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

package bio.overture.ego.selenium.driver;

import com.browserstack.local.Local;
import java.io.FileReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.remote.DesiredCapabilities;

@Slf4j
public class WebDriverFactory {

  private static final int TIMEOUT_SECONDS = 15;
  private static final int PAGELOAD_TIMEOUT = 30;

  public WebDriver createDriver(DriverType type) {
    switch (type) {
      case LOCAL:
        return createChromeDriver();
      case BROWSERSTACK:
        return createBrowserStackDriver();
      default:
        throw new IllegalStateException("How did you get here?");
    }
  }

  private WebDriver createChromeDriver() {
    val chromeDriverPath = System.getenv("CHROME_DRIVER_PATH");
    if (chromeDriverPath == null)
      throw new RuntimeException("Please set the CHROME_DRIVER_PATH environment variable");
    System.setProperty("webdriver.chrome.driver", chromeDriverPath);
    val driver = new ChromeDriver();
    driver
        .manage()
        .timeouts()
        .implicitlyWait(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .pageLoadTimeout(PAGELOAD_TIMEOUT, TimeUnit.SECONDS);
    return driver;
  }

  @SneakyThrows
  private WebDriver createBrowserStackDriver() {
    val parser = new JSONParser(JSONParser.MODE_PERMISSIVE);
    val config = (JSONObject) parser.parse(new FileReader("src/test/resources/conf/bs.conf.json"));

    val envs = (JSONArray) config.get("environments");
    val capabilities = new DesiredCapabilities();

    // TODO: Allow for many environments.
    val envCapabilities = (Map<String, String>) envs.get(0);
    val it1 = envCapabilities.entrySet().iterator();
    while (it1.hasNext()) {
      val pair = it1.next();
      capabilities.setCapability(pair.getKey(), pair.getValue());
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
    String accessKey = System.getenv("BROWSERSTACK_ACCESS_KEY");

    val options = new HashMap<String, String>();
    options.put("key", accessKey);
    val local = new Local();
    local.start(options);

    log.info(capabilities.toString());

    val driver =
        new BrowserStackDriverProxy(
            new URL(
                "http://" + username + ":" + accessKey + "@" + config.get("server") + "/wd/hub"),
            capabilities,
            local);

    driver
        .manage()
        .timeouts()
        .implicitlyWait(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .pageLoadTimeout(PAGELOAD_TIMEOUT, TimeUnit.SECONDS);

    return driver;
  }

  public enum DriverType {
    LOCAL,
    BROWSERSTACK
  }
}
