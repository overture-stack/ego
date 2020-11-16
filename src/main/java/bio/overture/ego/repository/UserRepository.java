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

package bio.overture.ego.repository;

import bio.overture.ego.model.entity.User;
import bio.overture.ego.model.enums.IdProviderType;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface UserRepository extends NamedRepository<User, UUID> {

  Optional<User> getUserByNameIgnoreCase(String name);

  boolean existsByEmailIgnoreCase(String email);

  Set<User> findAllByIdIn(Collection<UUID> userIds);

  /** Refer to NamedRepository.findByName Deprecation note */
  @Override
  @Deprecated
  default Optional<User> findByName(String name) {
    return getUserByNameIgnoreCase(name);
  }

  boolean existsByIdentityProviderAndProviderId(IdProviderType providerType, String providerId);

  boolean existsByProviderId(String providerId);
}
