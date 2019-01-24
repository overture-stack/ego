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

import bio.overture.ego.model.entity.Application;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.springframework.data.jpa.repository.EntityGraph.EntityGraphType.FETCH;

public interface ApplicationRepository extends NamedRepository<Application, UUID> {

  Application findOneByClientIdIgnoreCase(String clientId);

  Optional<Application> getApplicationByNameIgnoreCase(String name);

  @EntityGraph(value = "application-entity-with-relationships", type = FETCH)
  Optional<Application> getApplicationByClientIdIgnoreCase(String clientId);

  boolean existsByClientIdIgnoreCase(String clientId);

  Application findOneByNameIgnoreCase(String name);

  Application findOneByName(String name);

  Page<Application> findAllByStatusIgnoreCase(String status, Pageable pageable);

  Set<Application> findAllByIdIn(List<UUID> ids);

  @Override
  default Optional<Application> findByName(String name) {
    return getApplicationByNameIgnoreCase(name);
  }
}
