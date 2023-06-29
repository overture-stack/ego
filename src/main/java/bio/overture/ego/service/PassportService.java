package bio.overture.ego.service;

import static bio.overture.ego.utils.CollectionUtils.mapToSet;

import bio.overture.ego.model.dto.*;
import bio.overture.ego.model.entity.User;
import bio.overture.ego.model.entity.Visa;
import bio.overture.ego.model.entity.VisaPermission;
import bio.overture.ego.utils.CacheUtil;
import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkException;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Service
@Transactional
@Configuration
public class PassportService {

  /** Dependencies */
  @Autowired private VisaService visaService;

  @Autowired private VisaPermissionService visaPermissionService;

  @Autowired private CacheUtil cacheUtil;

  @Autowired private ClientRegistrationRepository clientRegistrationRepository;

  @Autowired private InMemoryClientRegistrationRepository inMemoryClientRegistrationRepository;

  private final String TOKEN_TYPE_PASSPORT = "urn:ga4gh:params:oauth:token-type:passport";
  private final String TOKEN_TYPE_ACCESS_TOKEN = "urn:ietf:params:oauth:token-type:access_token";
  private final String GRANT_TYPE_TOKEN_EXCHANGE =
      "urn:ietf:params:oauth:grant-type:token-exchange";
  private final String REFRESH_TOKEN = "refresh_token";
  private final String GA4GH_PASSPORT_SCOPE = "ga4gh_passport_v1";

  @Autowired
  public PassportService(
      @NonNull VisaPermissionService visaPermissionService, @NonNull VisaService visaService) {
    this.visaService = visaService;
    this.visaPermissionService = visaPermissionService;
  }

  public List<VisaPermission> getPermissions(String authToken, String providerType)
      throws JsonProcessingException, ParseException, JwkException {
    // Validates passport auth token
    isValidPassport(authToken, providerType);
    // Parses passport JWT token
    Passport parsedPassport = parsePassport(authToken);
    // Fetches visas for parsed passport
    List<PassportVisa> visas = getVisas(parsedPassport, providerType);
    // Fetches visa permissions for extracted visas
    List<VisaPermission> visaPermissions = getVisaPermissions(visas);
    // removes deduplicates from visaPermissions
    visaPermissions = deDupeVisaPermissions(visaPermissions);
    return visaPermissions;
  }

  // Validates passport token based on public key
  private void isValidPassport(@NonNull String authToken, @NonNull String providerType)
      throws JwkException {
    DecodedJWT jwt = JWT.decode(authToken);
    Jwk jwk = cacheUtil.getPassportBrokerPublicKey(providerType).get(jwt.getKeyId());
    Algorithm algorithm = Algorithm.RSA256((RSAPublicKey) jwk.getPublicKey(), null);
    algorithm.verify(jwt);
  }

  // Extracts Visas from parsed passport object
  private List<PassportVisa> getVisas(Passport passport, @NonNull String providerType) {
    List<PassportVisa> visas = new ArrayList<>();
    passport.getGa4ghPassportV1().stream()
        .forEach(
            visaJwt -> {
              try {
                visaService.isValidVisa(visaJwt, providerType);
                PassportVisa visa = visaService.parseVisa(visaJwt);
                if (visa != null) {
                  visas.add(visa);
                }
              } catch (JsonProcessingException | JwkException e) {
                e.printStackTrace();
              }
            });
    return visas;
  }

  // Fetches Visa Permissions for extracted Visa list
  private List<VisaPermission> getVisaPermissions(List<PassportVisa> visas) {
    List<VisaPermission> visaPermissions = new ArrayList<>();
    visas.stream()
        .distinct()
        .forEach(
            visa -> {
              List<Visa> visaEntities =
                  visaService.getByTypeAndValue(
                      visa.getGa4ghVisaV1().getType(), visa.getGa4ghVisaV1().getValue());
              if (visaEntities != null && !visaEntities.isEmpty()) {
                visaPermissions.addAll(visaPermissionService.getPermissionsForVisa(visaEntities));
              }
            });
    return visaPermissions;
  }

  public Set<Scope> extractScopes(@NonNull String passportJwtToken, @NonNull String providerType)
      throws ParseException, JwkException, JsonProcessingException {
    val resolvedPermissions = getPermissions(passportJwtToken, providerType);
    val output = mapToSet(resolvedPermissions, AbstractPermissionService::buildScope);
    if (output.isEmpty()) {
      output.add(Scope.defaultScope());
    }
    return output;
  }

  // Parse Passport token to extract the passport body
  @SneakyThrows
  private Passport parsePassport(@NonNull String passportJwtToken) {
    val body = decodeJwtBody(passportJwtToken);
    return new ObjectMapper().readValue(body, Passport.class);
  }

  @SneakyThrows
  private PassportRefreshToken parseRefreshToken(@NonNull String jwtRefreshToken) {
    val body = decodeJwtBody(jwtRefreshToken);
    return new ObjectMapper().readValue(body, PassportRefreshToken.class);
  }

  private String decodeJwtBody(@NonNull String jwtToken){
    val split_string = jwtToken.split("\\.");
    val base64EncodedBody = split_string[1];
    val base64Url = new Base64(true);
    return new String(base64Url.decode(base64EncodedBody));
  }

  // Removes duplicates from the VisaPermissons List
  private List<VisaPermission> deDupeVisaPermissions(List<VisaPermission> visaPermissions) {
    Set<VisaPermission> permissionsSet = new HashSet<VisaPermission>();
    permissionsSet.addAll(visaPermissions);
    return permissionsSet.stream().collect(Collectors.toList());
  }

  public String getPassportToken(String providerId, String accessToken) {

    if (accessToken == null || accessToken.isEmpty()) return null;

    val clientRegistration = clientRegistrationRepository.findByRegistrationId(providerId);

    val uri =
        UriComponentsBuilder.fromUriString(clientRegistration.getProviderDetails().getTokenUri())
            .queryParams(passportTokenParams(accessToken))
            .toUriString();

    val passportToken =
        getTemplate(clientRegistration)
            .exchange(uri, HttpMethod.POST, null, PassportToken.class)
            .getBody();

    return (passportToken != null) ? passportToken.getAccess_token() : null;
  }


  public PassportRefreshTokenResponse refreshToken(String providerId, String refreshJwtToken) {
    if (refreshJwtToken == null || refreshJwtToken.isEmpty()) return null;

    val clientRegistration = clientRegistrationRepository.findByRegistrationId(providerId);

    val uri =
        UriComponentsBuilder.fromUriString(clientRegistration.getProviderDetails().getTokenUri())
            .queryParams(refreshPassportTokenParams(clientRegistration, refreshJwtToken))
            .toUriString();

    return getTemplate(clientRegistration)
        .exchange(uri, HttpMethod.POST, null, PassportRefreshTokenResponse.class)
        .getBody();
  }

  public Map<String, ClientRegistration> getPassportClientRegistrations() {
    Map<String, ClientRegistration> passportProviderDetails = new HashMap<>();

    inMemoryClientRegistrationRepository.forEach(
        clientRegistration -> {
          if (clientRegistration.getScopes().contains(GA4GH_PASSPORT_SCOPE)) {
            passportProviderDetails.put(
                clientRegistration.getProviderDetails().getIssuerUri(), clientRegistration);
          }
        });
    return passportProviderDetails;
  }

  private RestTemplate getTemplate(ClientRegistration clientRegistration) {
    RestTemplate restTemplate = new RestTemplate();
    restTemplate
        .getInterceptors()
        .add(
            (x, y, z) -> {
              x.getHeaders()
                  .set(
                      HttpHeaders.AUTHORIZATION,
                      "Basic "
                          + getBasicAuthHeader(
                              clientRegistration.getClientId(),
                              clientRegistration.getClientSecret()));
              return z.execute(x, y);
            });
    return restTemplate;
  }

  private String getBasicAuthHeader(String clientId, String clientSecret) {
    String credentials = clientId + ":" + clientSecret;
    return new String(Base64.encodeBase64(credentials.getBytes()));
  }

  private MultiValueMap<String, String> passportTokenParams(String accessToken) {
    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    params.add("requested_token_type", TOKEN_TYPE_PASSPORT);
    params.add("subject_token", accessToken);
    params.add("subject_token_type", TOKEN_TYPE_ACCESS_TOKEN);
    params.add("grant_type", GRANT_TYPE_TOKEN_EXCHANGE);
    return params;
  }

  private MultiValueMap<String, String> refreshPassportTokenParams(
      ClientRegistration clientRegistration, String refreshJwtToken) {
    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    params.add("client_id", clientRegistration.getClientId());
    params.add("client_secret", clientRegistration.getClientSecret());
    params.add("refresh_token", refreshJwtToken);
    params.add("grant_type", REFRESH_TOKEN);
    return params;
  }

  @SneakyThrows
  public PassportRefreshToken getRefreshTokenClaims(String jwtToken, User user) {
    isValidPassport(
        jwtToken,
        this.getPassportClientRegistrations().get(user.getProviderIssuerUri()).getRegistrationId());

    return parseRefreshToken(jwtToken);
  }
}
