package bio.overture.ego.utils;

import static bio.overture.ego.model.exceptions.MalformedRequestException.checkMalformedRequest;
import static bio.overture.ego.utils.CollectionUtils.findDuplicates;
import static bio.overture.ego.utils.Joiners.PRETTY_COMMA;
import static lombok.AccessLevel.PRIVATE;

import bio.overture.ego.model.entity.Identifiable;
import java.util.Collection;
import java.util.UUID;
import lombok.NoArgsConstructor;
import lombok.val;

@NoArgsConstructor(access = PRIVATE)
public class Ids {

  public static <T extends Identifiable<UUID>> void checkDuplicates(
      Class<T> entityType, Collection<UUID> ids) {
    // check duplicate ids
    val duplicateIds = findDuplicates(ids);
    checkMalformedRequest(
        duplicateIds.isEmpty(),
        "The following %s ids contain duplicates: [%s]",
        entityType.getSimpleName(),
        PRETTY_COMMA.join(duplicateIds));
  }
}
