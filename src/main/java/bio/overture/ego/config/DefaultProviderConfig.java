package bio.overture.ego.config;

import static bio.overture.ego.utils.Strings.isDefined;
import static com.google.common.base.Preconditions.checkState;

import bio.overture.ego.model.enums.ProviderType;
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

  @Autowired private DefaultProviderService defaultProviderService;

  @Value("${spring.flyway.placeholders.default_provider}")
  private ProviderType configuredProvider;

  @Override
  public void onApplicationEvent(ContextRefreshedEvent event) {
    log.info("ApplicationContext refreshed, checking default provider configuration.");

    checkState(isDefined(configuredProvider.toString()), "Default provider is not defined!");
    val storedProviderResult = defaultProviderService.findById(configuredProvider);
    // it is assumed that before boot the tripwire default provider has been set by running the
    // flyway migration
    checkState(
        storedProviderResult.isPresent(),
        "Tripwire was not set! This means the flyway migration did not run yet.");
    val storedProvider = storedProviderResult.get().getId();
    checkState(
        configuredProvider.equals(storedProvider),
        "Configured default_provider '%s' does not match what was previously configured '%s'",
        configuredProvider,
        storedProvider);

    log.info(
        String.format(
            "Configured default_provider '%s' matches previously configured '%s, finished initializing.",
            configuredProvider, storedProvider));
  }
}
