package bio.overture.ego.utils;

import static bio.overture.ego.model.enums.ApplicationType.ADMIN;
import bio.overture.ego.model.enums.ApplicationType;
import org.springframework.security.test.context.support.WithSecurityContext;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithMockCustomApplicationSecurityContextFactory.class)
public @interface WithMockCustomApplication {

  String name() default "Admin Security App";
  String clientId() default "Admin-Security-APP-ID";
  String clientSecret() default "Admin-Security-APP-Secret";
  String redirectUri() default "mock.com";
  String description() default "Mock Application";
  ApplicationType type() default ADMIN;

}
