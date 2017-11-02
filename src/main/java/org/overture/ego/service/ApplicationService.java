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

import org.overture.ego.model.Page;
import org.overture.ego.model.QueryInfo;
import org.overture.ego.model.entity.Application;
import org.overture.ego.repository.ApplicationRepository;
import org.overture.ego.repository.mapper.ApplicationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.BiFunction;


@Service
public class ApplicationService {

  private final String APP_PREFIX = "";
  @Autowired
  ApplicationRepository applicationRepository;

  public Application create(Application applicationInfo) {
    applicationRepository.create(applicationInfo);
    return applicationRepository.getByName(applicationInfo.getName());
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

  public Page<Application> listApps(QueryInfo queryInfo) {
    return getAppsPage((sort, sortOrder) -> applicationRepository.listApps(queryInfo, sort,sortOrder),queryInfo);
  }

  public Page<Application> findApps(QueryInfo queryInfo, String query) {
    return getAppsPage((sort, sortOrder) ->
            applicationRepository.findApps(queryInfo, sort,sortOrder, "%"+query+"%"),queryInfo);
  }

  public Page<Application> getAppsPage(BiFunction<String, String, List<Application>> appPageFetcher,
                                 QueryInfo queryInfo)  {
    // Using string templates with JDBI opens up the room for SQL Injection
    // Field sanitation is must to avoid it
    return getAppsPage(queryInfo,
                    appPageFetcher.apply(queryInfo.getSort(ApplicationMapper::sanitizeSortField),
                                         queryInfo.getSortOrder()));
  }

  public Application getByName(String appName) {

     return applicationRepository.getByName(appName);
    }

  private Page<Application> getAppsPage(QueryInfo queryInfo, List<Application> apps){
    return Page.getPageFromPageInfo(queryInfo,apps);
  }

}
