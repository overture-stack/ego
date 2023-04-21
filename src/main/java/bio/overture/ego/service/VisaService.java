package bio.overture.ego.service;

import static bio.overture.ego.model.exceptions.NotFoundException.checkNotFound;
import static bio.overture.ego.model.exceptions.RequestValidationException.checkRequestValid;
import static org.mapstruct.factory.Mappers.getMapper;

import bio.overture.ego.event.token.ApiKeyEventsPublisher;
import bio.overture.ego.model.dto.VisaRequest;
import bio.overture.ego.model.entity.Visa;
import bio.overture.ego.repository.VisaRepository;
import java.util.Optional;
import java.util.UUID;
import javax.validation.constraints.NotNull;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
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
  private static final VisaService.VisaConverter VISA_CONVERTER =
      getMapper(VisaService.VisaConverter.class);

  /** Dependencies */
  @Autowired private VisaRepository visaRepository;

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

  public void delete(@NonNull UUID id) {
    checkExistence(id);
    super.delete(id);
  }

  @Override
  public Visa getWithRelationships(UUID uuid) {
    return null;
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
}
