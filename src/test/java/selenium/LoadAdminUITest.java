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

package selenium;

import static bio.overture.ego.model.enums.ApplicationType.ADMIN;
import static bio.overture.ego.model.enums.StatusType.APPROVED;
import static org.junit.Assert.*;

import bio.overture.ego.model.dto.CreateApplicationRequest;
import bio.overture.ego.service.ApplicationService;
import java.util.Calendar;
import java.util.UUID;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.Test;
import org.openqa.selenium.By;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
public class LoadAdminUITest extends AbstractSeleniumTest {

  /** Dependencies */
  @Autowired private ApplicationService applicationService;

  // 15min * 60s * 1000ns - 900,000ms
  @Test(timeout = 900000L)
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
    assertEquals(titleText, "Admin Portal");

    driver.findElement(By.className("fa-facebook")).click();

    val email = driver.findElement(By.id("email"));
    email.sendKeys(facebookUser);

    val pass = driver.findElement(By.id("pass"));
    pass.sendKeys(facebookPass);

    driver.findElement(By.id("loginbutton")).click();

    Thread.sleep(5000);

    val messageDiv =
        driver
            .findElement(By.id("root"))
            .findElement(By.tagName("div"))
            .findElement(By.tagName("div"))
            .getText();
    assertTrue(messageDiv.contains("Your account does not have an administrator userType."));

    val refreshCookie = driver.manage().getCookieNamed("refreshId");
    assertNotNull(refreshCookie);
    assertNotNull(refreshCookie.getValue());
    assertSame(UUID.fromString(refreshCookie.getValue()).getClass(), UUID.class);
    assertTrue(refreshCookie.isHttpOnly());

    val exp = refreshCookie.getExpiry();
    val millis = exp.getTime() - Calendar.getInstance().getTime().getTime();
    assertTrue(millis > 0);

    Thread.sleep(5000);
    log.info("Closing driver....");
    driver.close();
    log.info("----done");
    //driver.quit();
  }
}
