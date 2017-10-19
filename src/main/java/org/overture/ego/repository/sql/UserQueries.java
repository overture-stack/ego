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

  /*
    Query strings related to user
   */
  public static final String GET_ALL_USERS =
          SELECT_COMMON + " , (SELECT COUNT(*) AS TOTAL "+TABLE_NAME+") " +
          TABLE_NAME + SELECT_SUFFIX +
          " GROUP BY EGOUSER.USERID " +
          " ORDER BY EGOUSER.USERID " +
          " LIMIT :limit OFFSET :number";
  public static final String GET_BY_USERID = SELECT_COMMON + TABLE_NAME + SELECT_SUFFIX +
          " WHERE USERID=:id GROUP BY EGOUSER.USERID";
  public static final String GET_BY_USERNAME = SELECT_COMMON + TABLE_NAME + SELECT_SUFFIX +
          " WHERE EGOUSER.USERNAME=:name GROUP BY EGOUSER.USERID";
  public static final String UPDATE_QUERY = "UPDATE EGOUSER SET role=:role, status=:status," +
          "firstName=:firstName, lastName=:lastName, createdAt=:createdAt , lastLogin=:lastLogin, " +
          "preferredLanguage=:preferredLanguage WHERE userName=:userName";
  public static final String INSERT_QUERY = "INSERT INTO EGOUSER (userName, email, role, status, firstName, lastName, " +
          "createdAt,lastLogin,preferredLanguage) " +
          "VALUES (:name, :email, :role, :status, :firstName, :lastName, :createdAt, :lastLogin, :preferredLanguage)";
  public static final String DELETE_QUERY = "DELETE from EGOUSER where USERID=:id";
}
