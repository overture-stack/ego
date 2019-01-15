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

import bio.overture.ego.model.entity.Group;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.EntityGraph.EntityGraphType;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface GroupRepository extends NamedRepository<Group, UUID> {

  @EntityGraph(value = "group-entity-with-relationships", type = EntityGraphType.FETCH)
  Group findOneByNameIgnoreCase(String name);

  Optional<Group> getGroupByNameIgnoreCase(String name);

  Set<Group> findAllByIdIn(List<UUID> groupIds);

  @Override
  default Optional<Group> findByName(String name) {
    return getGroupByNameIgnoreCase(name);
  }
}
