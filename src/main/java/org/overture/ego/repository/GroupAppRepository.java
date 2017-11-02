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


import org.overture.ego.model.QueryInfo;
import org.overture.ego.model.entity.Application;
import org.overture.ego.model.entity.Group;
import org.overture.ego.repository.mapper.ApplicationMapper;
import org.overture.ego.repository.mapper.GroupsMapper;
import org.overture.ego.repository.sql.ApplicationQueries;
import org.overture.ego.repository.sql.GroupQueries;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.BindBean;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.Define;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.sqlobject.stringtemplate.UseStringTemplate3StatementLocator;

import java.util.List;

@UseStringTemplate3StatementLocator
public interface GroupAppRepository {

  String GET_ALL_APPS = "WITH allApps AS ( " +
          "    SELECT COUNT(*) AS TOTAL from groupapplication where groupapplication.grpname=:grpName" +
          ") " +
          "SELECT egoapplication.*, allApps.total from egoapplication, allApps " +
          "where egoapplication.appName " +
          "      IN " +
          "      (SELECT appName from groupapplication where groupapplication.grpname=:grpName) " +
          "GROUP BY egoapplication.appid, allApps.total " +
          "ORDER BY lower(egoapplication.<sort>) <sortOrder> " +
          "LIMIT :limit OFFSET :offset";

  String GET_ALL_GROUPS = "WITH allGroups AS ( " +
          "    SELECT COUNT(*) AS TOTAL from groupapplication where groupapplication.appName=:appName" +
          ") " +
          "SELECT egogroup.*, allGroups.total from egogroup, allGroups " +
          "where egogroup.grpname " +
          "      IN " +
          "      (SELECT grpname from groupapplication where groupapplication.appName=:appName) " +
          "GROUP BY egogroup.grpId, allGroups.total " +
          "ORDER BY lower(egogroup.<sort>) <sortOrder> " +
          "LIMIT :limit OFFSET :offset";
  
  @SqlUpdate("INSERT INTO GROUPAPPLICATION (appName, grpname) VALUES (:appName, :grpName)")
  int add(@Bind("appName") String appName,@Bind("grpName") String groupName);

  @SqlUpdate("DELETE from GROUPAPPLICATION where grpname=:grpName AND appName=:appName")
  void delete(@Bind("appName") String appName, @Bind("grpName") String groupName);

  @SqlQuery(GET_ALL_APPS)
  @RegisterMapper(ApplicationMapper.class)
  List<Application> getAllApps(@BindBean QueryInfo queryInfo, @Bind("grpName") String groupName,
                               @Define("sort") String sort, @Define("sortOrder") String sortOrder);

  @SqlQuery(ApplicationQueries.FIND_ALL_APPS_OF_GROUP)
  @RegisterMapper(ApplicationMapper.class)
  List<Application> findGroupsApps(@BindBean QueryInfo queryInfo, @Bind("grpName") String groupName,
                                   @Define("sort") String sort, @Define("sortOrder") String sortOrder,
                                   @Bind("query")String query);

  @SqlQuery(GET_ALL_GROUPS)
  @RegisterMapper(GroupsMapper.class)
  List<Group> getAllGroups(@BindBean QueryInfo queryInfo, @Bind("appName")String appName,
                           @Define("sort") String sort, @Define("sortOrder") String sortOrder);

  @SqlQuery(GroupQueries.FIND_ALL_GROUPS_OF_APP)
  @RegisterMapper(GroupsMapper.class)
  List<Group> findGroupsApp(@BindBean QueryInfo queryInfo, @Bind("appName")String appName,
                            @Define("sort") String sort, @Define("sortOrder") String sortOrder,
                            @Bind("query")String query);

}
