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

package bio.overture.ego.controller;

import bio.overture.ego.AuthorizationServiceMain;
import bio.overture.ego.model.entity.Application;
import bio.overture.ego.model.enums.ApplicationType;
import bio.overture.ego.service.ApplicationService;
import bio.overture.ego.service.UserService;
import bio.overture.ego.utils.EntityGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@SpringBootTest(
  classes = AuthorizationServiceMain.class,
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ApplicationControllerTest {

  /** Constants */
  private static final ObjectMapper MAPPER = new ObjectMapper();

  /** State */
  @LocalServerPort private int port;
  private TestRestTemplate restTemplate = new TestRestTemplate();
  private HttpHeaders headers = new HttpHeaders();
  private static boolean hasRunEntitySetup = false;

  /** Dependencies */
  @Autowired private EntityGenerator entityGenerator;
  @Autowired private ApplicationService applicationService;

  @Before
  public void setup() {
    // Initial setup of entities (run once
    if (!hasRunEntitySetup) {
      entityGenerator.setupTestUsers();
      entityGenerator.setupTestApplications();
      entityGenerator.setupTestGroups();
      hasRunEntitySetup = true;
    }

    headers.add("Authorization", "Bearer TestToken");
    headers.setContentType(MediaType.APPLICATION_JSON);
  }

  @Test
  @SneakyThrows
  public void addApplication_Success() {
    val app = Application.builder()
      .name("addApplication_Success")
      .clientId("addApplication_Success")
      .clientSecret("addApplication_Success")
      .redirectUri("http://example.com")
      .status("Approved")
      .applicationType(ApplicationType.CLIENT)
      .build();

    val entity = new HttpEntity<Application>(app, headers);
    val response =
      restTemplate.exchange(
        createURLWithPort("/applications"), HttpMethod.POST, entity, String.class);

    val responseStatus = response.getStatusCode();
    assertThat(responseStatus).isEqualTo(HttpStatus.OK);
    val responseJson = MAPPER.readTree(response.getBody());
    assertThat(responseJson.get("name").asText()).isEqualTo("addApplication_Success");
  }

  @Test
  @SneakyThrows
  public void addDuplicateApplication_Conflict() {
    val app1 = Application.builder()
      .name("addDuplicateApplication")
      .clientId("addDuplicateApplication")
      .clientSecret("addDuplicateApplication")
      .redirectUri("http://example.com")
      .status("Approved")
      .applicationType(ApplicationType.CLIENT)
      .build();

    val app2 = Application.builder()
      .name("addDuplicateApplication")
      .clientId("addDuplicateApplication")
      .clientSecret("addDuplicateApplication")
      .redirectUri("http://example.com")
      .status("Approved")
      .applicationType(ApplicationType.CLIENT)
      .build();

    val entity1 = new HttpEntity<Application>(app1, headers);
    val response1 =
      restTemplate.exchange(
        createURLWithPort("/applications"), HttpMethod.POST, entity1, String.class);

    val responseStatus1 = response1.getStatusCode();
    assertThat(responseStatus1).isEqualTo(HttpStatus.OK);

    val entity2 = new HttpEntity<Application>(app2, headers);
    val response2 =
      restTemplate.exchange(
        createURLWithPort("/applications"), HttpMethod.POST, entity2, String.class);
    val responseStatus2 = response2.getStatusCode();
    assertThat(responseStatus2).isEqualTo(HttpStatus.CONFLICT);
  }

  @Test
  @SneakyThrows
  public void getApplication_Success() {
    val applicationId = applicationService.getByClientId("111111").getId();
    val entity = new HttpEntity<String>(null, headers);
    val response =
      restTemplate.exchange(
        createURLWithPort(String.format("/applications/%s", applicationId)),
        HttpMethod.GET,
        entity,
        String.class);

    val responseStatus = response.getStatusCode();
    val responseJson = MAPPER.readTree(response.getBody());

    assertThat(responseStatus).isEqualTo(HttpStatus.OK);
    assertThat(responseJson.get("name").asText()).isEqualTo("Application 111111");
  }

  private String createURLWithPort(String uri) {
    return "http://localhost:" + port + uri;
  }
}
