package bio.overture.ego.service;

import bio.overture.ego.model.dto.Passport;
import bio.overture.ego.model.entity.Visa;
import bio.overture.ego.model.entity.VisaPermission;
import bio.overture.ego.model.exceptions.ForbiddenException;
import bio.overture.ego.model.exceptions.InternalServerException;
import bio.overture.ego.token.signer.TokenSigner;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.JWT;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.validation.constraints.NotNull;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.lang.String.format;

@Slf4j
@Service
@Transactional
public class PassportService {

  @Value("${broker.passport.url}")
  private String passportBrokerUrl;

  @Value("${broker.token.config.public-key}")
  private String publicKey;

  /** Dependencies */
  @Autowired private VisaService visaService;

  @Autowired private VisaPermissionService visaPermissionService;

  private RestTemplate restTemplate;
  private final TokenSigner tokenSigner;

  @Autowired
  public PassportService(
          @NonNull VisaPermissionService visaPermissionService,
          @NonNull VisaService visaService,
          @NotNull TokenSigner tokenSigner) {
    this.visaService = visaService;
    this.visaPermissionService = visaPermissionService;
    this.tokenSigner = tokenSigner;
  }

  public String validatePassportPermissions (String authToken) throws JsonProcessingException {

    val passportToken = fetchPassport(authToken);
    if (!isValidPassport(passportToken)) {

    }
    Object parsedPassport = parse (authToken);
    ObjectMapper mapper = new ObjectMapper();
    Passport passport = mapper.readValue((String) parsedPassport, Passport.class);
    //getPermissionsForPassport(visaId);
    //prepareUserPermissions(visaPermissions);
    return null;
  }

  private Object fetchPassport (String authToken) {
    val response =
            restTemplate.exchange(
                    passportBrokerUrl, HttpMethod.POST, new HttpEntity<>(authToken, null), String.class);
    return response.getBody();
  }

  private boolean isValidPassport(@NonNull Object passport) {

    return true;
  }

  private boolean isValidVisa(@NonNull Object visa) {

    return true;
  }

  private List<VisaPermission> getVisaPermissions (List<UUID> visaIds) {
    List<VisaPermission> visaPermissions = new ArrayList<>();
    visaIds.stream().distinct().forEach(visaId -> {
      if (visaService.getById(visaId) != null) {
        visaPermissions.addAll(visaPermissionService.getPermissionsByVisaId(visaId));
      }
    });
    return visaPermissions;
  }


  private Optional<Object> parse (@NonNull String token) {
    Object parsedObj;
    val tokenKey =
            tokenSigner
                    .getKey()
                    .orElseThrow(() -> new InternalServerException("Internal issue with token signer."));

    try {
      parsedObj = Jwts.parser().setSigningKey(tokenKey).parse(token);
    } catch (JwtException e) {
      log.error("JWT token is invalid", e);
      throw new ForbiddenException("Authorization is required for this action.");
    }
    return parsedObj == null ? Optional.empty() : Optional.of(parsedObj);
  }


}
