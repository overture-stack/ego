package bio.overture.ego.service;

import static bio.overture.ego.model.exceptions.NotFoundException.checkNotFound;
import static bio.overture.ego.model.exceptions.RequestValidationException.checkRequestValid;
import static java.lang.String.format;
import static org.mapstruct.factory.Mappers.getMapper;

import bio.overture.ego.event.token.ApiKeyEventsPublisher;
import bio.overture.ego.model.dto.PassportVisa;
import bio.overture.ego.model.dto.VisaRequest;
import bio.overture.ego.model.entity.Visa;
import bio.overture.ego.model.exceptions.InvalidTokenException;
import bio.overture.ego.model.exceptions.NotFoundException;
import bio.overture.ego.repository.VisaRepository;
import bio.overture.ego.utils.CacheUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.codec.binary.Base64;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.ReportingPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
public class VisaService extends AbstractNamedService<Visa, UUID> {
  /** Constants */
  private static final VisaConverter VISA_CONVERTER = getMapper(VisaConverter.class);

  /** Dependencies */
  @Autowired private VisaRepository visaRepository;

  @Autowired private CacheUtil cacheUtil;

  private final ApiKeyEventsPublisher apiKeyEventsPublisher;

  @Autowired
  public VisaService(
      @NonNull VisaRepository visaRepository,
      @NonNull ApiKeyEventsPublisher apiKeyEventsPublisher) {
    super(Visa.class, visaRepository);
    this.visaRepository = visaRepository;
    this.apiKeyEventsPublisher = apiKeyEventsPublisher;
  }

  public Visa create(@NonNull VisaRequest createRequest) {
    checkRequestValid(createRequest);
    val visa = VISA_CONVERTER.convertToVisa(createRequest);
    return getRepository().save(visa);
  }

  @Override
  public Visa getById(@NonNull UUID uuid) {
    val result = (Optional<Visa>) getRepository().findById(uuid);
    checkNotFound(result.isPresent(), "The visaId '%s' does not exist", uuid);
    return result.get();
  }

  public List<Visa> getByTypeAndValue(@NonNull String type, @NotNull String value) {
    val result = visaRepository.getByTypeAndValue(type, value);
    if (!result.isEmpty()) {
      return result;
    } else {
      throw new NotFoundException(
          format("No Visa exists with type '%s' and value '%s'", type, value));
    }
  }

  public void delete(@NonNull UUID id) {
    checkExistence(id);
    super.delete(id);
  }

  // Parses Visa JWT token to convert into Visa Object
  public PassportVisa parseVisa(@NonNull String visaJwtToken) throws JsonProcessingException {
    String[] split_string = visaJwtToken.split("\\.");
    String base64EncodedHeader = split_string[0];
    String base64EncodedBody = split_string[1];
    String base64EncodedSignature = split_string[2];
    Base64 base64Url = new Base64(true);
    String header = new String(base64Url.decode(base64EncodedHeader));
    String body = new String(base64Url.decode(base64EncodedBody));
    return new ObjectMapper().readValue(body, PassportVisa.class);
  }

  // Checks if the visa is a valid visa
  public boolean isValidVisa(@NonNull String authToken) {
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
      throw new InvalidTokenException("The visa token received from broker is invalid");
    }
    return false;
  }

  public Page<Visa> listVisa(@NonNull Pageable pageable) {
    return visaRepository.findAll(pageable);
  }

  public Visa partialUpdate(@NotNull UUID id, @NonNull VisaRequest updateRequest) {
    val visa = getById(id);
    VISA_CONVERTER.updateVisa(updateRequest, visa);
    return getRepository().save(visa);
  }

  @Mapper(
      nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
      unmappedTargetPolicy = ReportingPolicy.WARN)
  public abstract static class VisaConverter {
    public abstract Visa convertToVisa(VisaRequest request);

    public abstract void updateVisa(VisaRequest request, @MappingTarget Visa visaToUpdate);
  }

  @Override
  public Visa getWithRelationships(UUID uuid) {
    return null;
  }
}
