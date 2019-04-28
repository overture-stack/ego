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
import bio.overture.ego.model.join.GroupApplication;
import bio.overture.ego.model.join.UserApplication;
import bio.overture.ego.utils.QueryUtils;
import lombok.NonNull;
import lombok.val;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.Join;
import java.util.UUID;

import static bio.overture.ego.model.enums.JavaFields.CLIENTID;
import static bio.overture.ego.model.enums.JavaFields.CLIENTSECRET;
import static bio.overture.ego.model.enums.JavaFields.DESCRIPTION;
import static bio.overture.ego.model.enums.JavaFields.GROUP;
import static bio.overture.ego.model.enums.JavaFields.GROUPAPPLICATIONS;
import static bio.overture.ego.model.enums.JavaFields.ID;
import static bio.overture.ego.model.enums.JavaFields.NAME;
import static bio.overture.ego.model.enums.JavaFields.STATUS;
import static bio.overture.ego.model.enums.JavaFields.USER;
import static bio.overture.ego.model.enums.JavaFields.USERAPPLICATIONS;

public class ApplicationSpecification extends SpecificationBase<Application> {
  public static Specification<Application> containsText(@NonNull String text) {
    val finalText = QueryUtils.prepareForQuery(text);
    return (root, query, builder) -> {
      query.distinct(true);
      return builder.or(
          getQueryPredicates(
              builder, root, finalText, NAME, CLIENTID, CLIENTSECRET, DESCRIPTION, STATUS));
    };
  }

  public static Specification<Application> inGroup(@NonNull UUID groupId) {
    return (root, query, builder) -> {
      query.distinct(true);
      Join<Application, GroupApplication> applicationJoin = root.join(GROUPAPPLICATIONS);
      Join<GroupApplication, Group> groupJoin = applicationJoin.join(GROUP);
      return builder.equal(groupJoin.<Integer>get(ID), groupId);
    };
  }

  public static Specification<Application> usedBy(@NonNull UUID userId) {
    return (root, query, builder) -> {
      query.distinct(true);
      Join<Application, UserApplication> applicationJoin = root.join(USERAPPLICATIONS);
      Join<UserApplication, User> userJoin = applicationJoin.join(USER);
      return builder.equal(userJoin.<Integer>get(ID), userId);
    };
  }
}
