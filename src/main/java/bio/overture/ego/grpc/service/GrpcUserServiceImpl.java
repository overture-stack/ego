package bio.overture.ego.grpc.service;

import static bio.overture.ego.utils.CollectionUtils.mapToImmutableSet;

import bio.overture.ego.grpc.*;
import bio.overture.ego.model.exceptions.NotFoundException;
import bio.overture.ego.service.UserService;
import io.grpc.stub.StreamObserver;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class GrpcUserServiceImpl extends UserServiceGrpc.UserServiceImplBase {

  private final UserService userService;

  @Autowired
  public GrpcUserServiceImpl(UserService userService) {
    this.userService = userService;
  }

  @Override
  public void get(GetUserRequest request, StreamObserver<User> responseObserver) {
    val userBuilder = User.newBuilder();

    try {

      final val id = UUID.fromString(request.getId());

      try {
        val user = userService.get(id, true, true, false);

        val groups =
            mapToImmutableSet(
                user.getUserGroups(), userGroup -> userGroup.getGroup().getId().toString());
        val permissions =
            mapToImmutableSet(
                user.getUserPermissions(), userPermission -> userPermission.getId().toString());

        userBuilder
            .setId(id.toString())
            .setEmail(user.getEmail())
            .setFirstName(user.getFirstName() )
            .setLastName(user.getLastName())
            .setName(user.getName())
            .setCreatedAt(safeToString(user.getCreatedAt()))
            .setLastLogin(safeToString(user.getLastLogin()))
            .setPreferredLanguage(safeToString(user.getPreferredLanguage()))
            .setStatus(safeToString(user.getStatus()))
            .setType(safeToString(user.getType()))
            .addAllPermissions(permissions)
            .addAllGroups(groups);

      } catch (NotFoundException e) {
        log.debug("gRPC Get UserService could not find user with requested ID:", e.getMessage());
      }
    } catch (IllegalArgumentException e) {
      log.info("gRPC Get UserService received invalid ID:", e.getMessage());
    }

    responseObserver.onNext(userBuilder.build());
    responseObserver.onCompleted();
  }

  @Override
  public void list(ListUsersRequest request, StreamObserver<ListUsersResponse> responseObserver) {
    val output = ListUsersResponse.newBuilder().build();

    responseObserver.onNext(output);
    responseObserver.onCompleted();
  }

  private String safeToString(Object value) {
    return value == null ? "" : value.toString();
  }
}
