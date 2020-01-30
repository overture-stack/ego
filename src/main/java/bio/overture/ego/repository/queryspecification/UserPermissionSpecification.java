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

import static bio.overture.ego.model.enums.JavaFields.*;

import bio.overture.ego.model.entity.*;
import bio.overture.ego.utils.QueryUtils;
import java.util.UUID;
import javax.persistence.criteria.Join;
import lombok.NonNull;
import lombok.val;
import org.springframework.data.jpa.domain.Specification;

public class UserPermissionSpecification extends SpecificationBase<UserPermission> {

  public static Specification<UserPermission> withPolicy(@NonNull UUID policyId) {
    return (root, query, builder) -> {
      query.distinct(true);

      Join<UserPermission, Policy> userPermissionPolicyJoin = root.join(POLICY);
      return builder.equal(userPermissionPolicyJoin.<Integer>get(ID), policyId);
    };
  }

  public static Specification<UserPermission> withUser(@NonNull UUID userId) {
    return (root, query, builder) -> {
      query.distinct(true);
      Join<UserPermission, Policy> applicationJoin = root.join(OWNER);
      return builder.equal(applicationJoin.<Integer>get(ID), userId);
    };
  }

  public static Specification<UserPermission> containsText(@NonNull String text) {
    val finalText = QueryUtils.prepareForQuery(text);

    // TODO: these joins are not working
    return (root, query, builder) -> {
      Join<UserPermission, User> userPermissionJoin = root.join(OWNER);

      query.distinct(true);
      return builder.or(
          getQueryPredicatesForJoin(builder, userPermissionJoin, finalText, ID, NAME, ACCESS_LEVEL));
      //      return builder.or(getQueryPredicates(builder, root, finalText, ID, ACCESS_LEVEL));
    };
  }
}
