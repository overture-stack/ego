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

import bio.overture.ego.model.entity.Application;
import bio.overture.ego.model.entity.Group;
import bio.overture.ego.model.entity.User;
import bio.overture.ego.model.enums.JavaFields;
import bio.overture.ego.model.join.UserGroup;
import bio.overture.ego.utils.QueryUtils;
import java.util.UUID;
import javax.persistence.criteria.Join;
import lombok.NonNull;
import lombok.val;
import org.springframework.data.jpa.domain.Specification;

public class GroupSpecification extends SpecificationBase<Group> {
  public static Specification<Group> containsText(@NonNull String text) {
    val finalText = QueryUtils.prepareForQuery(text);
    return (root, query, builder) ->
        builder.or(getQueryPredicates(builder, root, finalText, "name", "description", "status"));
  }

  public static Specification<Group> containsApplication(@NonNull UUID appId) {
    return (root, query, builder) -> {
      query.distinct(true);
      Join<Application, Group> groupJoin = root.join("applications");
      return builder.equal(groupJoin.<Integer>get("id"), appId);
    };
  }

  public static Specification<Group> containsUser(@NonNull UUID userId) {
    return (root, query, builder) -> {
      query.distinct(true);
      Join<Group, UserGroup> groupJoin = root.join(JavaFields.USERGROUPS);
      Join<UserGroup, User> userJoin = groupJoin.join(JavaFields.USER);
      return builder.equal(userJoin.<Integer>get(JavaFields.ID), userId);
    };
  }
}
