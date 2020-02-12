package bio.overture.ego.controller;

import static bio.overture.ego.controller.resolver.PageableResolver.LIMIT;
import static bio.overture.ego.controller.resolver.PageableResolver.OFFSET;
import static bio.overture.ego.controller.resolver.PageableResolver.SORT;
import static bio.overture.ego.controller.resolver.PageableResolver.SORTORDER;
import static java.lang.String.format;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.util.StringUtils.isEmpty;
import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

import bio.overture.ego.model.dto.GenericResponse;
import bio.overture.ego.model.dto.MaskDTO;
import bio.overture.ego.model.dto.PageDTO;
import bio.overture.ego.model.dto.PermissionRequest;
import bio.overture.ego.model.dto.PolicyRequest;
import bio.overture.ego.model.dto.PolicyResponse;
import bio.overture.ego.model.entity.Group;
import bio.overture.ego.model.entity.Policy;
import bio.overture.ego.model.enums.Fields;
import bio.overture.ego.model.search.Filters;
import bio.overture.ego.model.search.SearchFilter;
import bio.overture.ego.security.AdminScoped;
import bio.overture.ego.service.GroupPermissionService;
import bio.overture.ego.service.PolicyService;
import bio.overture.ego.service.UserPermissionService;
import bio.overture.ego.utils.IgnoreCaseSortDecorator;
import bio.overture.ego.view.Views;
import com.fasterxml.jackson.annotation.JsonView;
import com.google.common.collect.ImmutableList;
import io.swagger.annotations.*;
import java.util.List;
import java.util.UUID;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

@Slf4j
@RestController
@RequestMapping("/policies")
public class PolicyController {

  /** Dependencies */
  private final PolicyService policyService;

  private final UserPermissionService userPermissionService;
  private final GroupPermissionService groupPermissionService;

  @Autowired
  public PolicyController(
      @NonNull PolicyService policyService,
      @NonNull UserPermissionService userPermissionService,
      @NonNull GroupPermissionService groupPermissionService) {
    this.policyService = policyService;
    this.groupPermissionService = groupPermissionService;
    this.userPermissionService = userPermissionService;
  }

  @AdminScoped
  @RequestMapping(method = GET, value = "/{id}")
  @ApiResponses(
      value = {@ApiResponse(code = 200, message = "Get policy by id", response = Policy.class)})
  @JsonView(Views.REST.class)
  public @ResponseBody Policy get(
      @RequestHeader(value = AUTHORIZATION, required = true) final String accessToken,
      @PathVariable(value = "id", required = true) UUID id) {
    return policyService.getById(id);
  }

  @AdminScoped
  @RequestMapping(method = GET, value = "")
  @ApiImplicitParams({
    @ApiImplicitParam(
        name = Fields.ID,
        required = false,
        dataType = "string",
        paramType = "query",
        value = "Search for ids containing this text"),
    @ApiImplicitParam(
        name = Fields.NAME,
        required = false,
        dataType = "string",
        paramType = "query",
        value = "Search for policies whose names contain this text"),
    @ApiImplicitParam(
        name = LIMIT,
        required = false,
        dataType = "string",
        paramType = "query",
        value = "Number of results to retrieve"),
    @ApiImplicitParam(
        name = OFFSET,
        required = false,
        dataType = "string",
        paramType = "query",
        value = "Index of first result to retrieve"),
    @ApiImplicitParam(
        name = SORT,
        required = false,
        dataType = "string",
        paramType = "query",
        value = "Field to sort on"),
    @ApiImplicitParam(
        name = SORTORDER,
        required = false,
        dataType = "string",
        paramType = "query",
        value = "Sorting order: ASC|DESC. Default order: DESC"),
  })
  @ApiResponses(value = {@ApiResponse(code = 200, message = "Page Policies")})
  @JsonView(Views.REST.class)
  public @ResponseBody PageDTO<Policy> getPolicies(
      @RequestHeader(value = AUTHORIZATION, required = true) final String accessToken,
      @ApiIgnore @Filters List<SearchFilter> filters,
      Pageable pageable) {
    return new PageDTO<>(policyService.listPolicies(filters, pageable));
  }

  @AdminScoped
  @RequestMapping(method = POST, value = "")
  @ApiResponses(
      value = {
        @ApiResponse(code = 200, message = "New Policy", response = Policy.class),
      })
  public @ResponseBody Policy create(
      @RequestHeader(value = AUTHORIZATION, required = true) final String accessToken,
      @RequestBody(required = true) PolicyRequest createRequest) {
    return policyService.create(createRequest);
  }

  @AdminScoped
  @RequestMapping(method = PUT, value = "/{id}")
  @ApiResponses(
      value = {@ApiResponse(code = 200, message = "Updated Policy", response = Policy.class)})
  public @ResponseBody Policy update(
      @RequestHeader(value = AUTHORIZATION, required = true) final String accessToken,
      @PathVariable(value = "id") UUID id,
      @RequestBody(required = true) PolicyRequest updatedRequst) {
    return policyService.partialUpdate(id, updatedRequst);
  }

  @AdminScoped
  @RequestMapping(method = DELETE, value = "/{id}")
  @ResponseStatus(value = HttpStatus.OK)
  public void delete(
      @RequestHeader(value = AUTHORIZATION, required = true) final String accessToken,
      @PathVariable(value = "id", required = true) UUID id) {
    policyService.delete(id);
  }

  @AdminScoped
  @RequestMapping(method = POST, value = "/{id}/permission/group/{group_id}")
  @ApiResponses(
      value = {@ApiResponse(code = 200, message = "Add group permission", response = String.class)})
  @JsonView(Views.REST.class)
  public @ResponseBody Group createGroupPermission(
      @RequestHeader(value = AUTHORIZATION, required = true) final String accessToken,
      @PathVariable(value = "id", required = true) UUID id,
      @PathVariable(value = "group_id", required = true) UUID groupId,
      @RequestBody(required = true) MaskDTO maskDTO) {
    return groupPermissionService.addPermissions(
        groupId, ImmutableList.of(new PermissionRequest(id, maskDTO.getMask())));
  }

  @AdminScoped
  @RequestMapping(method = DELETE, value = "/{id}/permission/group/{group_id}")
  @ApiResponses(
      value = {
        @ApiResponse(
            code = 200,
            message = "Delete group permission",
            response = GenericResponse.class)
      })
  public @ResponseBody GenericResponse deleteGroupPermission(
      @RequestHeader(value = AUTHORIZATION, required = true) final String accessToken,
      @PathVariable(value = "id", required = true) UUID id,
      @PathVariable(value = "group_id", required = true) UUID groupId) {
    groupPermissionService.deleteByPolicyAndOwner(id, groupId);
    return new GenericResponse(
        format("Deleted permission for group '%s' on policy '%s'.", groupId, id));
  }

  @AdminScoped
  @RequestMapping(method = POST, value = "/{id}/permission/user/{user_id}")
  @ApiResponses(
      value = {@ApiResponse(code = 200, message = "Add user permission", response = String.class)})
  public @ResponseBody String createUserPermission(
      @RequestHeader(value = AUTHORIZATION, required = true) final String accessToken,
      @PathVariable(value = "id", required = true) UUID id,
      @PathVariable(value = "user_id", required = true) UUID userId,
      @RequestBody(required = true) MaskDTO maskDTO) {
    userPermissionService.addPermissions(
        userId, ImmutableList.of(new PermissionRequest(id, maskDTO.getMask())));
    // TODO [rtisma]: change this to actually return proper response
    return "1 user permission successfully added to ACL '" + id + "'";
  }

  @AdminScoped
  @RequestMapping(method = DELETE, value = "/{id}/permission/user/{user_id}")
  @ApiResponses(
      value = {
        @ApiResponse(
            code = 200,
            message = "Delete group permission",
            response = GenericResponse.class)
      })
  public @ResponseBody GenericResponse deleteUserPermission(
      @RequestHeader(value = AUTHORIZATION, required = true) final String accessToken,
      @PathVariable(value = "id", required = true) UUID id,
      @PathVariable(value = "user_id", required = true) UUID userId) {

    userPermissionService.deleteByPolicyAndOwner(id, userId);
    return new GenericResponse(
        format("Deleted permission for user '%s' on policy '%s'.", userId, id));
  }

  @AdminScoped
  @RequestMapping(method = GET, value = "/{id}/users")
  @ApiImplicitParams({
    @ApiImplicitParam(
        name = LIMIT,
        required = false,
        dataType = "string",
        paramType = "query",
        value = "Number of results to retrieve"),
    @ApiImplicitParam(
        name = OFFSET,
        required = false,
        dataType = "string",
        paramType = "query",
        value = "Index of first result to retrieve"),
    @ApiImplicitParam(
        name = SORT,
        required = false,
        dataType = "string",
        paramType = "query",
        value = "Field to sort on"),
    @ApiImplicitParam(
        name = SORTORDER,
        required = false,
        dataType = "string",
        paramType = "query",
        value = "Sorting order: ASC|DESC. Default order: DESC"),
  })
  @ApiResponses(
      value = {
        @ApiResponse(
            code = 200,
            message = "Get list of user ids with given policy id",
            response = String.class)
      })
  public @ResponseBody PageDTO<PolicyResponse> findUserIds(
      @RequestHeader(value = AUTHORIZATION, required = true) final String accessToken,
      @PathVariable(value = "id", required = true) UUID id,
      @ApiParam(
              value = "Query string compares to AccessLevel and user Id and Name fields.",
              required = false)
          @RequestParam(value = "query", required = false)
          String query,
      @ApiIgnore @Filters List<SearchFilter> filters,
      Pageable pageable) {
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
  @ApiImplicitParams({
    @ApiImplicitParam(
        name = LIMIT,
        required = false,
        dataType = "string",
        paramType = "query",
        value = "Number of results to retrieve"),
    @ApiImplicitParam(
        name = OFFSET,
        required = false,
        dataType = "string",
        paramType = "query",
        value = "Index of first result to retrieve"),
    @ApiImplicitParam(
        name = SORT,
        required = false,
        dataType = "string",
        paramType = "query",
        value = "Field to sort on"),
    @ApiImplicitParam(
        name = SORTORDER,
        required = false,
        dataType = "string",
        paramType = "query",
        value = "Sorting order: ASC|DESC. Default order: DESC"),
  })
  @ApiResponses(
      value = {
        @ApiResponse(
            code = 200,
            message = "Get list of group ids with given policy id",
            response = String.class)
      })
  public @ResponseBody PageDTO<PolicyResponse> findGroupIds(
      @RequestHeader(value = AUTHORIZATION, required = true) final String accessToken,
      @PathVariable(value = "id", required = true) UUID id,
      @ApiParam(
              value = "Query string compares to AccessLevel and group Id and Name fields.",
              required = false)
          @RequestParam(value = "query", required = false)
          String query,
      @ApiIgnore @Filters List<SearchFilter> filters,
      Pageable pageable) {
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
}
