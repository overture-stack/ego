package org.overture.ego.utils;

import org.overture.ego.model.entity.Permission;

import java.util.List;
import java.util.stream.Collectors;

public class AclPermissionUtils {
  public static String extractPermissionString(Permission permission) {
    return String.format("%s.%s", permission.getEntity().getName(), permission.getMask().toString());
  }

  public static List<String> extractPermissionStrings(List<? extends Permission> permissions) {
    return permissions.stream().map(AclPermissionUtils::extractPermissionString)
        .collect(Collectors.toList());
  }
}
