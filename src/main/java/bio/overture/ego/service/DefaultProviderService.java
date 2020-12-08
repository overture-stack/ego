package bio.overture.ego.service;

import bio.overture.ego.model.entity.DefaultProvider;
import bio.overture.ego.repository.DefaultProviderRepository;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DefaultProviderService extends AbstractBaseService<DefaultProvider, String> {

  private final DefaultProviderRepository defaultProviderRepository;

  @Autowired
  public DefaultProviderService(@NonNull DefaultProviderRepository defaultProviderRepository) {
    super(DefaultProvider.class, defaultProviderRepository);
    this.defaultProviderRepository = defaultProviderRepository;
  }

  @Override
  public DefaultProvider getWithRelationships(String s) {
    return null;
  }
}
