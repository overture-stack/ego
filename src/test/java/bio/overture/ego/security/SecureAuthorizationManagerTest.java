package bio.overture.ego.security;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import bio.overture.ego.model.entity.Application;
import bio.overture.ego.model.entity.User;
import bio.overture.ego.model.enums.ApplicationType;
import bio.overture.ego.model.enums.StatusType;
import bio.overture.ego.model.enums.UserType;
import lombok.val;
import org.junit.Test;
import org.springframework.security.core.Authentication;

public class SecureAuthorizationManagerTest {
  SecureAuthorizationManager manager = new SecureAuthorizationManager();

  @Test
  public void testAuthorize() {
    val auth = mock(Authentication.class);

    val user = new User();
    // Users with UserType Admin will be authorized by authorizedWithAdminRole():
    // user type authorization should fail for them.
    user.setType(UserType.USER);
    user.setStatus(StatusType.APPROVED);

    when(auth.getPrincipal()).thenReturn(user);
    assertTrue("Approved user is authorized", manager.authorize(auth));

    for (StatusType type : StatusType.values()) {
      user.setStatus(type);
      if (type == StatusType.APPROVED) {
        assertTrue("Approved user is authorized", manager.authorize(auth));
      } else {
        assertFalse("User in with status " + type + " denied", manager.authorize(auth));
      }
    }
    user.setType(UserType.ADMIN);
    assertFalse("Admin user shouldn't be authorized", manager.authorize(auth));
  }

  @Test
  public void testAuthorizeWithAdminRole() {
    val auth = mock(Authentication.class);

    val user = new User();
    user.setType(UserType.ADMIN);
    user.setStatus(StatusType.APPROVED);
    when(auth.getPrincipal()).thenReturn(user);

    for (StatusType type : StatusType.values()) {
      user.setStatus(type);
      if (type == StatusType.APPROVED) {
        assertTrue("Approved admin is authorized", manager.authorizeWithAdminRole(auth));
      } else {
        assertFalse(
            "User in with status " + type + " denied", manager.authorizeWithAdminRole(auth));
      }
    }

    user.setType(UserType.ADMIN);
    assertFalse("Non-admin User isn't authorized", manager.authorizeWithAdminRole(auth));

    val app = new Application();
    app.setName("TestApp");
    app.setType(ApplicationType.ADMIN);
    when(auth.getPrincipal()).thenReturn(app);
    assertTrue(manager.authorizeWithAdminRole(auth));
  }

  @Test
  public void testAuthorizeWithApplication() {
    val auth = mock(Authentication.class);
    val app = new Application();
    app.setName("TestApp");
    when(auth.getPrincipal()).thenReturn(app);
    assertTrue(manager.authorizeWithApplication(auth));
  }
}
