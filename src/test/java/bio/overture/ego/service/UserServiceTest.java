package bio.overture.ego.service;

import static bio.overture.ego.model.enums.AccessLevel.WRITE;
import static bio.overture.ego.utils.CollectionUtils.repeatedCallsOf;
import static java.util.stream.Collectors.toList;

import bio.overture.ego.utils.EntityGenerator;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@SpringBootTest
@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@Transactional
public class UserServiceTest {

  @Autowired private UserService userService;
  @Autowired private EntityGenerator entityGenerator;
  @Autowired private RefreshTokenService refreshTokenService;
  @Autowired private TokenService tokenService;

  @Test
  public void testGetManyUsersWithRelations() {
    int numUsers = 3;

    val users = repeatedCallsOf(() -> entityGenerator.generateRandomUser(), numUsers);

    // Create a group and add it to first few users
    val group = entityGenerator.setupGroup("UserServiceTestGroup");
    val groupWithUsers = entityGenerator.addUsersToGroup(users.stream().collect(toList()), group);

    // Create application and add it to last few users
    val application = entityGenerator.setupApplication("UserServiceTestApplicaiton");
    entityGenerator.addUsersToApplication(users.stream().collect(toList()), application);

    // Create policy and add permission to users
    val policy = entityGenerator.setupSinglePolicy("UserServiceTestPolicy");
    entityGenerator.setupGroupPermission(groupWithUsers, policy, WRITE);

    // Update testUsers now that they have relations
    val reply =
        userService.getMany(
            users.stream().map(user -> user.getId()).collect(toList()), true, true, true);

    Assert.assertEquals(reply.size(), numUsers);

    reply.forEach(
        user -> {
          Assert.assertEquals(user.getUserGroups().size(), 1);
          Assert.assertEquals(
              user.getUserGroups().iterator().next().getGroup().getName(), group.getName());

          Assert.assertEquals(user.getUserApplications().size(), 1);
          Assert.assertEquals(
              user.getUserApplications().iterator().next().getApplication().getName(),
              application.getName());

          Assert.assertEquals(user.getPermissions().size(), 1);
          Assert.assertEquals(
              user.getPermissions().iterator().next(), "UserServiceTestPolicy.WRITE");
        });
  }

  @Test
  public void testAssociateUserWithRefreshToken() {

    val user1 = entityGenerator.setupUser("Homer Simpson");
    val user1Token = tokenService.generateUserToken(user1);

    val refreshToken1 = refreshTokenService.createRefreshToken(user1Token);

    val userWithRefreshToken = userService.get(user1.getId(), false, false, false, true);

    Assert.assertTrue(userWithRefreshToken.getRefreshToken() != null);
    Assert.assertEquals(userWithRefreshToken.getRefreshToken(), refreshToken1);
    Assert.assertEquals(userWithRefreshToken.getId(), user1.getId());
  }
}
