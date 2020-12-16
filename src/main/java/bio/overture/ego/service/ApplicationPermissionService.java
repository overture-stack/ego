package bio.overture.ego.service;

import static bio.overture.ego.repository.queryspecification.ApplicationPermissionSpecification.buildFilterAndQuerySpecification;
import static bio.overture.ego.repository.queryspecification.ApplicationPermissionSpecification.buildFilterSpecification;
import static bio.overture.ego.utils.CollectionUtils.mapToImmutableSet;
import static bio.overture.ego.utils.CollectionUtils.mapToList;
import static java.util.stream.Collectors.toUnmodifiableList;
import static java.util.stream.Collectors.toUnmodifiableSet;

import bio.overture.ego.event.token.ApiKeyEventsPublisher;
import bio.overture.ego.model.dto.PermissionRequest;
import bio.overture.ego.model.dto.PolicyResponse;
import bio.overture.ego.model.dto.ResolvedPermissionResponse;
import bio.overture.ego.model.entity.*;
import bio.overture.ego.model.join.GroupApplication;
import bio.overture.ego.model.join.UserApplication;
import bio.overture.ego.model.search.SearchFilter;
import bio.overture.ego.repository.ApplicationPermissionRepository;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ApplicationPermissionService
    extends AbstractNameablePermissionService<Application, ApplicationPermission> {

  /** Dependencies * */
  private final ApplicationService applicationService;

  private final ApiKeyEventsPublisher apiKeyEventsPublisher;

  @Autowired
  public ApplicationPermissionService(
      @NonNull ApplicationPermissionRepository applicationPermissionRepository,
      @NonNull ApplicationService applicationService,
      @NonNull ApiKeyEventsPublisher apiKeyEventsPublisher,
      @NonNull PolicyService policyService) {
    super(
        Application.class,
        ApplicationPermission.class,
        applicationService,
        policyService,
        applicationPermissionRepository);
    this.applicationService = applicationService;
    this.apiKeyEventsPublisher = apiKeyEventsPublisher;
  }

  /**
   * Decorates the call to addPermissions with the functionality to also cleanup user tokens in the
   * event that the permission added downgrades the available scopes to the users of this
   * application.
   *
   * @param applicationId Id of the application whose permissions are being added or updated
   * @param permissionRequests A list of permission changes
   */
  @Override
  public Application addPermissions(
      @NonNull UUID applicationId, @NonNull List<PermissionRequest> permissionRequests) {
    val application = super.addPermissions(applicationId, permissionRequests);
    val users = mapToImmutableSet(application.getUserApplications(), UserApplication::getUser);
    apiKeyEventsPublisher.requestApiKeyCleanupByUsers(users);
    return application;
  }

  /**
   * Decorates the call to deletePermissions with the functionality to also cleanup user tokens
   *
   * @param applicationId Id of the application whose permissions are being deleted
   * @param idsToDelete Ids of the permission to delete
   */
  @Override
  public void deletePermissions(
      @NonNull UUID applicationId, @NonNull Collection<UUID> idsToDelete) {
    super.deletePermissions(applicationId, idsToDelete);
    val application = applicationService.getWithRelationships(applicationId);
    val users = mapToImmutableSet(application.getUserApplications(), UserApplication::getUser);
    apiKeyEventsPublisher.requestApiKeyCleanupByUsers(users);
  }

  @Override
  protected Collection<ApplicationPermission> getPermissionsFromOwner(@NonNull Application owner) {
    return owner.getApplicationPermissions();
  }

  @Override
  protected Collection<ApplicationPermission> getPermissionsFromPolicy(@NonNull Policy policy) {
    return policy.getApplicationPermissions();
  }

  @SuppressWarnings("unchecked")
  public Page<PolicyResponse> listApplicationPermissionsByPolicy(
      @NonNull UUID policyId, List<SearchFilter> filters, @NonNull Pageable pageable) {

    val applicationPermissions =
        (Page<ApplicationPermission>)
            getRepository().findAll(buildFilterSpecification(policyId, filters), pageable);

    val responses =
        applicationPermissions.stream()
            .map(this::convertToPolicyResponse)
            .collect(toUnmodifiableList());

    return new PageImpl<>(responses, pageable, applicationPermissions.getTotalElements());
  }

  @SuppressWarnings("unchecked")
  public Page<PolicyResponse> findApplicationPermissionsByPolicy(
      @NonNull UUID policyId,
      List<SearchFilter> filters,
      String query,
      @NonNull Pageable pageable) {
    val applicationPermissions =
        (Page<ApplicationPermission>)
            getRepository()
                .findAll(buildFilterAndQuerySpecification(policyId, filters, query), pageable);

    val responses =
        applicationPermissions.stream()
            .map(this::convertToPolicyResponse)
            .collect(toUnmodifiableList());

    return new PageImpl<>(responses, pageable, applicationPermissions.getTotalElements());
  }

  @SuppressWarnings("unchecked")
  public List<ResolvedPermissionResponse> getResolvedPermissions(@NonNull UUID applicationId) {
    val app = applicationService.getWithRelationships(applicationId);
    val appPermissions = app.getApplicationPermissions();
    val groupPermissions =
        mapToList(app.getGroupApplications(), GroupApplication::getGroup).stream()
            .map(Group::getPermissions)
            .flatMap(Collection::stream)
            .collect(toUnmodifiableSet());
    return getResolvedPermissionsResponse(appPermissions, groupPermissions);
  }
}
