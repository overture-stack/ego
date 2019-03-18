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

import static bio.overture.ego.model.enums.ApplicationType.ADMIN;
import static bio.overture.ego.model.enums.StatusType.APPROVED;
import static org.assertj.core.api.Assertions.assertThat;

import bio.overture.ego.model.dto.CreateApplicationRequest;
import bio.overture.ego.service.ApplicationService;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.Test;
import org.openqa.selenium.By;
import org.springframework.beans.factory.annotation.Autowired;

public class LoadAdminUITest extends AbstractSeleniumTest {

  /** Dependencies */
  @Autowired private ApplicationService applicationService;

  @Test
  @SneakyThrows
  public void loadAdmin_Success() {
    val facebookUser = System.getenv("FACEBOOK_USER");
    val facebookPass = System.getenv("FACEBOOK_PASS");

    val uiPort = uiContainer.getMappedPort(80);

    applicationService.create(
        CreateApplicationRequest.builder()
            .clientId("seleniumClient")
            .clientSecret("seleniumSecret")
            .name("Selenium Tests")
            .redirectUri("http://localhost:" + uiPort)
            .type(ADMIN)
            .status(APPROVED)
            .description("testing")
            .build());

    driver.get("http://localhost:" + uiPort);
    val titleText =
        driver.findElement(By.className("Login")).findElement(By.tagName("h1")).getText();
    assertThat(titleText).isEqualTo("Admin Portal");

    Thread.sleep(1000);

    driver.findElement(By.className("fa-facebook")).click();

    Thread.sleep(1000);

    val email = driver.findElement(By.id("email"));
    email.sendKeys(facebookUser);

    val pass = driver.findElement(By.id("pass"));
    pass.sendKeys(facebookPass);

    driver.findElement(By.id("loginbutton")).click();

    Thread.sleep(1000);

    val messageDiv =
        driver
            .findElement(By.id("root"))
            .findElement(By.tagName("div"))
            .findElement(By.tagName("div"))
            .getText();
    assertThat(messageDiv).contains("Your account does not have an administrator userType.");

    Thread.sleep(1000);
  }
}
