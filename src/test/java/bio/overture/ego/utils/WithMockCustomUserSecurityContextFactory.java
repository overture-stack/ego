package bio.overture.ego.utils;

import static bio.overture.ego.model.enums.LanguageType.ENGLISH;
import static bio.overture.ego.model.enums.StatusType.APPROVED;
import bio.overture.ego.model.dto.CreateUserRequest;
import bio.overture.ego.model.entity.User;
import bio.overture.ego.service.UserService;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;
import java.util.ArrayList;


public class WithMockCustomUserSecurityContextFactory
        implements WithSecurityContextFactory<WithMockCustomUser> {

  @Autowired
  private UserService userService;

  @Override
  public SecurityContext createSecurityContext(WithMockCustomUser customUser) {
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    val principal = setupUser(customUser.firstName() + " " + customUser.lastName(), customUser);
    Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, new ArrayList<>());
    context.setAuthentication(auth);
    return context;
  }

  private User setupUser(String name, WithMockCustomUser customUser) {
    val names = name.split(" ", 2);
    val userName = String.format("%s%s@domain.com", names[0], names[1]);
    return userService
            .findByName(userName)
            .orElseGet(
                    () -> {
                      val createUserRequest = createUser(userName, customUser);
                      return userService.create(createUserRequest);
                    });
  }

  private CreateUserRequest createUser(String userName, WithMockCustomUser customUser){
    return  CreateUserRequest.builder()
            .email(userName)
            .firstName(customUser.firstName())
            .lastName(customUser.lastName())
            .status(APPROVED)
            .preferredLanguage(ENGLISH)
            .type(customUser.type())
            .build();
  }

}
