package org.overture.ego.utils;

import org.overture.ego.model.entity.Permission;

import java.util.List;
import java.util.stream.Collectors;

public class AclPermissionUtils {
  public static String extractPermissionString(Permission scope) {
    return String.format("%s.%s", scope.getEntity().getName(), scope.getMask().toString());
  }

  public static List<String> extractPermissionStrings(List<? extends Permission> permissions) {
    return permissions.stream().map(AclPermissionUtils::extractPermissionString)
        .collect(Collectors.toList());
  }
}
