package bio.overture.ego.model.enums;

import lombok.NoArgsConstructor;

import static lombok.AccessLevel.PRIVATE;


@NoArgsConstructor(access = PRIVATE)
public class Tables {

  //TODO: since table names do not contain underscores, shouldnt the variable name do the same?
  // A new convention can be, camelCaseText converts to the variable CAMEL_CASE_TEXT
  public static final String GROUP = "egogroup";
  public static final String GROUP_APPLICATION = "groupapplication";
  public static final String GROUP_USER = "usergroup";
  public static final String EGOUSER = "egouser";

}
