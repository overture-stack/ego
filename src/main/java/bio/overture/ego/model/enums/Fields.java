/*
 * Copyright (c) 2017. The Ontario Institute for Cancer Research. All rights reserved.
 *
 * Licensed under the Apache License; Version 2.0 (the "License" ;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing; software
 * distributed under the License is distributed on an "AS IS" BASIS;
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND; either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package bio.overture.ego.model.enums;

import static lombok.AccessLevel.PRIVATE;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = PRIVATE)
public class Fields {

  public static final String ID = "id";
  public static final String NAME = "name";
  public static final String EMAIL = "email";
  public static final String ROLE = "role";
  public static final String STATUS = "status";
  public static final String FIRSTNAME = "firstname";
  public static final String LASTNAME = "lastname";
  public static final String CREATEDAT = "createdat";
  public static final String LASTLOGIN = "lastlogin";
  public static final String PREFERREDLANGUAGE = "preferredlanguage";
  public static final String DESCRIPTION = "description";
  public static final String CLIENTID = "clientid";
  public static final String CLIENTSECRET = "clientsecret";
  public static final String REDIRECTURI = "redirecturi";
  public static final String USERID_JOIN = "user_id";
  public static final String GROUPID_JOIN = "group_id";
  public static final String POLICYID_JOIN = "policy_id";
  public static final String TOKENID_JOIN = "token_id";
  public static final String APPID_JOIN = "application_id";
  public static final String OWNER = "owner";
  public static final String ACCESS_LEVEL = "access_level";
  public static final String TOKEN = "token";
  public static final String ISSUEDATE = "issuedate";
  public static final String ISREVOKED = "isrevoked";
  public static final String APPLICATIONS = "applications";
  public static final String GROUPS = "groups";

}
