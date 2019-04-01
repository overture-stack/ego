package bio.overture.ego.utils;

import static bio.overture.ego.model.enums.StatusType.APPROVED;

import bio.overture.ego.model.dto.CreateApplicationRequest;
import bio.overture.ego.model.entity.Application;
import bio.overture.ego.service.ApplicationService;
import java.util.ArrayList;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

public class WithMockCustomApplicationSecurityContextFactory
    implements WithSecurityContextFactory<WithMockCustomApplication> {

  @Autowired private ApplicationService applicationService;

  @Override
  public SecurityContext createSecurityContext(WithMockCustomApplication customApplication) {
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    val principal = setupApplication(customApplication);
    Authentication auth =
        new UsernamePasswordAuthenticationToken(principal, null, new ArrayList<>());
    context.setAuthentication(auth);
    return context;
  }

  private Application setupApplication(WithMockCustomApplication customApplication) {
    return applicationService
        .findByClientId(customApplication.clientId())
        .orElseGet(
            () -> {
              val request = createApplicationCreateRequest(customApplication);
              return applicationService.create(request);
            });
  }

  private CreateApplicationRequest createApplicationCreateRequest(
      WithMockCustomApplication customApplication) {
    return CreateApplicationRequest.builder()
        .name(customApplication.clientId())
        .type(customApplication.type())
        .clientId(customApplication.clientId())
        .clientSecret(customApplication.clientSecret())
        .status(APPROVED)
        .redirectUri(customApplication.redirectUri())
        .description(customApplication.description())
        .build();
  }
}
