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

    checkState(isDefined(configuredProvider.toString()), "Default provider is not defined!");
    val storedProviderResult = defaultProviderService.findById(configuredProvider);
    // it is assumed that before boot the tripwire default provider has been set by running the
    // flyway migration. there will be a sql error if the table does not exist, indicating the
    // migration has not run.
    checkState(
        storedProviderResult.isPresent(),
        "Configured default-provider '%s' does not match what was previously configured. Check the defaultprovidertripwire table.",
        configuredProvider);
    ;

    log.info(
        String.format(
            "Configured default-provider '%s' matches previously configured '%s, finished bootstrap check.",
            configuredProvider, storedProviderResult.get().getId()));
  }
}
