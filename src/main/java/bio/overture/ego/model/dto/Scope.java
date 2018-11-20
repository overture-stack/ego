package bio.overture.ego.model.dto;

import bio.overture.ego.model.entity.Policy;
import bio.overture.ego.model.enums.AccessLevel;
import bio.overture.ego.model.params.ScopeName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.val;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

@Data
@AllArgsConstructor
public class Scope{
  Policy policy;
  AccessLevel policyMask;

  @Override
  public String toString() {
    return getPolicyName()+"."+ getMaskName();
  }

  public String getPolicyName() {
    if (policy == null) {
      return "Null policy";
    }
    if (policy.getName() == null) {
      return "Nameless policy";
    }
    return policy.getName();
  }

  public String getMaskName() {
    if (policyMask == null) {
      return "Null accessLevel";
    }
    return policyMask.toString();
  }

  public ScopeName toScopeName() {
    return new ScopeName(this.toString());
  }

  public static Set<Scope> missingScopes(Set<Scope> have, Set<Scope> want) {
    val map = new HashMap<Policy, AccessLevel>();
    val missing = new HashSet<Scope>();
    for (Scope scope : have) {
      map.put(scope.getPolicy(), scope.getPolicyMask());
    }

    for(val s: want) {
      val need = s.getPolicyMask();
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
      map.put(scope.getPolicy(), scope.getPolicyMask());
    }

    for(val s:want) {
      val policy = s.getPolicy();
      val need = s.getPolicyMask();
      val got=map.getOrDefault(policy, AccessLevel.DENY);
      // if we can do what we want, then add just what we need
      if (AccessLevel.allows(got, need)) {
        effectiveScope.add(new Scope(policy, need));
      } else {
        // If we can't do what we want, we can do what we have,
        // unless our permission is DENY, in which case we can't
        // do anything with
        if (got != AccessLevel.DENY) {
          effectiveScope.add(new Scope(policy, got));
        }
      }
    }
    return effectiveScope;
  }
}
