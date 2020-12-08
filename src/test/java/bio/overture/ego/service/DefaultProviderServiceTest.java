package bio.overture.ego.service;

import static org.junit.Assert.*;

import bio.overture.ego.model.enums.ProviderType;
import bio.overture.ego.repository.DefaultProviderRepository;
import bio.overture.ego.utils.EntityGenerator;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@Slf4j
@SpringBootTest
@RunWith(SpringRunner.class)
@ActiveProfiles("test")
public class DefaultProviderServiceTest {

  @Autowired DefaultProviderService defaultProviderService;
  @Autowired DefaultProviderRepository defaultProviderRepository;
  @Autowired private EntityGenerator entityGenerator;

  @Value("${spring.flyway.placeholders.default_provider}")
  private String configuredDefaultProvider;

  @Test
  public void defaultProviderConfigured_Success() {
    defaultProviderService.findById(configuredDefaultProvider);
    val defaultProvider = defaultProviderService.getById(configuredDefaultProvider);
    assertNotNull(defaultProvider);
    assertEquals(defaultProvider.getId(), configuredDefaultProvider);
  }

  @Test
  public void defaultProviderConfigured_Failure() {
    val defaultProviderType = Enum.valueOf(ProviderType.class, configuredDefaultProvider);
    val mockedDefaultProvider =
        entityGenerator.randomEnumExcluding(ProviderType.class, defaultProviderType);
    val provider = defaultProviderService.findById(mockedDefaultProvider.toString());
    assertTrue(provider.isEmpty());
    assertNotEquals(provider, configuredDefaultProvider);
  }
}
