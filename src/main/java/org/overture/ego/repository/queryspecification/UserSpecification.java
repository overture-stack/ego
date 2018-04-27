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

package org.overture.ego.repository.queryspecification;

import lombok.val;
import org.overture.ego.utils.QueryUtils;
import org.overture.ego.model.entity.Application;
import org.overture.ego.model.entity.Group;
import org.overture.ego.model.entity.User;
import org.springframework.data.jpa.domain.Specification;

import javax.annotation.Nonnull;
import javax.persistence.criteria.Join;


public class UserSpecification extends SpecificationBase<User> {

  public static Specification<User> containsText(@Nonnull String text) {
    val finalText = QueryUtils.prepareForQuery(text);
    return (root, query, builder) -> builder.or(getQueryPredicates(builder,root,finalText,
            "name","email","firstName","lastName","status")
    );
  }

  public static Specification<User> inGroup(@Nonnull Integer groupId) {
    return (root, query, builder) ->
    {
      Join<User, Group> groupJoin = root.join("wholeGroups");
      return builder.equal(groupJoin.<Integer> get("id"), groupId);
    };

  }

  public static Specification<User> ofApplication(@Nonnull Integer appId) {
    return (root, query, builder) ->
    {
      Join<User, Application> applicationJoin = root.join("wholeApplications");
      return builder.equal(applicationJoin.<Integer> get("id"), appId);
    };

  }
}
