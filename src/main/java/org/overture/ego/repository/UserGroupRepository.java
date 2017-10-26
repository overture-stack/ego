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
import org.overture.ego.model.entity.Group;
import org.overture.ego.model.entity.User;
import org.overture.ego.repository.mapper.GroupsMapper;
import org.overture.ego.repository.mapper.UserMapper;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.BindBean;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.Define;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.sqlobject.stringtemplate.UseStringTemplate3StatementLocator;

import java.util.List;

@UseStringTemplate3StatementLocator
public interface UserGroupRepository {

  String GET_ALL_USERS = "WITH allUsers AS ( " +
          "    SELECT COUNT(*) AS TOTAL from usergroup where usergroup.grpname=:grpName" +
          ") " +
          "SELECT egouser.*, allusers.total from egouser, allusers " +
          "where egouser.username " +
          "      IN " +
          "      (SELECT username from usergroup where usergroup.grpname=:grpName) " +
          "GROUP BY userid, allusers.total " +
          "ORDER BY lower(egouser.<sort>) <sortOrder> " +
          "LIMIT :limit OFFSET :offset";

  String GET_ALL_GROUPS = "WITH allGroups AS ( " +
          "    SELECT COUNT(*) AS TOTAL from usergroup where usergroup.username=:userName" +
          ") " +
          "SELECT egogroup.*, allGroups.total from egogroup, allGroups " +
          "where egogroup.grpname " +
          "      IN " +
          "      (SELECT grpname from usergroup where usergroup.username=:userName) " +
          "GROUP BY grpId, allGroups.total " +
          "ORDER BY lower(egogroup.<sort>) <sortOrder> " +
          "LIMIT :limit OFFSET :offset";

  @SqlUpdate("INSERT INTO USERGROUP (username, grpname) VALUES (:userName, :grpName)")
  int add(@Bind("userName")String userName, @Bind("grpName") String groupName);

  @SqlUpdate("DELETE from USERGROUP where grpname=:grpName AND username=:userName")
  void delete(@Bind("userName")String userName, @Bind("grpName") String groupName);

  @SqlQuery(GET_ALL_USERS)
  @RegisterMapper(UserMapper.class)
  List<User> getAllUsers(@BindBean QueryInfo queryInfo, @Bind("grpName") String groupName,
                         @Define("sort") String sort, @Define("sortOrder") String sortOrder);

  @SqlQuery(GET_ALL_GROUPS)
  @RegisterMapper(GroupsMapper.class)
  List<Group> getAllGroups(@BindBean QueryInfo queryInfo, @Bind("userName")String userName,
                           @Define("sort") String sort, @Define("sortOrder") String sortOrder);

}
