package bio.overture.ego.service.join;

import bio.overture.ego.model.entity.Group;
import bio.overture.ego.model.entity.User;
import bio.overture.ego.model.join.UserGroup;
import bio.overture.ego.model.join.UserGroupId;
import bio.overture.ego.model.search.SearchFilter;
import bio.overture.ego.repository.BaseRepository;
import bio.overture.ego.repository.queryspecification.GroupSpecification;
import bio.overture.ego.repository.queryspecification.UserSpecification;
import bio.overture.ego.service.GroupService;
import bio.overture.ego.service.UserService;
import bio.overture.ego.service.association.FindRequest;
import com.google.common.collect.ImmutableList;
import lombok.NonNull;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static bio.overture.ego.utils.Collectors.toImmutableList;
import static bio.overture.ego.utils.Collectors.toImmutableSet;
import static org.springframework.data.jpa.domain.Specification.where;

@Service
public class UserGroupJoinService {

  private final UserService userService;
  private final GroupService groupService;
  private final BaseRepository<UserGroup, UserGroupId> repository;

  @Autowired
  public UserGroupJoinService(
      @NonNull UserService userService,
      @NonNull GroupService groupService,
      @NonNull BaseRepository<UserGroup, UserGroupId> repository) {
    this.userService = userService;
    this.groupService = groupService;
    this.repository = repository;
  }

  public User associate(UUID parentId, Collection<UUID> childIds) {
    val parent = userService.getById(parentId);
    val children = groupService.getMany(childIds);
    val userGroups =
        children
            .stream()
            .map(
                c -> {
                  UserGroupId id =
                      UserGroupId.builder().userId(parentId).groupId(c.getId()).build();
                  return UserGroup.builder().id(id).user(parent).group(c).build();
                })
            .collect(toImmutableSet());
    repository.saveAll(userGroups);
    return parent;
  }

  public Group reverseAssociate(@NonNull UUID childId, @NonNull Collection<UUID> parentIds) {
    val child = groupService.getById(childId);
    val parents = userService.getMany(parentIds);
    val userGroups =
        parents
            .stream()
            .map(
                p -> {
                  UserGroupId id = UserGroupId.builder().userId(p.getId()).groupId(childId).build();
                  return UserGroup.builder().id(id).user(p).group(child).build();
                })
            .collect(toImmutableSet());
    repository.saveAll(userGroups);
    return child;
  }

  public void disassociate(UUID parentId, Collection<UUID> childIds) {
    val ids =
        childIds
            .stream()
            .map(c -> UserGroupId.builder().userId(parentId).groupId(c).build())
            .collect(toImmutableList());
    val userGroups = repository.findAllByIdIn(ids);
    repository.deleteAll(userGroups);
  }

  public void reverseDisassociate(@NonNull UUID childId, @NonNull Collection<UUID> parentIds) {
    val userGroupIds =
        parentIds
            .stream()
            .map(p -> UserGroupId.builder().userId(p).groupId(childId).build())
            .collect(toImmutableList());
    val userGroups = repository.findAllByIdIn(userGroupIds);
    repository.deleteAll(userGroups);
  }

  public Page<Group> findGroupsForUser(
      @NonNull UUID userId, @NonNull List<SearchFilter> filters, @NonNull Pageable pageable) {
    userService.checkExistence(userId);
    return groupService.findAll(
        where(GroupSpecification.containsUser(userId)).and(GroupSpecification.filterBy(filters)),
        pageable);
  }

  public Page<Group> findGroupsForUser(
      @NonNull UUID userId,
      @NonNull String query,
      @NonNull List<SearchFilter> filters,
      @NonNull Pageable pageable) {
    userService.checkExistence(userId);
    return groupService.findAll(
        where(GroupSpecification.containsUser(userId))
            .and(GroupSpecification.containsText(query))
            .and(GroupSpecification.filterBy(filters)),
        pageable);
  }

  public Page<Group> listGroupsForUser(@NonNull UUID userId, @NonNull Pageable pageable) {
    return findGroupsForUser(userId, ImmutableList.of(), pageable);
  }

  public Page<User> listUsersForGroup(@NonNull UUID groupId, @NonNull Pageable pageable) {
    return findUsersForGroup(groupId, ImmutableList.of(), pageable);
  }

  public Page<User> findUsersForGroup(
      @NonNull UUID groupId, @NonNull List<SearchFilter> filters, @NonNull Pageable pageable) {
    groupService.checkExistence(groupId);
    return userService.findAll(
        where(UserSpecification.inGroup(groupId)).and(UserSpecification.filterBy(filters)),
        pageable);
  }

  public Page<User> findUsersForGroup(
      @NonNull UUID groupId,
      @NonNull String query,
      @NonNull List<SearchFilter> filters,
      @NonNull Pageable pageable) {
    groupService.checkExistence(groupId);
    return userService.findAll(
        where(UserSpecification.inGroup(groupId))
            .and(UserSpecification.containsText(query))
            .and(UserSpecification.filterBy(filters)),
        pageable);
  }

  public Page<User> findUsersForGroup(@NonNull FindRequest findRequest) {
    groupService.checkExistence(findRequest.getId());
    val spec = buildFindUsersByGroupSpecification(findRequest);
    return userService.findAll(spec, findRequest.getPageable());
  }

  public Page<Group> findGroupsForUser(@NonNull FindRequest findRequest) {
    userService.checkExistence(findRequest.getId());
    val spec = buildFindGroupsByUserSpecification(findRequest);
    return groupService.findAll(spec, findRequest.getPageable());
  }

  private static Specification<Group> buildFindGroupsByUserSpecification(
      @NonNull FindRequest findRequest) {
    val baseSpec =
        where(GroupSpecification.containsUser(findRequest.getId()))
            .and(GroupSpecification.filterBy(findRequest.getFilters()));
    return findRequest
        .getQuery()
        .map(q -> baseSpec.and(GroupSpecification.containsText(q)))
        .orElse(baseSpec);
  }

  private static Specification<User> buildFindUsersByGroupSpecification(
      @NonNull FindRequest findRequest) {
    val baseSpec =
        where(UserSpecification.inGroup(findRequest.getId()))
            .and(UserSpecification.filterBy(findRequest.getFilters()));
    return findRequest
        .getQuery()
        .map(q -> baseSpec.and(UserSpecification.containsText(q)))
        .orElse(baseSpec);
  }
}
