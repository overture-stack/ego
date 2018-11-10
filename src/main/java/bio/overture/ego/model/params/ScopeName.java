package bio.overture.ego.model.params;

import lombok.Data;
import lombok.val;
import bio.overture.ego.model.enums.AccessLevel;
import org.springframework.security.oauth2.common.exceptions.InvalidScopeException;
import static java.lang.String.format;
@Data
public class ScopeName {
  private String scopeName;

  public ScopeName(String name) {
    val results = name.split(":");

    if (results.length != 2) {
      throw new InvalidScopeException(format("Bad scope name '%s'. Must be of the form \"<policyName>:<permission>\"",
        name));
    }
    this.scopeName = name;
  }

  public AccessLevel getMask() {
    val results = scopeName.split(":");
    return AccessLevel.fromValue(results[1]);
  }

  public String getName() {
    val results = scopeName.split(":");
    return results[0];
  }

  @Override
  public String toString() {
    return scopeName;
  }
}