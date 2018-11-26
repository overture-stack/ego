package org.overture.ego.utils;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.collection.internal.AbstractPersistentCollection;

@Slf4j
public class SessionUtils {
    public static void unsetSession(AbstractPersistentCollection property){
        if(property.getSession() != null){
            property.unsetSession(property.getSession());
        }
    }
}
