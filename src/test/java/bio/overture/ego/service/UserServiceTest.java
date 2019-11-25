package bio.overture.ego.service;

import bio.overture.ego.repository.RefreshTokenRepository;
import bio.overture.ego.repository.UserRepository;
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

import static bio.overture.ego.model.enums.AccessLevel.WRITE;
import static bio.overture.ego.utils.CollectionUtils.repeatedCallsOf;
import static java.util.stream.Collectors.toList;

@Slf4j
@SpringBootTest
@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@Transactional
public class UserServiceTest {

  @Autowired private UserService userService;
  @Autowired private EntityGenerator entityGenerator;

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

  @Autowired private UserRepository userRepository;

  @Autowired private RefreshTokenRepository refreshTokenRepository;

  @Test
  public void testAssociateUserWithRefreshToken() {
    // create a user
    val user1 = entityGenerator.setupUser("Jimmy Hoffa");
    // create a refresh token
    val refreshToken1 = entityGenerator.setupRefreshToken(user1);
    // set user on refresh token
    refreshToken1.setUser(user1);
    log.info("set user1 on refresh token");
    // set refresh token on user - are these sets just the one to one assoc?
    user1.setRefreshToken(refreshToken1);
    log.info("set refresh token on user1");
    userRepository.save(user1);
    log.info("finished saving user1");
    userService.get(user1.getId(), false, false, false, true);
    log.info("finished");

    val userWithRefreshToken = userService.get(user1.getId(), false, false, false, true);
    Assert.assertTrue(userWithRefreshToken.getRefreshToken() != null);
    Assert.assertEquals(userWithRefreshToken.getRefreshToken(), refreshToken1);
    Assert.assertEquals(userWithRefreshToken.getId(), user1.getId());
  }
}
