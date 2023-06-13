package bio.overture.ego.utils;

import static bio.overture.ego.model.exceptions.InternalServerException.buildInternalServerException;
import static org.springframework.http.HttpMethod.GET;

import com.auth0.jwk.Jwk;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
public class CacheUtil {

  @Autowired
  private ClientRegistrationRepository clientRegistrationRepository;

  private RestTemplate restTemplate;

  @Cacheable("getPassportBrokerPublicKey")
  public Map<String, Jwk> getPassportBrokerPublicKey(String providerType) {
    ResponseEntity<Map<String, List>> response;
    Map<String, Jwk> jwkMap = new HashMap();

    val clientRegistration = clientRegistrationRepository.findByRegistrationId(providerType);
    try {
      restTemplate = new RestTemplate();
      response =
          restTemplate.exchange(
              clientRegistration.getProviderDetails().getJwkSetUri(),
              GET,
              null,
              new ParameterizedTypeReference<Map<String, List>>() {});
    } catch (HttpStatusCodeException err) {
      log.error(
          "Invalid {} response from passport broker config: {}",
          err.getStatusCode().value(),
          err.getMessage());
      throw buildInternalServerException(
          "Invalid %s response from passport broker config.", err.getStatusCode().value());
    }
    for (Object obj : response.getBody().get("keys")) {
      Jwk jwkObject = Jwk.fromValues((Map<String, Object>) obj);
      jwkMap.put(jwkObject.getId(), jwkObject);
    }
    return jwkMap;
  }

  @CacheEvict(value = "evictAll", allEntries = true)
  public void evictAllCaches() {}

  @Scheduled(fixedRate = 6000)
  public void evictAllCachesEveryDay() {
    evictAllCaches();
  }
}
