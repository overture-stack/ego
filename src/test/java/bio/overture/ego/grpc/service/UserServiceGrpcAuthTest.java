package bio.overture.ego.grpc.service;

import static bio.overture.ego.utils.EntityGenerator.generateNonExistentId;
import static io.grpc.Metadata.ASCII_STRING_MARSHALLER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.Assert.fail;

import bio.overture.ego.grpc.GetUserRequest;
import bio.overture.ego.grpc.ListUsersRequest;
import bio.overture.ego.grpc.UserServiceGrpc;
import bio.overture.ego.grpc.interceptor.AuthInterceptor;
import bio.overture.ego.model.dto.CreateApplicationRequest;
import bio.overture.ego.model.dto.CreateUserRequest;
import bio.overture.ego.model.entity.Application;
import bio.overture.ego.model.entity.User;
import bio.overture.ego.model.enums.ApplicationType;
import bio.overture.ego.model.enums.StatusType;
import bio.overture.ego.model.enums.UserType;
import bio.overture.ego.service.ApplicationService;
import bio.overture.ego.service.TokenService;
import bio.overture.ego.service.UserService;
import io.grpc.Channel;
import io.grpc.Metadata;
import io.grpc.ServerInterceptors;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.MetadataUtils;
import io.grpc.testing.GrpcCleanupRule;
import java.io.IOException;
import java.util.UUID;
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
@ActiveProfiles({"test", "auth"})
@SpringBootTest
@RunWith(SpringRunner.class)
public class UserServiceGrpcAuthTest {

  private static String serverName;
  private static Channel channel;
  private static UserServiceGrpc.UserServiceBlockingStub stub;
  private static boolean hasRunSetup = false;

  private static Metadata userAuthMeta = new Metadata();
  private static Metadata userAdminAuthMeta = new Metadata();
  private static Metadata appAuthMeta = new Metadata();
  private static Metadata emptyAuthMeta = new Metadata();
  private static Metadata.Key<String> JWT_KEY = Metadata.Key.of("jwt", ASCII_STRING_MARSHALLER);
  private static User testUser;
  private static User testAdmin;
  private static Application testApp;

  @Autowired private AuthInterceptor authInterceptor;
  @Autowired private UserServiceGrpcImpl userServiceGrpc;
  @Autowired private UserService userService;
  @Autowired private ApplicationService appService;
  @Autowired private TokenService tokenService;

  @Rule public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

  @Before
  public void init() throws IOException {
    grpcSetup();
    testDataSetup();
  }

  public void grpcSetup() throws IOException {

    serverName = InProcessServerBuilder.generateName();
    // Create a client channel and register for automatic graceful shutdown.
    channel =
        grpcCleanup.register(InProcessChannelBuilder.forName(serverName).directExecutor().build());

    // Create a server, add service with auth interceptor, start, and register for automatic
    // graceful shutdown.
    grpcCleanup.register(
        InProcessServerBuilder.forName(serverName)
            .directExecutor()
            .addService(ServerInterceptors.intercept(userServiceGrpc, authInterceptor))
            .build()
            .start());

    stub = UserServiceGrpc.newBlockingStub(channel);
  }

  public void testDataSetup() {
    if (!hasRunSetup) {
      hasRunSetup = true;
      // Setup test data and meta data for auth
      testUser =
          userService.create(
              CreateUserRequest.builder()
                  .status(StatusType.APPROVED)
                  .email("approvedUserGrpc@example.com")
                  .type(UserType.USER)
                  .build());
      userAuthMeta.put(JWT_KEY, tokenService.generateUserToken(testUser));

      testAdmin =
          userService.create(
              CreateUserRequest.builder()
                  .status(StatusType.APPROVED)
                  .email("approvedAdminGrpc@example.com")
                  .type(UserType.ADMIN)
                  .build());
      userAdminAuthMeta.put(JWT_KEY, tokenService.generateUserToken(testAdmin));

      testApp =
          appService.create(
              CreateApplicationRequest.builder()
                  .status(StatusType.APPROVED)
                  .type(ApplicationType.CLIENT)
                  .clientId("grpctest")
                  .clientSecret("grpctestsecret")
                  .redirectUri("http://test.example.com")
                  .name("grpctest")
                  .build());
      appAuthMeta.put(JWT_KEY, tokenService.generateAppToken(testApp));
    }
  }

  @Test
  public void getUser_noAuth_rejected() {

    val noAuthStub = MetadataUtils.attachHeaders(stub, emptyAuthMeta);

    // Test that the interceptor rejects this request
    assertThatExceptionOfType(StatusRuntimeException.class)
        .as("Request should be rejected due to missing JWT")
        .isThrownBy(() -> noAuthStub.getUser(GetUserRequest.newBuilder().setId(UUID.randomUUID().toString()).build()));
  }

  @Test
  public void getUser_userAuth_success() {

    val authStub = MetadataUtils.attachHeaders(stub, userAuthMeta);

    // Test that the interceptor rejects this request
    try {
      val reply =
          authStub.getUser(GetUserRequest.newBuilder().setId(testUser.getId().toString()).build());
      assertThat(reply.getId().getValue()).isEqualTo(testUser.getId().toString());

    } catch (StatusRuntimeException e) {
      fail("User disallowed to access their ego data via grpc.");
    }
  }

  @Test
  public void getUser_userAuth_rejectedForWrongUser() {

    val authStub = MetadataUtils.attachHeaders(stub, userAuthMeta);
    UUID randomId = generateNonExistentId(userService);

    // Test that the interceptor rejects this request
    assertThatExceptionOfType(StatusRuntimeException.class)
        .as("User should not be allowed to access data of a different user.")
        .isThrownBy(() -> authStub.getUser(GetUserRequest.newBuilder().setId(randomId.toString()).build()));
  }

  @Test
  public void getUser_adminAuth_success() {
    val authStub = MetadataUtils.attachHeaders(stub, userAdminAuthMeta);

    // Test that the interceptor rejects this request
    try {
      val reply =
          authStub.getUser(GetUserRequest.newBuilder().setId(testUser.getId().toString()).build());
      assertThat(reply.getId().getValue()).isEqualTo(testUser.getId().toString());

    } catch (StatusRuntimeException e) {
      fail("Admin disallowed to access ego user data.");
    }
  }

  @Test
  public void getUser_appAuth_success() {
    val authStub = MetadataUtils.attachHeaders(stub, appAuthMeta);

    // Test that the interceptor rejects this request
    try {
      val reply =
          authStub.getUser(GetUserRequest.newBuilder().setId(testUser.getId().toString()).build());
      assertThat(reply.getId().getValue()).isEqualTo(testUser.getId().toString());

    } catch (StatusRuntimeException e) {
      fail("App disallowed to access ego user data.");
    }
  }

  @Test
  public void listUsers_noAuth_rejected() {
    val authStub = MetadataUtils.attachHeaders(stub, emptyAuthMeta);

    // Test that the interceptor rejects this request
    assertThatExceptionOfType(StatusRuntimeException.class)
        .as("Request should be rejected due to missing JWT")
        .isThrownBy(() -> authStub.listUsers(ListUsersRequest.newBuilder().build()));
  }

  @Test
  public void listUsers_userAuth_rejected() {

    val authStub = MetadataUtils.attachHeaders(stub, userAuthMeta);

    // Test that the interceptor rejects this request
    assertThatExceptionOfType(StatusRuntimeException.class)
        .as("Request should be rejected due to missing JWT")
        .isThrownBy(() -> authStub.listUsers(ListUsersRequest.newBuilder().build()));
  }

  @Test
  public void listUsers_adminAuth_success() {
    val authStub = MetadataUtils.attachHeaders(stub, userAdminAuthMeta);

    // Test that the interceptor rejects this request
    try {
      val reply = authStub.listUsers(ListUsersRequest.newBuilder().build());
      assertThat(reply.getUsersCount()).isGreaterThanOrEqualTo(2);

    } catch (StatusRuntimeException e) {
      fail("Admin disallowed to access list user service.");
    }
  }

  @Test
  public void listUsers_appAuth_success() {
    val authStub = MetadataUtils.attachHeaders(stub, appAuthMeta);

    // Test that the interceptor rejects this request
    try {
      val reply = authStub.listUsers(ListUsersRequest.newBuilder().build());
      assertThat(reply.getUsersCount()).isGreaterThanOrEqualTo(2);

    } catch (StatusRuntimeException e) {
      fail("App disallowed to access list user service.");
    }
  }
}
