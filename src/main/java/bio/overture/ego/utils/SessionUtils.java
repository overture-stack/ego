package bio.overture.ego.utils;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.collection.internal.AbstractPersistentCollection;
import java.util.List;
import java.util.Set;

@Slf4j
public class SessionUtils {
    public static void unsetSession(Set property){
        if(property instanceof AbstractPersistentCollection){
            AbstractPersistentCollection persistentProperty = (AbstractPersistentCollection)property;
            persistentProperty.unsetSession(persistentProperty.getSession());
        }
    }

    public static void unsetSession(List property){
        if(property instanceof AbstractPersistentCollection){
            AbstractPersistentCollection persistentProperty = (AbstractPersistentCollection)property;
            persistentProperty.unsetSession(persistentProperty.getSession());
        }
    }
}
