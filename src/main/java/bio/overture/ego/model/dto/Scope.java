package bio.overture.ego.model.dto;

import bio.overture.ego.model.entity.Policy;
import bio.overture.ego.model.enums.AccessLevel;
import bio.overture.ego.model.params.ScopeName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import lombok.val;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import static java.util.Objects.isNull;

@Data
@AllArgsConstructor
public class Scope {

  private Policy policy;
  private AccessLevel accessLevel;

  @Override
  public String toString() {
    return getPolicyName() + "." + getAccessLevelName();
  }

  public String getPolicyName() {
    if (isNull(policy)) {
      return "Null policy";
    }
    if (isNull(policy.getName())) {
      return "Nameless policy";
    }
    return policy.getName();
  }

  public String getAccessLevelName() {
    if (isNull(accessLevel)) {
      return "Null accessLevel";
    }
    return accessLevel.toString();
  }

  public ScopeName toScopeName() {
    return new ScopeName(this.toString());
  }

  public static Set<Scope> missingScopes(Set<Scope> have, Set<Scope> want) {
    val map = new HashMap<Policy, AccessLevel>();
    val missing = new HashSet<Scope>();
    for (Scope scope : have) {
      map.put(scope.getPolicy(), scope.getAccessLevel());
    }

    for (val s : want) {
      val need = s.getAccessLevel();
      AccessLevel got = map.get(s.getPolicy());

      if (got == null || !AccessLevel.allows(got, need)) {
        missing.add(s);
      }
    }
    return missing;
  }

  public static Set<Scope> effectiveScopes(Set<Scope> have, Set<Scope> want) {
    // In general, our effective scope is the lesser of the scope we have,
    // or the scope we want. This lets us have tokens that don't give away all of
    // the authority that we do by creating a list of scopes we want to authorize.
    val map = new HashMap<Policy, AccessLevel>();
    val effectiveScope = new HashSet<Scope>();
    for (val scope : have) {
      map.put(scope.getPolicy(), scope.getAccessLevel());
    }

    for (val s : want) {
      val policy = s.getPolicy();
      val need = s.getAccessLevel();
      val got = map.getOrDefault(policy, AccessLevel.DENY);
      // if we can do what we want, then add just what we need
      if (AccessLevel.allows(got, need)) {
        effectiveScope.add(new Scope(policy, need));
      } else {
        // If we can't do what we want, we can do what we have,
        // unless our permission is DENY, in which case we can't
        // do anything
        if (got != AccessLevel.DENY) {
          effectiveScope.add(new Scope(policy, got));
        }
      }
    }
    return effectiveScope;
  }

  /**
   * Return a set of explicit scopes, which always include a scope with READ access for each scope
   * with WRITE access.
   *
   * @param scopes
   * @return The explicit version of the set of scopes passed in.
   */
  public static Set<Scope> explicitScopes(Set<Scope> scopes) {
    val explicit = new HashSet<Scope>();
    for (val s : scopes) {
      explicit.add(s);
      if (s.getAccessLevel().equals(AccessLevel.WRITE)) {
        explicit.add(new Scope(s.getPolicy(), AccessLevel.READ));
      }
    }
    return explicit;
  }

  public static Scope createScope(@NonNull Policy policy, @NonNull AccessLevel accessLevel){
    return new Scope(policy, accessLevel);
  }

}
