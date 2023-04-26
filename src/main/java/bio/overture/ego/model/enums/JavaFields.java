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
public class JavaFields {

  public static final String ID = "id";
  public static final String NAME = "name";
  public static final String TYPE = "type";
  public static final String EMAIL = "email";
  public static final String STATUS = "status";
  public static final String POLICY = "policy";
  public static final String ACCESS_LEVEL = "accessLevel";
  public static final String FIRSTNAME = "firstName";
  public static final String LASTNAME = "lastName";
  public static final String CREATEDAT = "createdAt";
  public static final String LASTLOGIN = "lastLogin";
  public static final String PREFERREDLANGUAGE = "preferredLanguage";
  public static final String APPLICATIONS = "applications";
  public static final String APPLICATION = "application";
  public static final String OWNER = "owner";
  public static final String SCOPES = "scopes";
  public static final String GROUPS = "groups";
  public static final String GROUP = "group";
  public static final String USERS = "users";
  public static final String USER = "user";
  public static final String USERTYPE = "usertype";
  public static final String APPLICATIONTYPE = "applicationtype";
  public static final String TOKENS = "tokens";
  public static final String TOKEN = "token";
  public static final String USERPERMISSIONS = "userPermissions";
  public static final String PERMISSIONS = "permissions";
  public static final String USERPERMISSION = "userPermission";
  public static final String GROUPPERMISSION = "groupPermission";
  public static final String GROUPPERMISSIONS = "groupPermissions";
  public static final String DESCRIPTION = "description";
  public static final String CLIENTID = "clientId";
  public static final String CLIENTSECRET = "clientSecret";
  public static final String REDIRECTURI = "redirectUri";
  public static final String USER_ID = "userId";
  public static final String GROUP_ID = "groupId";
  public static final String USERGROUPS = "userGroups";
  public static final String APPLICATION_ID = "applicationId";
  public static final String GROUPAPPLICATIONS = "groupApplications";
  public static final String USERAPPLICATIONS = "userApplications";
  public static final String REFRESH_ID = "refreshId";
  public static final String PROVIDERTYPE = "providerType";
  public static final String PROVIDER_SUBJECT_ID = "providerSubjectId";
  public static final String ERROR_REDIRECT_URI = "errorRedirectUri";
  // Visas Added
  public static final String SOURCE = "source";
  public static final String VALUE = "value";

  public static final String BY = "by";

  public static final String VISAPERMISSION = "ACLVISAPERMISSION";
}
