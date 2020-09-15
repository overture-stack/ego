package bio.overture.ego.model.enums;

import static lombok.AccessLevel.PRIVATE;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = PRIVATE)
public class Tables {

  public static final String APPLICATION = "egoapplication";
  public static final String GROUP = "egogroup";
  public static final String TOKEN = "token";
  public static final String GROUP_APPLICATION = "groupapplication";
  public static final String USER_GROUP = "usergroup";
  public static final String EGOUSER = "egouser";
  public static final String USER_APPLICATION = "userapplication";
  public static final String USER_PERMISSION = "userpermission";
  public static final String GROUP_PERMISSION = "grouppermission";
  public static final String POLICY = "policy";
  public static final String TOKENSCOPE = "tokenscope";
  public static final String REFRESHTOKEN = "refreshtoken";
  public static final String APPLICATION_PERMISSION = "applicationpermission";
}
