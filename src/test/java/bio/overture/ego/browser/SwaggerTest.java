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

import lombok.SneakyThrows;
import lombok.val;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;

public class SwaggerTest extends BrowserStackJUnitTest {

  @Test
  @SneakyThrows
  public void test() {
    driver.get("http://localhost:" + this.port + "/swagger-ui.html");
    Thread.sleep(5000);
    val titleText = driver.findElement(By.className("info_title")).getText();
    Assertions.assertThat(titleText).isEqualTo("ego Service API");
  }
}
