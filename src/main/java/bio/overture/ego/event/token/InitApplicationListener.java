package bio.overture.ego.event.token;

import bio.overture.ego.service.InitializationService;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationContextInitializedEvent;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * After the application is ready to serve requests, attempt to initialize the application.
 * If there is an error during the initialization, the app will shutdown.
 */
@Slf4j
@Component
@ConditionalOnProperty(
    value = "initialization.enabled",
    havingValue = "true",
    matchIfMissing = false
)
public class InitApplicationListener implements ApplicationListener<ApplicationPreparedEvent> {

  private final InitializationService initializationService;

  @Autowired
  public InitApplicationListener(@NonNull InitializationService initializationService) {
    this.initializationService = initializationService;
  }

  @Override
  public void onApplicationEvent(ApplicationPreparedEvent applicationPreparedEvent) {
    initializationService.initialize();
  }

}
