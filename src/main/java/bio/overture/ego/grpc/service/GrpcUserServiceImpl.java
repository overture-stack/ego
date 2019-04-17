package bio.overture.ego.grpc.service;

import bio.overture.ego.grpc.*;
import bio.overture.ego.model.exceptions.NotFoundException;
import bio.overture.ego.service.UserService;
import bio.overture.ego.utils.CollectionUtils;
import io.grpc.stub.StreamObserver;
import java.util.Collections;
import java.util.List;
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
    User output = User.getDefaultInstance();

    try {
      val id = UUID.fromString(request.getId());

      try {
        val user = userService.get(id, true, true, true);
        output = user.toProto();

      } catch (NotFoundException e) {
        log.debug("gRPC Get UserService could not find user with requested ID:", e.getMessage());
      }
    } catch (IllegalArgumentException e) {
      log.info("gRPC Get UserService received invalid ID:", e.getMessage());
    }

    responseObserver.onNext(output);
    responseObserver.onCompleted();
  }

  @Override
  public void list(ListUsersRequest request, StreamObserver<ListUsersResponse> responseObserver) {
    val output = ListUsersResponse.newBuilder();

    val userResults =
        userService.listUsers(Collections.EMPTY_LIST, ProtoUtils.getPageable(request.getPage()));

    List<bio.overture.ego.model.entity.User> users = userResults.getContent();

    output.addAllUsers(CollectionUtils.mapToImmutableSet(users, user -> user.toProto()));

    responseObserver.onNext(output.build());
    responseObserver.onCompleted();
  }
}
