package bio.overture.ego.utils;

import java.util.List;
import java.util.Set;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.hibernate.collection.internal.AbstractPersistentCollection;

@Slf4j
public class HibernateSessions {

  public static void unsetSession(@NonNull Set property) {
    if (property instanceof AbstractPersistentCollection) {
      val persistentProperty = (AbstractPersistentCollection) property;
      persistentProperty.unsetSession(persistentProperty.getSession());
    }
  }

  public static void unsetSession(@NonNull List property) {
    if (property instanceof AbstractPersistentCollection) {
      val persistentProperty = (AbstractPersistentCollection) property;
      persistentProperty.unsetSession(persistentProperty.getSession());
    }
  }
}
