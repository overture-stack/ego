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

import org.overture.ego.model.entity.Group;
import org.overture.ego.repository.mapper.GroupsMapper;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.BindBean;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;

import java.util.List;

@RegisterMapper(GroupsMapper.class)
public interface GroupRepository {

  String selectQueryBase = "SELECT EGOGROUP.GRPID, EGOGROUP.GRPNAME, EGOGROUP.STATUS, " +
          "  EGOGROUP.DESCRIPTION, " +
          "  STRING_AGG(GROUPAPPLICATION.APPNAME, ',') AS Applications " +
          "  FROM EGOGROUP " +
          "  LEFT JOIN GROUPAPPLICATION on EGOGROUP.GRPNAME = GROUPAPPLICATION.GRPNAME";

  @SqlQuery(selectQueryBase + " GROUP BY EGOGROUP.GRPID")
  List<Group> getAllGroups();

  @SqlUpdate("INSERT INTO EGOGROUP (grpName, status, description) " +
          "VALUES (:name, :status, :description)")
  int create(@BindBean Group group);

  @SqlQuery(selectQueryBase + " WHERE GRPID=:id GROUP BY EGOGROUP.GRPID")
  Group read(@Bind("id") int grpId);

  @SqlQuery(selectQueryBase + " WHERE grpName=:name")
  Group getByName(@Bind("name") String grpName);

  @SqlUpdate("UPDATE EGOGROUP SET grpName=:name, status=:status," +
          "description=:description WHERE grpName=:grpName")
  int update(@BindBean Group group);

  @SqlUpdate("DELETE from EGOGROUP where GRPID=:id")
  int delete(@Bind("id") int grpId);

}
