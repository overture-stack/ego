package bio.overture.ego.config;

import static bio.overture.ego.utils.Strings.isDefined;
import static com.google.common.base.Preconditions.checkState;

import bio.overture.ego.model.enums.ProviderType;
import bio.overture.ego.service.DefaultProviderService;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DefaultProviderGuardListener implements ApplicationListener<ContextRefreshedEvent> {

  private final DefaultProviderService defaultProviderService;
  private final ProviderType configuredProvider;

  @Autowired
  public DefaultProviderGuardListener(
      @NonNull DefaultProviderService defaultProviderService,
      @Value("${spring.flyway.placeholders.default-provider}") ProviderType configuredProvider) {
    this.defaultProviderService = defaultProviderService;
    this.configuredProvider = configuredProvider;
  }

  @Override
  public void onApplicationEvent(ContextRefreshedEvent event) {
    log.info("ApplicationContext refreshed, checking default provider configuration.");

    checkState(
        isDefined(configuredProvider.toString()), "Configured default provider is not defined!");
    // it is assumed that before boot the tripwire default provider has been set by running the
    // flyway migration.

    val storedDefaultProviders = defaultProviderService.findAll();

    checkState(
        storedDefaultProviders.size() == 1,
        "Tripwire was not set! This means the flyway migration did not run yet.");

    val storedProvider = storedDefaultProviders.get(0).getId();
    checkState(
        storedProvider.equals(configuredProvider),
        "Configured default-provider '%s' does not match what was previously configured '%s'",
        configuredProvider,
        storedProvider);

    log.info(
        String.format(
            "Configured default-provider '%s' matches previously configured '%s', finished bootstrap check.",
            configuredProvider, storedProvider));
  }
}
