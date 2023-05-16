package bio.overture.ego.service;

import bio.overture.ego.model.dto.Passport;
import bio.overture.ego.model.entity.Visa;
import bio.overture.ego.model.entity.VisaPermission;
import bio.overture.ego.model.exceptions.InvalidTokenException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
public class PassportService {

  @Value("${broker.token.config.public-key}")
  private String publicKey;

  /** Dependencies */
  @Autowired private VisaService visaService;

  @Autowired private VisaPermissionService visaPermissionService;

  @Autowired
  public PassportService(
      @NonNull VisaPermissionService visaPermissionService,
      @NonNull VisaService visaService) {
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
    List<Visa> visas = getVisas(parsedPassport);
    // Fetches visa permissions for extracted visas
    List<VisaPermission> visaPermissions = getVisaPermissions(visas);
    // removes deuplicates from visaPermissions
    // TO_DO : visaPermissions = deDupeVisaPermissions (visaPermissions);
    return visaPermissions;
  }

  // TO_DO : Validates passport token based on public key
  private boolean isValidPassport(@NonNull Object passport) {
    return true;
  }

  // Extracts Visas from parsed passport object
  private List<Visa> getVisas(Passport passport) {
    List<Visa> visas = new ArrayList<>();
    passport.getGa4ghPassportV1().stream()
        .forEach(
            visaJwt -> {
              try {
                Visa visa = visaService.parseVisa(visaJwt);
                if (visa != null) {
                  visas.add(visa);
                }
              } catch (JsonProcessingException e) {
                e.printStackTrace();
              }
            });
    return visas;
  }

  // Fetches Visa Permissions for extracted Visa list
  private List<VisaPermission> getVisaPermissions(List<Visa> visas) {
    List<VisaPermission> visaPermissions = new ArrayList<>();
    visas.stream()
        .distinct()
        .forEach(
            visa -> {
              if (visaService.getById(visa.getId()) != null) {
                visaPermissions.addAll(visaPermissionService.getPermissionsForVisa(visa));
              }
            });
    return visaPermissions;
  }

  // Parse Passport token to extract the passport body
  public Passport parsePassport(@NonNull String passportJwtToken) throws JsonProcessingException {
    String[] split_string = passportJwtToken.split("//.");
    String base64EncodedHeader = split_string[0];
    String base64EncodedBody = split_string[1];
    String base64EncodedSignature = split_string[2];
    Base64 base64Url = new Base64(true);
    String header = new String(base64Url.decode(base64EncodedHeader));
    String body = new String(base64Url.decode(base64EncodedBody));
    return new ObjectMapper().readValue(body, Passport.class);
  }
}
