package bio.overture.ego.utils;

import static bio.overture.ego.model.enums.UserType.ADMIN;

import bio.overture.ego.model.enums.UserType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import org.springframework.security.test.context.support.WithSecurityContext;

@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithMockCustomUserSecurityContextFactory.class)
public @interface WithMockCustomUser {

  String firstName() default "Admin";

  String lastName() default "User";

  UserType type() default ADMIN;
}
