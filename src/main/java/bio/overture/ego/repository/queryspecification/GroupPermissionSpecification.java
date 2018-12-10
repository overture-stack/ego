/*
 * Copyright (c) 2017. The Ontario Institute for Cancer Research. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package bio.overture.ego.repository.queryspecification;

import bio.overture.ego.model.entity.*;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;

public class GroupPermissionSpecification extends SpecificationBase<Permission> {
  public static Specification<GroupPermission> withPolicy(@Nonnull UUID policyId) {
    return (root, query, builder) -> {
      Join<GroupPermission, Policy> applicationJoin = root.join("policy");
      return builder.equal(applicationJoin.<Integer>get("id"), policyId);
    };
  }
}
