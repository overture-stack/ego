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

public class UserQueries {


  private static  final String SELECT_COMMON = "SELECT EGOUSER.USERID, EGOUSER.USERNAME,EGOUSER.EMAIL, " +
          " EGOUSER.ROLE, EGOUSER.STATUS, EGOUSER.FIRSTNAME, EGOUSER.LASTNAME, " +
          "  EGOUSER.CREATEDAT,EGOUSER.LASTLOGIN, EGOUSER.PREFERREDLANGUAGE, " +
          "  STRING_AGG(USERGROUP.GRPNAME, ',') AS Groups, " +
          "  STRING_AGG(USERAPPLICATION.APPNAME, ',') AS Applications ";
  private static final String TABLE_NAME = " FROM EGOUSER ";
  private static final String SELECT_SUFFIX =
          "  LEFT JOIN  USERGROUP ON EGOUSER.USERNAME = USERGROUP.USERNAME " +
          "  LEFT JOIN USERAPPLICATION on EGOUSER.USERNAME = USERAPPLICATION.APPNAME";
  private static final String SELECT_PAGING_SUFFIX =
          " GROUP BY EGOUSER.USERID " +
          " ORDER BY lower(EGOUSER.<sort>) <sortOrder> " +
          " LIMIT :limit OFFSET :offset";
  private static final String SELECT_SEARCH_STRING =
          " lower(EGOUSER.FIRSTNAME) LIKE :query OR" +
          " lower(EGOUSER.LASTNAME) LIKE :query OR" +
          " lower(EGOUSER.USERNAME) LIKE :query OR" +
          " lower(EGOUSER.EMAIL) LIKE :query " ;

  /*
    Query strings related to user
   */
  public static final String GET_ALL =
          SELECT_COMMON + " , (SELECT COUNT(*) AS TOTAL "+TABLE_NAME+") " +
          TABLE_NAME + SELECT_SUFFIX + SELECT_PAGING_SUFFIX;
  public static final String FIND_ALL =
          SELECT_COMMON + " , (SELECT COUNT(*) AS TOTAL "+TABLE_NAME+ " WHERE " + SELECT_SEARCH_STRING +") " +
                  TABLE_NAME + SELECT_SUFFIX +
                  " WHERE " + SELECT_SEARCH_STRING +
                  SELECT_PAGING_SUFFIX;
  public static final String GET_BY_ID = SELECT_COMMON + TABLE_NAME + SELECT_SUFFIX +
          " WHERE USERID=:id GROUP BY EGOUSER.USERID";
  public static final String GET_BY_NAME = SELECT_COMMON + TABLE_NAME + SELECT_SUFFIX +
          " WHERE EGOUSER.USERNAME=:name GROUP BY EGOUSER.USERID";
  public static final String UPDATE_QUERY = "UPDATE EGOUSER SET userName=:name, role=:role, status=:status," +
          "firstName=:firstName, lastName=:lastName, createdAt=:createdAt , lastLogin=:lastLogin, " +
          "preferredLanguage=:preferredLanguage WHERE userid=:id";
  public static final String INSERT_QUERY = "INSERT INTO EGOUSER (userName, email, role, status, firstName, lastName, " +
          "createdAt,lastLogin,preferredLanguage) " +
          "VALUES (:name, :email, :role, :status, :firstName, :lastName, :createdAt, :lastLogin, :preferredLanguage)";
  public static final String DELETE_QUERY = "DELETE from EGOUSER where USERID=:id";

  public static final String FIND_ALL_USERS_IN_GROUP =
          "WITH allUsers AS ( " +
          "    SELECT COUNT(*) AS TOTAL from usergroup where usergroup.grpname=:grpName" +
          ") " +
          "SELECT egouser.*, allusers.total from egouser, allusers " +
          "where egouser.username " +
          "      IN " +
          "      (SELECT username from usergroup where usergroup.grpname=:grpName) " +
          " AND " + SELECT_SEARCH_STRING +
          "GROUP BY userid, allusers.total " +
          "ORDER BY lower(egouser.<sort>) <sortOrder> " +
          "LIMIT :limit OFFSET :offset";

  public static final String FIND_ALL_USERS_OF_APP = "WITH allUsers AS ( " +
          "    SELECT COUNT(*) AS TOTAL from userapplication where userapplication.appName=:appName" +
          ") " +
          "SELECT egouser.*, allusers.total from egouser, allusers " +
          "where egouser.username " +
          "      IN " +
          "      (SELECT username from userapplication where userapplication.appName=:appName) " +
          " AND " + SELECT_SEARCH_STRING +
          "GROUP BY userid, allusers.total " +
          "ORDER BY lower(egouser.<sort>) <sortOrder> " +
          "LIMIT :limit OFFSET :offset";
}
