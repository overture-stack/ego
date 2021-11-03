package bio.overture.ego.model.params;

import static java.lang.String.format;

import bio.overture.ego.model.enums.AccessLevel;
import lombok.Data;

@Data
public class ScopeName {
  private String scopeName;

  public ScopeName(String name) {
    if (name.indexOf(".") == -1) {
      throw new RuntimeException(
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
