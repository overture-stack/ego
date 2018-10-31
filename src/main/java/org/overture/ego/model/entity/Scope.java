package org.overture.ego.model.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.val;
import org.overture.ego.model.enums.PolicyMask;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

@Data
@AllArgsConstructor
public class Scope{
  Policy policy;
  PolicyMask policyMask;
  @Override
  public String toString() {
    return policy.getName()+":"+ policyMask.toString();
  }

  public static Set<Scope> missingScopes(Set<Scope> have, Set<Scope> want) {
    val map = new HashMap<Policy, PolicyMask>();
    val missing = new HashSet<Scope>();
    for (Scope scope : want) {
      map.put(scope.getPolicy(), scope.getPolicyMask());
    }

    for(val s: have) {
      val need = s.getPolicyMask();
      PolicyMask got = map.get(s.getPolicy());

      if (got == null || !PolicyMask.allows(got, need)) {
        missing.add(s);
      }
    }
    return missing;
  }

  public static Set<Scope> effectiveScopes(Set<Scope> have, Set<Scope> want) {
    val map = new HashMap<Policy, PolicyMask>();
    val effectiveScope = new HashSet<Scope>();
    for (val scope : have) {
      map.put(scope.getPolicy(), scope.getPolicyMask());
    }

    for(val s:want) {
      val p = s.getPolicy();
      val need = s.getPolicyMask();
      PolicyMask got= map.getOrDefault(p, PolicyMask.DENY);
      if (PolicyMask.allows(got, need)) {
        effectiveScope.add(new Scope(p, need));
      } else {
        effectiveScope.add(new Scope(p, got));
      }
    }
    return effectiveScope;
  }
}
