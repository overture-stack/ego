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

package org.overture.ego.repository.sql;

public class GroupQueries {

  private static  final String SELECT_COMMON = "SELECT EGOGROUP.GRPID, EGOGROUP.GRPNAME, EGOGROUP.STATUS, " +
          "  EGOGROUP.DESCRIPTION, " +
          "  STRING_AGG(GROUPAPPLICATION.APPNAME, ',') AS Applications ";
  private static final String TABLE_NAME = "  FROM EGOGROUP ";
  private static final String SELECT_SUFFIX =
          "  LEFT JOIN GROUPAPPLICATION on EGOGROUP.GRPNAME = GROUPAPPLICATION.GRPNAME";
  private static final String SELECT_SEARCH_STRING =
                  " lower(EGOGROUP.GRPNAME) LIKE :query ";

  private static final String SELECT_PAGING_SUFFIX =
          " GROUP BY EGOGROUP.GRPID " +
          " ORDER BY lower(EGOGROUP.<sort>) <sortOrder>  " +
          " LIMIT :limit OFFSET :offset";
  /*
    Query strings related to user
   */
  public static final String GET_ALL =
          SELECT_COMMON + " , (SELECT COUNT(*) AS TOTAL "+TABLE_NAME+") " +
                  TABLE_NAME + SELECT_SUFFIX +
                  SELECT_PAGING_SUFFIX;

  public static final String FIND_ALL =
          SELECT_COMMON + " , (SELECT COUNT(*) AS TOTAL "+TABLE_NAME+ " WHERE " + SELECT_SEARCH_STRING +") " +
                  TABLE_NAME + SELECT_SUFFIX +
                  " WHERE " + SELECT_SEARCH_STRING +
                  SELECT_PAGING_SUFFIX;

  public static final String GET_BY_ID = SELECT_COMMON + TABLE_NAME + SELECT_SUFFIX +
          " WHERE GRPID=:id GROUP BY EGOGROUP.GRPID";
  public static final String GET_BY_NAME = SELECT_COMMON + TABLE_NAME + SELECT_SUFFIX +
          " WHERE EGOGROUP.GRPNAME=:name GROUP BY EGOGROUP.GRPID";
  public static final String UPDATE_QUERY = "UPDATE EGOGROUP SET grpName=:name, status=:status," +
          "description=:description WHERE grpId=:id";
  public static final String INSERT_QUERY = "INSERT INTO EGOGROUP (grpName, status, description) " +
          "VALUES (:name, :status, :description)";
  public static final String DELETE_QUERY = "DELETE from EGOGROUP where GRPID=:id";

  public static final String FIND_ALL_GROUPS_OF_USER =
          "WITH allGroups AS ( " +
          "    SELECT COUNT(*) AS TOTAL from usergroup where usergroup.username=:userName" +
          ") " +
          "SELECT egogroup.*, allGroups.total from egogroup, allGroups " +
          "where egogroup.grpname " +
          "      IN " +
          "      (SELECT grpname from usergroup where usergroup.username=:userName) " +
          " AND " + SELECT_SEARCH_STRING +
          "GROUP BY grpId, allGroups.total " +
          "ORDER BY lower(egogroup.<sort>) <sortOrder> " +
          "LIMIT :limit OFFSET :offset";

  public static final String FIND_ALL_GROUPS_OF_APP =
          "WITH allGroups AS ( " +
          "    SELECT COUNT(*) AS TOTAL from groupapplication where groupapplication.appName=:appName" +
          ") " +
          "SELECT egogroup.*, allGroups.total from egogroup, allGroups " +
          "where egogroup.grpname " +
          "      IN " +
          "      (SELECT grpname from groupapplication where groupapplication.appName=:appName) " +
          " AND " + SELECT_SEARCH_STRING +
          "GROUP BY egogroup.grpId, allGroups.total " +
          "ORDER BY lower(egogroup.<sort>) <sortOrder> " +
          "LIMIT :limit OFFSET :offset";
}
