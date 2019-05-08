package bio.overture.ego.grpc.service;

import static bio.overture.ego.utils.CollectionUtils.repeatedCallsOf;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

import bio.overture.ego.grpc.GetUserRequest;
import bio.overture.ego.grpc.ListUsersRequest;
import bio.overture.ego.grpc.PagedRequest;
import bio.overture.ego.grpc.UserServiceGrpc;
import bio.overture.ego.model.entity.Application;
import bio.overture.ego.model.entity.Group;
import bio.overture.ego.model.entity.User;
import bio.overture.ego.model.enums.AccessLevel;
import bio.overture.ego.service.UserService;
import bio.overture.ego.utils.EntityGenerator;
import io.grpc.Channel;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;
import java.io.IOException;
import java.util.Collection;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@Slf4j
@ActiveProfiles("test")
@SpringBootTest
@RunWith(SpringRunner.class)
public class UserServiceGrpcTest {

  private String serverName;

  private Channel channel;

  private UserServiceGrpc.UserServiceBlockingStub stub;

  // Test Data
  private static boolean hasRunEntitySetup = false;
  private static Collection<User> testUsers;
  private static Group groupWithUsers;
  private static Application appWithUsers;
  private static Collection<User> usersWithGroup;

  @Autowired private EntityGenerator entityGenerator;
  @Autowired UserServiceGrpcImpl userServiceGrpc;
  @Autowired UserService userService;

  @Rule public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

  @Before
  public void setUpInProcessGrpc() throws IOException {

    // Generate a unique in-process server name.
    serverName = InProcessServerBuilder.generateName();
    // Create a client channel and register for automatic graceful shutdown.
    channel =
        grpcCleanup.register(InProcessChannelBuilder.forName(serverName).directExecutor().build());

    // Create a server, add service, start, and register for automatic graceful shutdown.
    grpcCleanup.register(
        InProcessServerBuilder.forName(serverName)
            .directExecutor()
            .addService(userServiceGrpc)
            .build()
            .start());

    stub = UserServiceGrpc.newBlockingStub(channel);
  }

  @Before
  public void setUpTestData() {
    if (!hasRunEntitySetup) {
      // Lets add several users to test list functionality
      val users = repeatedCallsOf(() -> entityGenerator.generateRandomUser(), 7);

      // Create a group and add it to first few users
      val group = entityGenerator.setupGroup("GrpcUserTestGroup");
      usersWithGroup = users.stream().limit(4).collect(toList());
      groupWithUsers = entityGenerator.addUsersToGroup(usersWithGroup, group);

      // Create application and add it to last few users
      val application = entityGenerator.setupApplication("GrpcUserTestApplicaiton");
      appWithUsers =
          entityGenerator.addUsersToApplication(
              users.stream().skip(3).collect(toList()), application);

      // Create policy and add permission to users
      val groupPolicy = entityGenerator.setupSinglePolicy("GrpcUserTestPolicyForGroup");
      entityGenerator.setupGroupPermission(groupWithUsers, groupPolicy, AccessLevel.WRITE);

      val userPolicy = entityGenerator.setupSinglePolicy("GrpcUserTestPolicyForUser");
      entityGenerator.addPermissionToUsers(users, userPolicy, AccessLevel.READ);

      // Update testUsers now that they have relations
      testUsers =
          userService.getMany(
              users.stream().map(user -> user.getId()).collect(toList()), true, true, true);

      hasRunEntitySetup = true;
    }
  }

  @Test
  public void getUser_success() {

    // Test with the user that has a group and application
    val testUser =
        testUsers.stream()
            .filter(
                user -> user.getUserGroups().size() > 0 && user.getUserApplications().size() > 0)
            .findFirst()
            .get();

    val reply = stub.getUser(GetUserRequest.newBuilder().setId(testUser.getId().toString()).build());

    // Ensure all fields populated and value matches expected
    assertThat(reply.getId().getValue()).isEqualTo(testUser.getId().toString());
    assertThat(reply.getFirstName().getValue()).isEqualTo(testUser.getFirstName());
    assertThat(reply.getLastName().getValue()).isEqualTo(testUser.getLastName());
    assertThat(reply.getEmail().getValue()).isEqualTo(testUser.getEmail());
    assertThat(reply.getStatus().getValue()).isEqualTo(testUser.getStatus().toString());
    assertThat(reply.getPreferredLanguage().getValue())
        .isEqualTo(testUser.getPreferredLanguage().toString());
    assertThat(reply.getType().getValue()).isEqualTo(testUser.getType().toString());

    assertThat(reply.hasCreatedAt()).isTrue();
    assertThat(reply.hasLastLogin()).isTrue();

    assertThat(reply.getApplicationsList().size()).isEqualTo(1);
    assertThat(reply.getGroupsList().size()).isEqualTo(1);
    assertThat(reply.getScopesList().size()).isEqualTo(2);
  }

  @Test
  public void listUser_emptyRequest() {
    val request = ListUsersRequest.newBuilder().build();

    val reply = stub.listUsers(request);

    // Ensure response includes pagination data
    assertThat(reply.hasPage()).isTrue();
    assertThat(reply.getPage().getMaxResults()).isGreaterThanOrEqualTo(7);

    // Make sure we got users in the response (quick sanity check, not in depth)
    assertThat(reply.getUsersCount()).isGreaterThanOrEqualTo(7);
    val user = reply.getUsers(0);
    assertThat(user.hasId()).isTrue();
  }

  @Test
  public void listUser_pagedRequests() {
    val pagedRequest1 = PagedRequest.newBuilder().setPageNumber(0).setPageSize(2);
    val request1 = ListUsersRequest.newBuilder().setPage(pagedRequest1).build();

    val reply1 = stub.listUsers(request1);

    // Correct number of users
    assertThat(reply1.getUsersCount()).isEqualTo(2);

    // Correct pagination info
    assertThat(reply1.hasPage()).isTrue();
    assertThat(reply1.getPage().getMaxResults()).isGreaterThanOrEqualTo(7);
    assertThat(reply1.getPage().hasNextPage()).isTrue();
    assertThat(reply1.getPage().getNextPage().getValue()).isEqualTo(1);

    val user1 = reply1.getUsers(0);

    val pagedRequest2 =
        PagedRequest.newBuilder()
            .setPageNumber(reply1.getPage().getNextPage().getValue())
            .setPageSize(2);
    val request2 = ListUsersRequest.newBuilder().setPage(pagedRequest2).build();

    val reply2 = stub.listUsers(request2);

    // Correct pagination info
    assertThat(reply2.hasPage()).isTrue();
    assertThat(reply2.getPage().getMaxResults()).isGreaterThanOrEqualTo(7);
    assertThat(reply2.getPage().getNextPage().getValue()).isEqualTo(2);

    // different user (ensure we're not repeating user blocks)
    val user2 = reply2.getUsers(0);
    assertThat(user1.getId()).isNotEqualTo(user2.getId());
  }

  @Test
  public void listUser_largePageRequest() {
    val pagedRequest = PagedRequest.newBuilder().setPageNumber(0).setPageSize(1000000);
    val request = ListUsersRequest.newBuilder().setPage(pagedRequest).build();

    val reply = stub.listUsers(request);

    // Correct number of users
    assertThat(reply.getUsersCount()).isGreaterThanOrEqualTo(7);
    assertThat(reply.getUsersCount()).isLessThanOrEqualTo(1000);
  }

  @Test
  public void listUser_nonExistentPageNumberRequest() {
    val pagedRequest = PagedRequest.newBuilder().setPageNumber(100000).setPageSize(1000000);
    val request = ListUsersRequest.newBuilder().setPage(pagedRequest).build();

    val reply = stub.listUsers(request);

    // Correct number of users
    assertThat(reply.getUsersCount()).isEqualTo(0);
    assertThat(reply.getPage().hasNextPage()).isFalse();
  }
}
