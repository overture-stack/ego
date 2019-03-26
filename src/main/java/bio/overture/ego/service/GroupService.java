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

package bio.overture.ego.service;

import bio.overture.ego.model.dto.GroupRequest;
import bio.overture.ego.model.entity.Group;
import bio.overture.ego.model.search.SearchFilter;
import bio.overture.ego.repository.GroupRepository;
import bio.overture.ego.repository.queryspecification.GroupSpecification;
import bio.overture.ego.service.association.FindRequest;
import lombok.NonNull;
import lombok.val;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.TargetType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static bio.overture.ego.model.enums.JavaFields.APPLICATIONS;
import static bio.overture.ego.model.enums.JavaFields.ID;
import static bio.overture.ego.model.enums.JavaFields.PERMISSIONS;
import static bio.overture.ego.model.enums.JavaFields.USERS;
import static bio.overture.ego.model.exceptions.NotFoundException.checkNotFound;
import static bio.overture.ego.model.exceptions.UniqueViolationException.checkUnique;
import static bio.overture.ego.utils.FieldUtils.onUpdateDetected;
import static javax.persistence.criteria.JoinType.LEFT;
import static org.mapstruct.factory.Mappers.getMapper;
import static org.springframework.data.jpa.domain.Specifications.where;

@Service
@Transactional
public class GroupService extends AbstractNamedService<Group, UUID> {

  /** Constants */
  private static final GroupConverter GROUP_CONVERTER = getMapper(GroupConverter.class);

  /** Dependencies */
  private final GroupRepository groupRepository;

  @Autowired
  public GroupService(@NonNull GroupRepository groupRepository) {
    super(Group.class, groupRepository);
    this.groupRepository = groupRepository;
  }

  @Override
  public Group getWithRelationships(UUID id) {
    val result =
        (Optional<Group>) getRepository().findOne(fetchSpecification(id, true, true, true));
    checkNotFound(result.isPresent(), "The groupId '%s' does not exist", id);
    return result.get();
  }

  public Group create(@NonNull GroupRequest request) {
    checkNameUnique(request.getName());
    val group = GROUP_CONVERTER.convertToGroup(request);
    return getRepository().save(group);
  }

  public Group partialUpdate(@NonNull UUID id, @NonNull GroupRequest r) {
    val group = getById(id);
    validateUpdateRequest(group, r);
    GROUP_CONVERTER.updateGroup(r, group);
    return getRepository().save(group);
  }

  public Page<Group> listGroups(@NonNull List<SearchFilter> filters, @NonNull Pageable pageable) {
    return groupRepository.findAll(GroupSpecification.filterBy(filters), pageable);
  }

  public Page<Group> findGroups(
      @NonNull String query, @NonNull List<SearchFilter> filters, @NonNull Pageable pageable) {
    return groupRepository.findAll(
        where(GroupSpecification.containsText(query)).and(GroupSpecification.filterBy(filters)),
        pageable);
  }

  public Page<Group> findUserGroups(
      @NonNull UUID userId, @NonNull List<SearchFilter> filters, @NonNull Pageable pageable) {
    return groupRepository.findAll(
        where(GroupSpecification.containsUser(userId)).and(GroupSpecification.filterBy(filters)),
        pageable);
  }

  public static Specification<Group> buildFindGroupsByUserSpecification(
      @NonNull FindRequest findRequest) {
    val baseSpec =
        where(GroupSpecification.containsUser(findRequest.getId()))
            .and(GroupSpecification.filterBy(findRequest.getFilters()));
    return findRequest
        .getQuery()
        .map(q -> baseSpec.and(GroupSpecification.containsText(q)))
        .orElse(baseSpec);
  }

  public Page<Group> findUserGroups(
      @NonNull UUID userId,
      @NonNull String query,
      @NonNull List<SearchFilter> filters,
      @NonNull Pageable pageable) {
    return groupRepository.findAll(
        where(GroupSpecification.containsUser(userId))
            .and(GroupSpecification.containsText(query))
            .and(GroupSpecification.filterBy(filters)),
        pageable);
  }

  public Page<Group> findApplicationGroups(
      @NonNull UUID appId, @NonNull List<SearchFilter> filters, @NonNull Pageable pageable) {
    return groupRepository.findAll(
        where(GroupSpecification.containsApplication(appId))
            .and(GroupSpecification.filterBy(filters)),
        pageable);
  }

  public static Specification<Group> buildFindGroupsByApplicationSpecification(
      @NonNull FindRequest applicationFindRequest) {
    val baseSpec =
        where(GroupSpecification.containsApplication(applicationFindRequest.getId()))
            .and(GroupSpecification.filterBy(applicationFindRequest.getFilters()));
    return applicationFindRequest
        .getQuery()
        .map(q -> baseSpec.and(GroupSpecification.containsText(q)))
        .orElse(baseSpec);
  }

  public Page<Group> findApplicationGroups(
      @NonNull UUID appId,
      @NonNull String query,
      @NonNull List<SearchFilter> filters,
      @NonNull Pageable pageable) {
    return groupRepository.findAll(
        where(GroupSpecification.containsApplication(appId))
            .and(GroupSpecification.containsText(query))
            .and(GroupSpecification.filterBy(filters)),
        pageable);
  }

  private void validateUpdateRequest(Group originalGroup, GroupRequest updateRequest) {
    onUpdateDetected(
        originalGroup.getName(),
        updateRequest.getName(),
        () -> checkNameUnique(updateRequest.getName()));
  }

  private void checkNameUnique(String name) {
    checkUnique(
        !groupRepository.existsByNameIgnoreCase(name), "A group with same name already exists");
  }

  private static Specification<Group> fetchSpecification(
      UUID id, boolean fetchApplications, boolean fetchUsers, boolean fetchGroupPermissions) {
    return (fromGroup, query, builder) -> {
      if (fetchApplications) {
        fromGroup.fetch(APPLICATIONS, LEFT);
      }
      if (fetchUsers) {
        fromGroup.fetch(USERS, LEFT);
      }
      if (fetchGroupPermissions) {
        fromGroup.fetch(PERMISSIONS, LEFT);
      }
      return builder.equal(fromGroup.get(ID), id);
    };
  }

  @Mapper(
      nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
      unmappedTargetPolicy = ReportingPolicy.WARN)
  public abstract static class GroupConverter {

    public abstract Group convertToGroup(GroupRequest request);

    public abstract void updateGroup(Group updatingGroup, @MappingTarget Group groupToUpdate);

    public Group copy(Group groupToCopy) {
      val newGroup = initGroupEntity(Group.class);
      updateGroup(groupToCopy, newGroup);
      return newGroup;
    }

    public abstract void updateGroup(GroupRequest request, @MappingTarget Group groupToUpdate);

    protected Group initGroupEntity(@TargetType Class<Group> groupClass) {
      return Group.builder().build();
    }
  }
}
