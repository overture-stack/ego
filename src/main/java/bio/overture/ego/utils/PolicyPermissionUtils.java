package bio.overture.ego.utils;

import static bio.overture.ego.utils.CollectionUtils.mapToList;

import bio.overture.ego.model.entity.Permission;
import java.util.List;
import java.util.Set;

public class PolicyPermissionUtils {
  public static String extractPermissionString(Permission permission) {
    return String.format(
        "%s.%s", permission.getPolicy().getName(), permission.getAccessLevel().toString());
  }

  public static List<String> extractPermissionStrings(Set<? extends Permission> permissions) {
    return mapToList(permissions, PolicyPermissionUtils::extractPermissionString);
  }

  // TODO - Maybe temporary if we are getting rid of List<Type> usage
  public static List<String> extractPermissionStrings(List<? extends Permission> permissions) {
    return mapToList(permissions, PolicyPermissionUtils::extractPermissionString);
  }
}
