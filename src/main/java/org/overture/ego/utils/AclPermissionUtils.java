package org.overture.ego.utils;

import org.overture.ego.model.entity.AclPermission;
import org.overture.ego.model.enums.AclMask;

import java.util.List;
import java.util.stream.Collectors;

public class AclPermissionUtils {
  public static String extractPermissionString(AclPermission permission) {
    return String.format("%s.%s", permission.getEntity().getName(), permission.getMask().toString());
  }

  public static List<String> extractPermissionStrings(List<? extends AclPermission> permissions) {
    return permissions.stream().map(AclPermissionUtils::extractPermissionString)
        .collect(Collectors.toList());
  }
}
