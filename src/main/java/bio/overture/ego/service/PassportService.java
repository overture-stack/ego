package bio.overture.ego.service;

import bio.overture.ego.model.dto.Passport;
import bio.overture.ego.model.dto.PassportToken;
import bio.overture.ego.model.dto.PassportVisa;
import bio.overture.ego.model.dto.Scope;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import static bio.overture.ego.utils.CollectionUtils.mapToSet;

@Slf4j
@Service
@Transactional
@Configuration
public class PassportService {

  /** Dependencies */
  @Autowired private VisaService visaService;

  @Autowired private VisaPermissionService visaPermissionService;

  @Autowired private CacheUtil cacheUtil;

  private final String REQUESTED_TOKEN_TYPE = "urn:ga4gh:params:oauth:token-type:passport";
  private final String SUBJECT_TOKEN_TYPE = "urn:ietf:params:oauth:token-type:access_token";
  private final String GRANT_TYPE = "urn:ietf:params:oauth:grant-type:token-exchange";

  @Value("${spring.security.oauth2.client.registration.passport.clientId}")
  private String clientId;

  @Value("${spring.security.oauth2.client.registration.passport.clientSecret}")
  private String clientSecret;

  @Value("${spring.security.oauth2.client.provider.passport.issuer-uri}")
  private String passportIssuerUri;



  @Autowired
  public PassportService(
      @NonNull VisaPermissionService visaPermissionService, @NonNull VisaService visaService) {
    this.visaService = visaService;
    this.visaPermissionService = visaPermissionService;
  }

  public List<VisaPermission> getPermissions(String authToken)
      throws JsonProcessingException, ParseException, JwkException {
    // Validates passport auth token
    isValidPassport(authToken);
    // Parses passport JWT token
    Passport parsedPassport = parsePassport(authToken);
    // Fetches visas for parsed passport
    List<PassportVisa> visas = getVisas(parsedPassport);
    // Fetches visa permissions for extracted visas
    List<VisaPermission> visaPermissions = getVisaPermissions(visas);
    // removes deduplicates from visaPermissions
    visaPermissions = deDupeVisaPermissions(visaPermissions);
    return visaPermissions;
  }

  // Validates passport token based on public key
  private void isValidPassport(@NonNull String authToken)
      throws ParseException, JwkException, JsonProcessingException {
    DecodedJWT jwt = JWT.decode(authToken);
    Jwk jwk = cacheUtil.getPassportBrokerPublicKey().get(jwt.getKeyId());
    Algorithm algorithm = Algorithm.RSA256((RSAPublicKey) jwk.getPublicKey(), null);
    algorithm.verify(jwt);
  }

  // Extracts Visas from parsed passport object
  private List<PassportVisa> getVisas(Passport passport) {
    List<PassportVisa> visas = new ArrayList<>();
    passport.getGa4ghPassportV1().stream()
        .forEach(
            visaJwt -> {
              try {
                visaService.isValidVisa(visaJwt);
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

  public Set<Scope> extractScopes(@NonNull String passportJwtToken) throws ParseException, JwkException, JsonProcessingException {
      val resolvedPermissions = getPermissions(passportJwtToken);
      val output = mapToSet(resolvedPermissions, AbstractPermissionService::buildScope);
      if (output.isEmpty()) {
        output.add(Scope.defaultScope());
      }
      return output;
  }

  // Parse Passport token to extract the passport body
  public Passport parsePassport(@NonNull String passportJwtToken) throws JsonProcessingException {
    String[] split_string = passportJwtToken.split("\\.");
    String base64EncodedHeader = split_string[0];
    String base64EncodedBody = split_string[1];
    String base64EncodedSignature = split_string[2];
    Base64 base64Url = new Base64(true);
    String header = new String(base64Url.decode(base64EncodedHeader));
    String body = new String(base64Url.decode(base64EncodedBody));
    return new ObjectMapper().readValue(body, Passport.class);
  }

  // Removes duplicates from the VisaPermissons List
  private List<VisaPermission> deDupeVisaPermissions(List<VisaPermission> visaPermissions) {
    Set<VisaPermission> permissionsSet = new HashSet<VisaPermission>();
    permissionsSet.addAll(visaPermissions);
    return permissionsSet.stream().collect(Collectors.toList());
  }

  public String getPassportToken(String accessToken) {

    if (accessToken == null || accessToken.isEmpty()) return null;

    val params = passportTokenParams(accessToken);

    val uri = UriComponentsBuilder
        .fromUriString(passportIssuerUri)
        .path("/token")
        .queryParams(params)
        .toUriString();

    val passportToken = getTemplate(clientId, clientSecret)
                .exchange(uri,
                    HttpMethod.POST,
                    null,
                    PassportToken.class)
                .getBody();

    return (passportToken != null) ?
      passportToken.getAccess_token() :
        null;
  }

  private RestTemplate getTemplate(String clientId, String clientSecret) {
    RestTemplate restTemplate = new RestTemplate();
    restTemplate
        .getInterceptors()
        .add(
            (x, y, z) -> {
              x.getHeaders()
                  .set(
                      HttpHeaders.AUTHORIZATION,
                      "Basic " + getBasicAuthHeader(clientId, clientSecret));
              return z.execute(x, y);
            });
    return restTemplate;
  }

  private String getBasicAuthHeader(String clientId, String clientSecret) {
    String credentials = clientId + ":" + clientSecret;
    return new String(Base64.encodeBase64(credentials.getBytes()));
  }

  private MultiValueMap<String, String> passportTokenParams(String accessToken){
    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    params.add("requested_token_type", REQUESTED_TOKEN_TYPE);
    params.add("subject_token", accessToken);
    params.add("subject_token_type", SUBJECT_TOKEN_TYPE);
    params.add("grant_type", GRANT_TYPE);
    return params;
  }
}
