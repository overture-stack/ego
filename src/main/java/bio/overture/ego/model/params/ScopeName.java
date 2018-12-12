package bio.overture.ego.model.params;

import bio.overture.ego.model.enums.AccessLevel;
import lombok.Data;
import org.springframework.security.oauth2.common.exceptions.InvalidScopeException;

import static java.lang.String.format;

@Data
public class ScopeName {
  private String scopeName;

  public ScopeName(String name) {
    if (name.indexOf(".") == -1) {
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
