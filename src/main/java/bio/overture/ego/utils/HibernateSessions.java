package bio.overture.ego.utils;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.hibernate.collection.internal.AbstractPersistentCollection;

@Slf4j
public class HibernateSessions {

  public static void unsetSession(@NonNull Set property) {
    unsetSession((Collection) property);
  }

  public static void unsetSession(@NonNull List property) {
    unsetSession((Collection) property);
  }

  public static void unsetSession(@NonNull Collection property) {
    if (property instanceof AbstractPersistentCollection) {
      val persistentProperty = (AbstractPersistentCollection) property;
      persistentProperty.unsetSession(persistentProperty.getSession());
    }
  }
}
