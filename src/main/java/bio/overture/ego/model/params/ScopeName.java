package bio.overture.ego.model.params;

import static java.lang.String.format;

import bio.overture.ego.model.enums.AccessLevel;
import bio.overture.ego.model.exceptions.InvalidScopeException;
import lombok.Data;

@Data
public class ScopeName {
  private String scopeName;

  public ScopeName(String name) {
    if (!name.contains(".")) {
      throw new InvalidScopeException(
          format("Bad scope name '%s'. Must be of the form \"<policyName>.<permission>\"", name));
    }
    scopeName = name;
  }

  public AccessLevel getAccessLevel() {
    return AccessLevel.fromValue(scopeName.substring(scopeName.lastIndexOf(".") + 1));
  }

  public String getName() {
    return scopeName.substring(0, scopeName.lastIndexOf("."));
  }

  @Override
  public String toString() {
    return scopeName;
  }
}
