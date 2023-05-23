package bio.overture.ego.utils;

import static bio.overture.ego.model.exceptions.InternalServerException.buildInternalServerException;
import static org.springframework.http.HttpMethod.GET;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
public class CacheUtil {

  @Value("${broker.publicKey.url}")
  private String brokerPublicKeyUrl;

  private RestTemplate restTemplate;

  @Cacheable("getPassportBrokerPublicKey")
  public String getPassportBrokerPublicKey() {
    ResponseEntity<String> response;
    try {
      response = restTemplate.exchange(brokerPublicKeyUrl, GET, null, String.class);
    } catch (HttpStatusCodeException err) {
      log.error(
          "Invalid {} response from passport broker service: {}",
          err.getStatusCode().value(),
          err.getMessage());
      throw buildInternalServerException(
          "Invalid %s response from passport broker service.", err.getStatusCode().value());
    }
    return response.getBody();
  }

  @CacheEvict(value = "evictAll", allEntries = true)
  public void evictAllCaches() {}

  @Scheduled(fixedRate = 6000)
  public void evictAllCachesEveryDay() {
    evictAllCaches();
  }
}
