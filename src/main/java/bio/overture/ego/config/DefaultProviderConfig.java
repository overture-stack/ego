package bio.overture.ego.config;

import static java.util.Objects.isNull;

import bio.overture.ego.service.DefaultProviderService;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DefaultProviderConfig implements ApplicationListener<ContextRefreshedEvent> {

  @Autowired DefaultProviderService defaultProviderService;

  @Value("${spring.flyway.placeholders.default_provider}")
  String configuredProvider;

  @Override
  public void onApplicationEvent(ContextRefreshedEvent event) {
    log.info("ApplicationContext refreshed, checking default provider configuration.");
    val storedProvider = defaultProviderService.findById(configuredProvider);
    if (isNull(configuredProvider) || storedProvider.isEmpty()) {
      throw new IllegalStateException(
          "Configured default_provider does not match database. Check your configuration in app.properties!");
    }
    log.info(
        String.format(
            "Configured default_provider '%s' is correct, finished initializing.",
            configuredProvider));
  }
}
