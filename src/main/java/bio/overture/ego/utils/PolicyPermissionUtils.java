package bio.overture.ego.utils;

import bio.overture.ego.model.entity.AbstractPermission;
import lombok.NonNull;

import java.util.Collection;
import java.util.List;

import static bio.overture.ego.utils.CollectionUtils.mapToList;

public class PolicyPermissionUtils {

  public static String extractPermissionString(@NonNull AbstractPermission permission) {
    return String.format(
        "%s.%s", permission.getPolicy().getName(), permission.getAccessLevel().toString());
  }

  public static List<String> extractPermissionStrings(@NonNull Collection<? extends AbstractPermission> permissions) {
    return mapToList(permissions, PolicyPermissionUtils::extractPermissionString);
  }

}
