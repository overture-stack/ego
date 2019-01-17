package bio.overture.ego.model.enums;

import static lombok.AccessLevel.PRIVATE;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = PRIVATE)
public class SqlFields {

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
  public static final String USERID_JOIN = "user_id";
  public static final String GROUPID_JOIN = "group_id";
  public static final String APPID_JOIN = "application_id";

}