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

package org.overture.ego.service;

import lombok.val;
import org.overture.ego.model.entity.Application;
import org.overture.ego.repository.ApplicationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;


@Service
public class ApplicationService {

  private final String APP_PREFIX = "";
  @Autowired
  ApplicationRepository applicationRepository;

  public Application create(Application applicationInfo) {
    applicationRepository.create(applicationInfo);
    return applicationRepository.getByName(applicationInfo.getApplicationName());
  }

  public Application get(String applicationId) {
    //TODO: change id to string
    int appID = Integer.parseInt(applicationId);
    return applicationRepository.read(appID);
  }

  public Application update(Application updatedApplicationInfo) {
    applicationRepository.update(updatedApplicationInfo.getId(), updatedApplicationInfo);
    return updatedApplicationInfo;
  }

  public void delete(String applicationId) {
    //TODO: change id to string
    int appID = Integer.parseInt(applicationId);
    applicationRepository.delete(appID);
  }

  public List<Application> listApps() {
    return applicationRepository.listApps();
  }
}
