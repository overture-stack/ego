package bio.overture.ego.model.enums;

import lombok.NoArgsConstructor;

import static lombok.AccessLevel.PRIVATE;

@NoArgsConstructor(access = PRIVATE)
public class SqlFields {

  public static final String ID = "id";
  public static final String NAME = "name";
  public static final String EMAIL = "email";
  public static final String ROLE = "role";
  public static final String TOKEN = "token";
  public static final String STATUS = "status";
  public static final String FIRSTNAME = "firstname";
  public static final String LASTNAME = "lastname";
  public static final String CREATEDAT = "createdat";
  public static final String LASTLOGIN = "lastlogin";
  public static final String PREFERREDLANGUAGE = "preferredlanguage";
  public static final String USERID_JOIN = "user_id";
  public static final String GROUPID_JOIN = "group_id";
  public static final String TOKENID_JOIN = "token_id";
  public static final String APPID_JOIN = "application_id";
  public static final String ACCESS_LEVEL = "access_level";
  public static final String POLICY = "policy";
  public static final String OWNER = "owner";
  public static final String DESCRIPTION = "description";
  public static final String POLICYID_JOIN = "policy_id";
  public static final String CLIENTID = "clientid";
  public static final String CLIENTSECRET = "clientsecret";
  public static final String REDIRECTURI = "redirecturi";
  public static final String ISSUEDATE = "issuedate";
  public static final String ISREVOKED = "isrevoked";
}
