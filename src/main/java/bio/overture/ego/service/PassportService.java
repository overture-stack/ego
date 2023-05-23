package bio.overture.ego.service;

import bio.overture.ego.model.dto.Passport;
import bio.overture.ego.model.dto.PassportVisa;
import bio.overture.ego.model.entity.Visa;
import bio.overture.ego.model.entity.VisaPermission;
import bio.overture.ego.model.exceptions.InvalidTokenException;
import bio.overture.ego.utils.CacheUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import java.util.*;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
public class PassportService {

  /** Dependencies */
  @Autowired private VisaService visaService;

  @Autowired private VisaPermissionService visaPermissionService;

  @Autowired private CacheUtil cacheUtil;

  @Autowired
  public PassportService(
      @NonNull VisaPermissionService visaPermissionService, @NonNull VisaService visaService) {
    this.visaService = visaService;
    this.visaPermissionService = visaPermissionService;
  }

  public List<VisaPermission> getPermissions(String authToken) throws JsonProcessingException {
    // Validates passport auth token
    if (!isValidPassport(authToken)) {
      throw new InvalidTokenException("The passport token received from broker is invalid");
    }
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
  private boolean isValidPassport(@NonNull String authToken) {
    Claims claims;
    try {
      claims =
          Jwts.parser()
              .setSigningKey(cacheUtil.getPassportBrokerPublicKey())
              .parseClaimsJws(authToken)
              .getBody();
      if (claims != null) {
        return true;
      }
    } catch (Exception exception) {
      throw new InvalidTokenException("The passport token received from broker is invalid");
    }
    return false;
  }

  // Extracts Visas from parsed passport object
  private List<PassportVisa> getVisas(Passport passport) {
    List<PassportVisa> visas = new ArrayList<>();
    passport.getGa4ghPassportV1().stream()
        .forEach(
            visaJwt -> {
              try {
                if (visaService.isValidVisa(visaJwt)) {
                  PassportVisa visa = visaService.parseVisa(visaJwt);
                  if (visa != null) {
                    visas.add(visa);
                  }
                }
              } catch (JsonProcessingException e) {
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
              Visa visaEntity = new Visa();
              visaEntity.setId(UUID.fromString(visa.getJti()));
              visaPermissions.addAll(visaPermissionService.getPermissionsForVisa(visaEntity));
            });
    return visaPermissions;
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
}
