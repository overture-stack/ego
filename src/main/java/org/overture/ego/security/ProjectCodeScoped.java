package org.overture.ego.security;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Method Security Meta Annotation
 */
@Retention(RetentionPolicy.RUNTIME)
//@PreAuthorize("@authorizationManager.authorize(authentication)")
public @interface ProjectCodeScoped {
}
