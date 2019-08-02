package bio.overture.ego.model.enums;

import static lombok.AccessLevel.PRIVATE;

import lombok.NoArgsConstructor;

/**
 * Note: When using a Lombok annotation with field names (for example @EqualsAndHashCode(ids =
 * {LombokFields.id}) lombok does not look at the variable's value, but instead takes the variables
 * name as the value. https://github.com/rzwitserloot/lombok/issues/1094
 */
@NoArgsConstructor(access = PRIVATE)
public class LombokFields {

  public static final String id = "doesn't matter, lombok doesnt use this string";
  public static final String groups = "doesn't matter, lombok doesnt use this string";
  public static final String userApplications = "doesn't matter, lombok doesnt use this string";
  public static final String policy = "doesn't matter, lombok doesnt use this string";
  public static final String userPermissions = "doesn't matter, lombok doesnt use this string";
  public static final String owner = "doesn't matter, lombok doesnt use this string";
  public static final String scopes = "doesn't matter, lombok doesnt use this string";
  public static final String users = "doesn't matter, lombok doesnt use this string";
  public static final String permissions = "doesn't matter, lombok doesnt use this string";
  public static final String tokens = "doesn't matter, lombok doesnt use this string";
  public static final String userGroups = "doesn't matter, lombok doesnt use this string";
  public static final String groupApplications = "doesn't matter, lombok doesnt use this string";
}
