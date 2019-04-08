package bio.overture.ego.grpc;

import io.grpc.*;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class GrpcServer implements CommandLineRunner, DisposableBean {

  private Server server;

  private final UserServiceImpl userServiceImpl;

  @Autowired
  public GrpcServer(UserServiceImpl userServiceImpl) {
    this.userServiceImpl = userServiceImpl;
  }

  @Override
  public void run(String... args) throws Exception {
    int port = 50051;

    server =
        ServerBuilder.forPort(port)
            .addService(this.userServiceImpl)
            //            .addService(ProtoReflectionService.newInstance())
            .build()
            .start();

    log.info("gRPC Server started, listening on " + port);
    startDaemonAwaitThread();
  }

  private void startDaemonAwaitThread() {
    Thread awaitThread =
        new Thread(
            () -> {
              try {
                this.server.awaitTermination();
              } catch (InterruptedException e) {
                log.error("gRPC server stopped.", e);
              }
            });
    awaitThread.start();
  }

  @Override
  public final void destroy() throws Exception {
    log.info("Shutting down gRPC server ...");
    Optional.ofNullable(server).ifPresent(Server::shutdown);
    log.info("gRPC server stopped.");
  }
}
