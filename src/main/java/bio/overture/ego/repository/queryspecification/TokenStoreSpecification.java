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

import bio.overture.ego.model.entity.ApiKey;
import bio.overture.ego.model.entity.User;
import bio.overture.ego.utils.QueryUtils;
import jakarta.persistence.criteria.Join;
import java.util.UUID;
import lombok.NonNull;
import lombok.val;
import org.springframework.data.jpa.domain.Specification;

public class TokenStoreSpecification extends SpecificationBase<ApiKey> {

  public static Specification<ApiKey> containsText(@NonNull String text) {
    val finalText = QueryUtils.prepareForQuery(text);
    return (root, query, builder) -> {
      query.distinct(true);

      return builder.or(getQueryPredicates(builder, root, finalText, NAME));
    };
  }

  public static Specification<ApiKey> containsUser(@NonNull UUID userId) {
    return (root, query, builder) -> {
      query.distinct(true);

      Join<ApiKey, User> apiKeyJoin = root.join(OWNER);
      return builder.equal(apiKeyJoin.<Integer>get(ID), userId);
    };
  }
}
