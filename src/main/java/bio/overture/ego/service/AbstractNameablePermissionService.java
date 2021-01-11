package bio.overture.ego.service;

import bio.overture.ego.model.dto.PolicyResponse;
import bio.overture.ego.model.entity.*;
import bio.overture.ego.repository.NameablePermissionRepository;
import java.util.*;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Transactional
public abstract class AbstractNameablePermissionService<
        O extends NameableEntity<UUID>, P extends AbstractPermission<O>>
    extends AbstractPermissionService<O, P> {

  /** Dependencies */
  private final BaseService<Policy, UUID> policyBaseService;

  private final BaseService<O, UUID> ownerBaseService;
  private final NameablePermissionRepository<O, P> nameablePermissionRepository;
  private final Class<O> ownerType;

  public AbstractNameablePermissionService(
      @NonNull Class<O> ownerType,
      @NonNull Class<P> entityType,
      @NonNull BaseService<O, UUID> ownerBaseService,
      @NonNull BaseService<Policy, UUID> policyBaseService,
      @NonNull NameablePermissionRepository repository) {
    super(ownerType, entityType, ownerBaseService, policyBaseService, repository);
    this.nameablePermissionRepository = repository;
    this.ownerType = ownerType;
    this.policyBaseService = policyBaseService;
    this.ownerBaseService = ownerBaseService;
  }

  protected PolicyResponse convertToPolicyResponse(@NonNull P p) {
    val name = p.getOwner().getName();
    val id = p.getOwner().getId().toString();
    val mask = p.getAccessLevel();
    return PolicyResponse.builder().name(name).id(id).mask(mask).build();
  }
}
