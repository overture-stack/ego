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

public class ApplicationQueries {

  private static  final String SELECT_COMMON = "SELECT * ";
  private static final String TABLE_NAME = "  FROM EGOAPPLICATION ";
  private static final String SELECT_SUFFIX = "";

  /*
    Query strings related to user
   */
  public static final String GET_ALL =
          SELECT_COMMON + " , (SELECT COUNT(*) AS TOTAL "+TABLE_NAME+") " +
                  TABLE_NAME + SELECT_SUFFIX +
                  " GROUP BY EGOAPPLICATION.APPID " +
                  " ORDER BY EGOAPPLICATION.APPID " +
                  " LIMIT :limit OFFSET :offset";
  public static final String GET_BY_ID = "SELECT appid,appName,clientId,clientSecret,redirectUri,description,status " +
          "FROM EGOAPPLICATION WHERE appid=:id";
  public static final String GET_BY_NAME =
          "SELECT appid,appName,clientId,clientSecret,redirectUri,description,status " +
                  "FROM EGOAPPLICATION WHERE appName=:name";
  public static final String UPDATE_QUERY = "UPDATE EGOAPPLICATION " +
          "SET appName=:name, clientId=:clientId" +
          ", clientSecret=:clientSecret, redirectUri=:redirectUri, description=:description, status=:status" +
          " WHERE appid=:id";
  public static final String INSERT_QUERY =
          "INSERT INTO EGOAPPLICATION (appName, clientId, clientSecret, redirectUri, description, status) " +
          "VALUES (:name, :clientId, :clientSecret, :redirectUri, :description, :status)";
  public static final String DELETE_QUERY = "DELETE from EGOAPPLICATION where appid=:id";
}
