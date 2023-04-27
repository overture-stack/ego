package bio.overture.ego.controller;

import static bio.overture.ego.controller.resolver.PageableResolver.LIMIT;
import static bio.overture.ego.controller.resolver.PageableResolver.OFFSET;
import static bio.overture.ego.controller.resolver.PageableResolver.SORT;
import static bio.overture.ego.controller.resolver.PageableResolver.SORTORDER;
import static bio.overture.ego.model.dto.GenericResponse.createGenericResponse;
import static org.springframework.util.StringUtils.isEmpty;
import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

import bio.overture.ego.model.dto.*;
import bio.overture.ego.model.entity.Application;
import bio.overture.ego.model.entity.Group;
import bio.overture.ego.model.entity.Policy;
import bio.overture.ego.model.entity.User;
import bio.overture.ego.model.enums.Fields;
import bio.overture.ego.model.search.Filters;
import bio.overture.ego.model.search.SearchFilter;
import bio.overture.ego.security.AdminScoped;
import bio.overture.ego.service.ApplicationPermissionService;
import bio.overture.ego.service.GroupPermissionService;
import bio.overture.ego.service.PolicyService;
import bio.overture.ego.service.UserPermissionService;
import bio.overture.ego.utils.IgnoreCaseSortDecorator;
import bio.overture.ego.view.Views;
import com.fasterxml.jackson.annotation.JsonView;
import com.google.common.collect.ImmutableList;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/policies")
@Tag(name = "Policies")
public class PolicyController {

  /** Dependencies */
  private final PolicyService policyService;

  private final UserPermissionService userPermissionService;
  private final GroupPermissionService groupPermissionService;
  private final ApplicationPermissionService applicationPermissionService;

  @Autowired
  public PolicyController(
      @NonNull PolicyService policyService,
      @NonNull UserPermissionService userPermissionService,
      @NonNull GroupPermissionService groupPermissionService,
      @NonNull ApplicationPermissionService applicationPermissionService) {
    this.policyService = policyService;
    this.groupPermissionService = groupPermissionService;
    this.userPermissionService = userPermissionService;
    this.applicationPermissionService = applicationPermissionService;
  }

  @AdminScoped
  @RequestMapping(method = GET, value = "/{id}")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Get policy by id")})
  @JsonView(Views.REST.class)
  public @ResponseBody Policy getPolicy(
      @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = true)
          final String authorization,
      @PathVariable(value = "id", required = true) UUID id) {
    return policyService.getById(id);
  }

  @AdminScoped
  @RequestMapping(method = GET, value = "")
  @Parameters({
    @Parameter(
        name = Fields.ID,
        required = false,
        schema = @Schema(type = "string"),
        in = ParameterIn.QUERY,
        description = "Search for ids containing this text"),
    @Parameter(
        name = Fields.NAME,
        required = false,
        schema = @Schema(type = "string"),
        in = ParameterIn.QUERY,
        description = "Search for policies whose names contain this text"),
    @Parameter(
        name = LIMIT,
        required = false,
        schema = @Schema(type = "string"),
        in = ParameterIn.QUERY,
        description = "Number of results to retrieve"),
    @Parameter(
        name = OFFSET,
        required = false,
        schema = @Schema(type = "string"),
        in = ParameterIn.QUERY,
        description = "Index of first result to retrieve"),
    @Parameter(
        name = SORT,
        required = false,
        schema = @Schema(type = "string"),
        in = ParameterIn.QUERY,
        description = "Field to sort on"),
    @Parameter(
        name = SORTORDER,
        required = false,
        schema = @Schema(type = "string"),
        in = ParameterIn.QUERY,
        description = "Sorting order: ASC|DESC. Default order: DESC"),
  })
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Page Policies")})
  @JsonView(Views.REST.class)
  public @ResponseBody PageDTO<Policy> listPolicies(
      @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = true)
          final String authorization,
      @Parameter(hidden = true) @Filters List<SearchFilter> filters,
      @Parameter(hidden = true) Pageable pageable) {
    return new PageDTO<>(policyService.listPolicies(filters, pageable));
  }

  @AdminScoped
  @RequestMapping(method = POST, value = "")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "New Policy"),
      })
  public @ResponseBody Policy createPolicy(
      @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = true)
          final String authorization,
      @RequestBody(required = true) PolicyRequest createRequest) {
    return policyService.create(createRequest);
  }

  @AdminScoped
  @RequestMapping(method = PUT, value = "/{id}")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Updated Policy")})
  public @ResponseBody Policy updatePolicy(
      @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = true)
          final String authorization,
      @PathVariable(value = "id") UUID id,
      @RequestBody(required = true) PolicyRequest updatedRequst) {
    return policyService.partialUpdate(id, updatedRequst);
  }

  @AdminScoped
  @RequestMapping(method = DELETE, value = "/{id}")
  @ResponseStatus(value = HttpStatus.OK)
  public void deletePolicy(
      @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = true)
          final String authorization,
      @PathVariable(value = "id", required = true) UUID id) {
    policyService.delete(id);
  }

  @AdminScoped
  @RequestMapping(method = POST, value = "/{id}/permission/group/{group_id}")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Add group permission",
            content = @Content(schema = @Schema(implementation = String.class)))
      })
  @JsonView(Views.REST.class)
  public @ResponseBody Group createGroupPermission(
      @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = true)
          final String authorization,
      @PathVariable(value = "id", required = true) UUID id,
      @PathVariable(value = "group_id", required = true) UUID groupId,
      @RequestBody(required = true) MaskDTO maskDTO) {
    return groupPermissionService.addPermissions(
        groupId, ImmutableList.of(new PermissionRequest(id, maskDTO.getMask())));
  }

  @AdminScoped
  @RequestMapping(method = DELETE, value = "/{id}/permission/group/{group_id}")
  @ApiResponses(
      value = {@ApiResponse(responseCode = "200", description = "Delete group permission")})
  public @ResponseBody GenericResponse deleteGroupPermission(
      @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = true)
          final String authorization,
      @PathVariable(value = "id", required = true) UUID id,
      @PathVariable(value = "group_id", required = true) UUID groupId) {
    groupPermissionService.deleteByPolicyAndOwner(id, groupId);
    return createGenericResponse("Deleted permission for group '%s' on policy '%s'.", groupId, id);
  }

  @AdminScoped
  @RequestMapping(method = POST, value = "/{id}/permission/user/{user_id}")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Add user permission",
            content = @Content(schema = @Schema(implementation = String.class)))
      })
  public @ResponseBody User createUserPermission(
      @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = true)
          final String authorization,
      @PathVariable(value = "id", required = true) UUID id,
      @PathVariable(value = "user_id", required = true) UUID userId,
      @RequestBody(required = true) MaskDTO maskDTO) {
    return userPermissionService.addPermissions(
        userId, ImmutableList.of(new PermissionRequest(id, maskDTO.getMask())));
  }

  @AdminScoped
  @RequestMapping(method = DELETE, value = "/{id}/permission/user/{user_id}")
  @ApiResponses(
      value = {@ApiResponse(responseCode = "200", description = "Delete group permission")})
  public @ResponseBody GenericResponse deleteUserPermission(
      @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = true)
          final String authorization,
      @PathVariable(value = "id", required = true) UUID id,
      @PathVariable(value = "user_id", required = true) UUID userId) {

    userPermissionService.deleteByPolicyAndOwner(id, userId);
    return createGenericResponse("Deleted permission for user '%s' on policy '%s'.", userId, id);
  }

  @AdminScoped
  @RequestMapping(method = POST, value = "/{id}/permission/application/{application_id}")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Add application permission",
            content = @Content(schema = @Schema(implementation = String.class)))
      })
  public @ResponseBody Application createApplicationPermission(
      @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = true)
          final String authorization,
      @PathVariable(value = "id", required = true) UUID id,
      @PathVariable(value = "application_id", required = true) UUID applicationId,
      @RequestBody(required = true) MaskDTO maskDTO) {
    return applicationPermissionService.addPermissions(
        applicationId, ImmutableList.of(new PermissionRequest(id, maskDTO.getMask())));
  }

  @AdminScoped
  @RequestMapping(method = DELETE, value = "/{id}/permission/application/{application_id}")
  @ApiResponses(
      value = {@ApiResponse(responseCode = "200", description = "Delete application permission")})
  public @ResponseBody GenericResponse deleteApplicationPermission(
      @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = true)
          final String authorization,
      @PathVariable(value = "id", required = true) UUID id,
      @PathVariable(value = "application_id", required = true) UUID applicationId) {
    applicationPermissionService.deleteByPolicyAndOwner(id, applicationId);
    return createGenericResponse(
        "Deleted permission for application '%s' on policy '%s'.", applicationId, id);
  }

  @AdminScoped
  @RequestMapping(method = GET, value = "/{id}/users")
  @Parameters({
    @Parameter(
        name = LIMIT,
        required = false,
        schema = @Schema(type = "string"),
        in = ParameterIn.QUERY,
        description = "Number of results to retrieve"),
    @Parameter(
        name = OFFSET,
        required = false,
        schema = @Schema(type = "string"),
        in = ParameterIn.QUERY,
        description = "Index of first result to retrieve"),
    @Parameter(
        name = SORT,
        required = false,
        schema = @Schema(type = "string"),
        in = ParameterIn.QUERY,
        description = "Field to sort on"),
    @Parameter(
        name = SORTORDER,
        required = false,
        schema = @Schema(type = "string"),
        in = ParameterIn.QUERY,
        description =
            "Sorting order: ASC|DESC. Default order: DESC. Note: ascending sort order for the mask field is: READ,WRITE,DENY"),
  })
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Get list of user ids with given policy id",
            content = @Content(schema = @Schema(implementation = String.class)))
      })
  public @ResponseBody PageDTO<PolicyResponse> findUserIds(
      @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = true)
          final String authorization,
      @PathVariable(value = "id", required = true) UUID id,
      @Parameter(
              description = "Query string compares to AccessLevel and user Id field.",
              required = false)
          @RequestParam(value = "query", required = false)
          String query,
      @Parameter(hidden = true) @Filters List<SearchFilter> filters,
      @Parameter(hidden = true) Pageable pageable) {
    val decoratedPageable = new IgnoreCaseSortDecorator(pageable);
    if (isEmpty(query)) {
      return new PageDTO<>(
          userPermissionService.listUserPermissionsByPolicy(id, filters, decoratedPageable));
    } else {
      return new PageDTO<>(
          userPermissionService.findUserPermissionsByPolicy(id, filters, query, decoratedPageable));
    }
  }

  @AdminScoped
  @RequestMapping(method = GET, value = "/{id}/groups")
  @Parameters({
    @Parameter(
        name = LIMIT,
        required = false,
        schema = @Schema(type = "string"),
        in = ParameterIn.QUERY,
        description = "Number of results to retrieve"),
    @Parameter(
        name = OFFSET,
        required = false,
        schema = @Schema(type = "string"),
        in = ParameterIn.QUERY,
        description = "Index of first result to retrieve"),
    @Parameter(
        name = SORT,
        required = false,
        schema = @Schema(type = "string"),
        in = ParameterIn.QUERY,
        description = "Field to sort on"),
    @Parameter(
        name = SORTORDER,
        required = false,
        schema = @Schema(type = "string"),
        in = ParameterIn.QUERY,
        description =
            "Sorting order: ASC|DESC. Default order: DESC. Note: ascending sort order for the mask field is: READ,WRITE,DENY"),
  })
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Get list of group ids with given policy id",
            content = @Content(schema = @Schema(implementation = String.class)))
      })
  public @ResponseBody PageDTO<PolicyResponse> findGroupIds(
      @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = true)
          final String authorization,
      @PathVariable(value = "id", required = true) UUID id,
      @Parameter(
              description = "Query string compares to AccessLevel and group Id and Name fields.",
              required = false)
          @RequestParam(value = "query", required = false)
          String query,
      @Parameter(hidden = true) @Filters List<SearchFilter> filters,
      @Parameter(hidden = true) Pageable pageable) {
    val decoratedPageable = new IgnoreCaseSortDecorator(pageable);
    if (isEmpty(query)) {
      return new PageDTO(
          groupPermissionService.listGroupPermissionsByPolicy(id, filters, decoratedPageable));
    } else {
      return new PageDTO<>(
          groupPermissionService.findGroupPermissionsByPolicy(
              id, filters, query, decoratedPageable));
    }
  }

  @AdminScoped
  @RequestMapping(method = GET, value = "/{id}/applications")
  @Parameters({
    @Parameter(
        name = LIMIT,
        required = false,
        schema = @Schema(type = "string"),
        in = ParameterIn.QUERY,
        description = "Number of results to retrieve"),
    @Parameter(
        name = OFFSET,
        required = false,
        schema = @Schema(type = "string"),
        in = ParameterIn.QUERY,
        description = "Index of first result to retrieve"),
    @Parameter(
        name = SORT,
        required = false,
        schema = @Schema(type = "string"),
        in = ParameterIn.QUERY,
        description = "Field to sort on"),
    @Parameter(
        name = SORTORDER,
        required = false,
        schema = @Schema(type = "string"),
        in = ParameterIn.QUERY,
        description =
            "Sorting order: ASC|DESC. Default order: DESC. Note: ascending sort order for the mask field is: READ,WRITE,DENY"),
  })
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Get list of application ids with given policy id",
            content = @Content(schema = @Schema(implementation = String.class)))
      })
  public @ResponseBody PageDTO<PolicyResponse> findApplicationIds(
      @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = true)
          final String authorization,
      @PathVariable(value = "id", required = true) UUID id,
      @Parameter(
              description =
                  "Query string compares to AccessLevel and Application Id and Name fields.",
              required = false)
          @RequestParam(value = "query", required = false)
          String query,
      @Parameter(hidden = true) @Filters List<SearchFilter> filters,
      @Parameter(hidden = true) Pageable pageable) {
    val decoratedPageable = new IgnoreCaseSortDecorator(pageable);
    if (isEmpty(query)) {
      return new PageDTO(
          applicationPermissionService.listApplicationPermissionsByPolicy(
              id, filters, decoratedPageable));
    } else {
      return new PageDTO<>(
          applicationPermissionService.findApplicationPermissionsByPolicy(
              id, filters, query, decoratedPageable));
    }
  }
}
