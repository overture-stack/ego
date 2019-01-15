package bio.overture.ego.utils;

import bio.overture.ego.model.entity.Application;
import bio.overture.ego.model.entity.Group;
import bio.overture.ego.model.entity.User;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class EntityTools {
  public static List<UUID> extractGroupIds(Set<Group> entities) {
    return entities.stream().map(Group::getId).collect(java.util.stream.Collectors.toList());
  }

  public static List<String> extractGroupNames(List<Group> entities) {
    return entities.stream().map(Group::getName).collect(java.util.stream.Collectors.toList());
  }

  public static List<UUID> extractUserIds(Set<User> entities) {
    return entities.stream().map(User::getId).collect(java.util.stream.Collectors.toList());
  }

  public static List<UUID> extractAppIds(Set<Application> entities) {
    return entities.stream().map(Application::getId).collect(Collectors.toList());
  }
}
