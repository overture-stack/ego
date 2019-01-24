package bio.overture.ego.model.enums;

import lombok.NoArgsConstructor;

import static lombok.AccessLevel.PRIVATE;

@NoArgsConstructor(access = PRIVATE)
public class Tables {

  // TODO: since table names do not contain underscores, shouldnt the variable name do the same?
  // A new convention can be, camelCaseText converts to the variable CAMEL_CASE_TEXT
  public static final String APPLICATION = "egoapplication";
  public static final String GROUP = "egogroup";
  public static final String GROUP_APPLICATION = "groupapplication";
  public static final String GROUP_USER = "usergroup";
  public static final String EGOUSER = "egouser";
  public static final String USER_APPLICATION = "userapplication";
  public static final String USER_PERMISSION = "userpermission";
  public static final String GROUP_PERMISSION = "grouppermission";
  public static final String TOKEN_APPLICATION = "tokenapplication";
  public static final String POLICY = "policy";
}
