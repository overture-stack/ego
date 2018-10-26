package org.overture.ego.model.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.overture.ego.model.enums.PolicyMask;

@Data
@AllArgsConstructor
public class Scope{
  Policy policy;
  PolicyMask policyMask;
  @Override
  public String toString() {
    return policy.getName()+":"+ policyMask.toString();
  }
}
