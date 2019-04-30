package bio.overture.ego.utils;

import static bio.overture.ego.utils.Collectors.toImmutableList;
import static bio.overture.ego.utils.Collectors.toImmutableSet;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.Objects.isNull;
import static lombok.AccessLevel.PRIVATE;

import bio.overture.ego.model.entity.Application;
import bio.overture.ego.model.entity.Group;
import bio.overture.ego.model.entity.Identifiable;
import bio.overture.ego.model.entity.User;
import bio.overture.ego.model.join.GroupApplication;
import bio.overture.ego.model.join.GroupApplicationId;
import bio.overture.ego.model.join.UserApplication;
import bio.overture.ego.model.join.UserApplicationId;
import bio.overture.ego.model.join.UserGroup;
import bio.overture.ego.model.join.UserGroupId;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.val;

@NoArgsConstructor(access = PRIVATE)
public class Converters {

  public static List<UUID> convertToUUIDList(Collection<String> uuids) {
    return uuids.stream().map(UUID::fromString).collect(toImmutableList());
  }

  public static Set<UUID> convertToUUIDSet(Collection<String> uuids) {
    return uuids.stream().map(UUID::fromString).collect(toImmutableSet());
  }

  public static <ID, T extends Identifiable<ID>> Set<ID> convertToIds(Collection<T> entities) {
    return entities.stream().map(Identifiable::getId).collect(toImmutableSet());
  }

  public static <T> List<T> nullToEmptyList(List<T> list) {
    if (isNull(list)) {
      return newArrayList();
    } else {
      return list;
    }
  }

  public static <T> Set<T> nullToEmptySet(Set<T> set) {
    if (isNull(set)) {
      return newHashSet();
    } else {
      return set;
    }
  }

  public static <T> Collection<T> nullToEmptyCollection(Collection<T> collection) {
    if (isNull(collection)) {
      return newHashSet();
    } else {
      return collection;
    }
  }

  /**
   * If {@param nullableValue} is non-null, then the {@param consumer} will accept it, otherwise,
   * nothing.
   */
  public static <V> void nonNullAcceptor(V nullableValue, @NonNull Consumer<V> consumer) {
    if (!isNull(nullableValue)) {
      consumer.accept(nullableValue);
    }
  }

  public static UserApplication convertToUserApplication(@NonNull User u, @NonNull Application a) {
    val id = UserApplicationId.builder().applicationId(a.getId()).userId(u.getId()).build();
    return UserApplication.builder().id(id).user(u).application(a).build();
  }

  public static UserGroup convertToUserGroup(@NonNull User u, @NonNull Group g) {
    val id = UserGroupId.builder().groupId(g.getId()).userId(u.getId()).build();
    return UserGroup.builder().id(id).user(u).group(g).build();
  }

  public static GroupApplication convertToGroupApplication(
      @NonNull Group g, @NonNull Application a) {
    val id = GroupApplicationId.builder().applicationId(a.getId()).groupId(g.getId()).build();
    return GroupApplication.builder().id(id).group(g).application(a).build();
  }
}
