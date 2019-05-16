package bio.overture.ego.grpc.service;

import static bio.overture.ego.grpc.ProtoUtils.createPagedResponse;
import static bio.overture.ego.grpc.ProtoUtils.getPageable;

import bio.overture.ego.grpc.*;
import bio.overture.ego.grpc.interceptor.ApplicationAuthInterceptor;
import bio.overture.ego.model.exceptions.NotFoundException;
import bio.overture.ego.service.UserService;
import bio.overture.ego.utils.CollectionUtils;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.util.Collections;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class UserServiceGrpcImpl extends UserServiceGrpc.UserServiceImplBase {

  private final UserService userService;

  @Autowired
  public UserServiceGrpcImpl(UserService userService) {
    this.userService = userService;
  }

  @Override
  public void getUser(GetUserRequest request, StreamObserver<User> responseObserver) {
    User output = User.getDefaultInstance();

    try {
      val id = UUID.fromString(request.getId());

      val authInfo = ApplicationAuthInterceptor.AUTH_INFO.get();
      if (authInfo != null) {

        // Auth Checks - must be Admin, or an app, or a user requesting their own data.
        val selfRequest = authInfo.isUser() && id.equals(authInfo.getId());
        if (!(authInfo.isAdmin() || authInfo.isApp() || selfRequest)) {
          responseObserver.onError(
              Status.UNAUTHENTICATED
                  .withDescription("Must be ADMIN or a user requesting themselves.")
                  .asRuntimeException());
          return;
        }
      }

      val user = userService.get(id, true, true, true);
      output = user.toProto();

    } catch (NotFoundException e) {
      log.debug("gRPC Get UserService could not find user with requested ID:", e.getMessage());
      responseObserver.onError(
          Status.NOT_FOUND.withDescription("No User found for provided ID.").asRuntimeException());
      return;
    } catch (IllegalArgumentException e) {
      log.info("gRPC Get UserService received invalid ID:", e.getMessage());
      responseObserver.onError(
          Status.INVALID_ARGUMENT.withDescription("ID is not a valid UUID.").asRuntimeException());
      return;
    }

    responseObserver.onNext(output);
    responseObserver.onCompleted();
  }

  @Override
  public void listUsers(
      ListUsersRequest request, StreamObserver<ListUsersResponse> responseObserver) {

    val authInfo = ApplicationAuthInterceptor.AUTH_INFO.get();
    if (authInfo != null) {
      // Auth Checks - must be admin or an app
      if (!(authInfo.isAdmin() || authInfo.isApp())) {
        responseObserver.onError(
            Status.UNAUTHENTICATED
                .withDescription("Must be an application or ADMIN user.")
                .asRuntimeException());
        return;
      }
    }

    val output = ListUsersResponse.newBuilder();

    // Find Page of users (filtered by groups if provided)
    val userPage = findUsersForListRequest(request);

    if (userPage.hasContent()) {
      val userIds = CollectionUtils.mapToImmutableSet(userPage.getContent(), user -> user.getId());

      // Only run this fetch if we have at least 1 user ID, filtering by empty list throws error
      val users = userService.getMany(userIds, true, true, true);

      // Add to output in order from first query
      userIds.forEach(
          id ->
              users.stream()
                  .filter(user -> user.getId().equals(id))
                  .findFirst()
                  .ifPresent(user -> output.addUsers(user.toProto())));
    }

    output.setPage(createPagedResponse(userPage, request.getPage().getPageNumber()));

    responseObserver.onNext(output.build());
    responseObserver.onCompleted();
  }

  private Page<bio.overture.ego.model.entity.User> findUsersForListRequest(
      ListUsersRequest request) {
    val query = request.hasQuery() ? request.getQuery().getValue() : "";
    val groups = request.getGroupIdsList();
    val pageable = getPageable(request.getPage());

    if (groups.isEmpty()) {
      return userService.findUsers(query, Collections.EMPTY_LIST, pageable);

    } else {
      val groupIds = CollectionUtils.mapToImmutableSet(groups, group -> UUID.fromString(group));
      return userService.findUsersForGroups(groupIds, query, Collections.EMPTY_LIST, pageable);
    }
  }
}
