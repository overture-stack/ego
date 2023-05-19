/*
 * Copyright (c) 2017. The Ontario Institute for Cancer Research. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package bio.overture.ego.controller;

import static org.springframework.http.HttpStatus.*;
import static org.springframework.web.bind.annotation.RequestMethod.*;

import bio.overture.ego.model.entity.VisaPermission;
import bio.overture.ego.service.PassportService;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/passport")
@Tag(name = "Passport", description = "poassport-controller")
public class PassportController {

  private final PassportService passportService;

  @Autowired
  public PassportController(@NonNull PassportService passportService) {
    this.passportService = passportService;
  }

  @RequestMapping(method = POST, value = "/passport/token")
  @ResponseStatus(value = OK)
  @SneakyThrows
  public @ResponseBody List<VisaPermission> getVisaPermissions(@RequestBody String authToken) {
    return passportService.getPermissions(authToken);
  }
}
