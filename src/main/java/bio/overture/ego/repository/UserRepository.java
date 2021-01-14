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
import bio.overture.ego.model.enums.ProviderType;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;

public interface UserRepository extends BaseRepository<User, UUID> {

  Set<User> findAllByIdIn(Collection<UUID> userIds);

  boolean existsByProviderTypeAndProviderSubjectId(
      ProviderType providerType, String providerSubjectId);

  boolean existsByProviderSubjectId(String providerSubjectId);
}
