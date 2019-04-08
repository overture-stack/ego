package bio.overture.ego.grpc;

import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class UserServiceImpl extends UserServiceGrpc.UserServiceImplBase {

  @Autowired
  public UserServiceImpl() {}

  @Override
  public void get(GetUserRequest request, StreamObserver<User> responseObserver) {
    val output = User.newBuilder().build();

    responseObserver.onNext(output);
    responseObserver.onCompleted();
  }

  @Override
  public void list(ListUsersRequest request, StreamObserver<ListUsersResponse> responseObserver) {
    val output = ListUsersResponse.newBuilder().build();

    responseObserver.onNext(output);
    responseObserver.onCompleted();
  }
}
