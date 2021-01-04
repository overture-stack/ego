package bio.overture.ego.service;

import static bio.overture.ego.model.exceptions.NotFoundException.buildNotFoundException;

import bio.overture.ego.model.entity.DefaultProvider;
import bio.overture.ego.model.enums.ProviderType;
import bio.overture.ego.repository.DefaultProviderRepository;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DefaultProviderService extends AbstractBaseService<DefaultProvider, ProviderType> {

  private final DefaultProviderRepository defaultProviderRepository;

  @Autowired
  public DefaultProviderService(@NonNull DefaultProviderRepository defaultProviderRepository) {
    super(DefaultProvider.class, defaultProviderRepository);
    this.defaultProviderRepository = defaultProviderRepository;
  }

  @Override
  public DefaultProvider getWithRelationships(@NonNull ProviderType id) {
    return defaultProviderRepository
        .findById(id)
        .orElseThrow(() -> buildNotFoundException("Could not find default provider type '%s'", id));
  }
}
