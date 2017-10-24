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

package org.overture.ego.repository;


import org.overture.ego.model.PageInfo;
import org.overture.ego.model.entity.Application;
import org.overture.ego.model.entity.Group;
import org.overture.ego.model.entity.User;
import org.overture.ego.repository.mapper.ApplicationMapper;
import org.overture.ego.repository.mapper.GroupsMapper;
import org.overture.ego.repository.mapper.UserMapper;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.BindBean;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;

import java.util.List;

public interface UserAppRepository {

  String GET_ALL_USERS = "WITH allUsers AS ( " +
          "    SELECT COUNT(*) AS TOTAL from userapplication where userapplication.appName=:appName" +
          ") " +
          "SELECT egouser.*, allusers.total from egouser, allusers " +
          "where egouser.username " +
          "      IN " +
          "      (SELECT username from userapplication where userapplication.appName=:appName) " +
          "GROUP BY userid, allusers.total " +
          "ORDER BY EGOUSER.USERID DESC " +
          "LIMIT :limit OFFSET :offset";

  String GET_ALL_APPS = "WITH allApps AS ( " +
          "    SELECT COUNT(*) AS TOTAL from userapplication where userapplication.userName=:userName" +
          ") " +
          "SELECT egoapplication.*, allApps.total from egoapplication, allApps " +
          "where egoapplication.appName " +
          "      IN " +
          "      (SELECT appName from userapplication where userapplication.userName=:userName) " +
          "GROUP BY egoapplication.appid, allApps.total " +
          "ORDER BY egoapplication.appid DESC " +
          "LIMIT :limit OFFSET :offset";

  @SqlUpdate("INSERT INTO USERAPPLICATION (username, appName) VALUES (:userName, :appName)")
  int add(@Bind("userName") String userName, @Bind("appName") String appName);

  @SqlUpdate("DELETE from USERAPPLICATION where userName=:userName AND appName=:appName")
  void delete(@Bind("userName") String userName, @Bind("appName") String appName);

  @SqlQuery(GET_ALL_USERS)
  @RegisterMapper(UserMapper.class)
  List<User> getAllUsers(@BindBean PageInfo pageInfo, @Bind("appName") String appName);

  @SqlQuery(GET_ALL_APPS)
  @RegisterMapper(ApplicationMapper.class)
  List<Application> getAllApps(@BindBean PageInfo pageInfo, @Bind("userName") String userName);

}
