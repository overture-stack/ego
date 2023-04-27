package bio.overture.ego.service;

import static bio.overture.ego.model.enums.StatusType.APPROVED;
import static bio.overture.ego.model.exceptions.InternalServerException.checkInternalServerException;
import static java.util.Objects.nonNull;

import bio.overture.ego.config.InitializationConfig;
import bio.overture.ego.model.dto.CreateApplicationRequest;
import bio.overture.ego.model.entity.InitTripWire;
import bio.overture.ego.repository.InitTripWireRepository;
import jakarta.transaction.Transactional;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class InitializationService {

  private final InitTripWireRepository initTripWireRepository;
  private final ApplicationService applicationService;
  private final InitializationConfig config;

  @Autowired
  public InitializationService(
      @NonNull InitTripWireRepository initTripWireRepository,
      @NonNull ApplicationService applicationService,
      @NonNull InitializationConfig config) {
    this.initTripWireRepository = initTripWireRepository;
    this.applicationService = applicationService;
    this.config = config;
  }

  @Transactional
  public void initialize() {
    if (isInitialized()) {
      log.info("[InitTripWire]: Already tripped, skipping initialization");
    } else {
      log.info("[InitTripWire]: Not tripped, initializing applications");
      if (nonNull(config.getApplications()) && !config.getApplications().isEmpty()) {
        config.getApplications().stream()
            .map(InitializationService::convertToApplication)
            .forEach(applicationService::create);
      }
      val initTripWire = InitTripWire.builder().initialized(1).build();
      initTripWireRepository.save(initTripWire);
    }
  }

  public boolean isInitialized() {
    val results = initTripWireRepository.findAll();
    checkInternalServerException(
        results.size() < 2,
        "Returned more than 1 result, when only a maximum of 1 result was expected");
    if (!results.isEmpty()) {
      val initTripWire = results.get(0);
      return initTripWire.getInitialized() > 0;
    }
    return false;
  }

  private static CreateApplicationRequest convertToApplication(
      InitializationConfig.InitialApplication initialApplication) {
    return CreateApplicationRequest.builder()
        .name(initialApplication.getName())
        .clientId(initialApplication.getClientId())
        .clientSecret(initialApplication.getClientSecret())
        .description(initialApplication.getDescription())
        .redirectUri(initialApplication.getRedirectUri())
        .type(initialApplication.getType())
        .status(APPROVED)
        .build();
  }
}
