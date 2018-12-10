package bio.overture.ego.controller;

import bio.overture.ego.model.dto.PageDTO;
import bio.overture.ego.model.dto.PolicyResponse;
import bio.overture.ego.model.entity.Policy;
import bio.overture.ego.model.exceptions.PostWithIdentifierException;
import bio.overture.ego.model.params.PolicyIdStringWithAccessLevel;
import bio.overture.ego.model.search.Filters;
import bio.overture.ego.model.search.SearchFilter;
import bio.overture.ego.security.AdminScoped;
import bio.overture.ego.service.*;
import bio.overture.ego.view.Views;
import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

@Slf4j
@RestController
@RequestMapping("/policies")
public class PolicyController {
  private final PolicyService policyService;
  private final GroupService groupService;
  private final UserService userService;
  private final UserPermissionService userPermissionService;
  private final GroupPermissionService groupPermissionService;

  @Autowired
  public PolicyController(
      PolicyService policyService,
      GroupService groupService,
      UserService userService,
      UserPermissionService userPermissionService,
      GroupPermissionService groupPermissionService) {
    this.policyService = policyService;
    this.groupService = groupService;
    this.userService = userService;
    this.groupPermissionService = groupPermissionService;
    this.userPermissionService = userPermissionService;
  }

  @AdminScoped
  @RequestMapping(method = RequestMethod.GET, value = "/{id}")
  @ApiResponses(
      value = {@ApiResponse(code = 200, message = "Get policy by id", response = Policy.class)})
  @JsonView(Views.REST.class)
  public @ResponseBody Policy get(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = true) final String accessToken,
      @PathVariable(value = "id", required = true) String applicationId) {
    return policyService.get(applicationId);
  }

  @AdminScoped
  @RequestMapping(method = RequestMethod.GET, value = "")
  @ApiImplicitParams({
    @ApiImplicitParam(
        name = "limit",
        dataType = "string",
        paramType = "query",
        value = "Number of results to retrieve"),
    @ApiImplicitParam(
        name = "offset",
        dataType = "string",
        paramType = "query",
        value = "Index of first result to retrieve"),
    @ApiImplicitParam(
        name = "sort",
        dataType = "string",
        paramType = "query",
        value = "Field to sort on"),
    @ApiImplicitParam(
        name = "sortOrder",
        dataType = "string",
        paramType = "query",
        value = "Sorting order: ASC|DESC. Default order: DESC"),
    @ApiImplicitParam(
        name = "status",
        dataType = "string",
        paramType = "query",
        value =
            "Filter by status. "
                + "You could also specify filters on any field of the policy being queried as "
                + "query parameters in this format: name=something")
  })
  @ApiResponses(
      value = {@ApiResponse(code = 200, message = "Page of Policies", response = PageDTO.class)})
  @JsonView(Views.REST.class)
  public @ResponseBody PageDTO<Policy> getPolicies(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = true) final String accessToken,
      @ApiIgnore @Filters List<SearchFilter> filters,
      Pageable pageable) {
    return new PageDTO<>(policyService.listPolicies(filters, pageable));
  }

  @AdminScoped
  @RequestMapping(method = RequestMethod.POST, value = "")
  @ApiResponses(
      value = {
        @ApiResponse(code = 200, message = "New Policy", response = Policy.class),
        @ApiResponse(
            code = 400,
            message = PostWithIdentifierException.reason,
            response = Policy.class)
      })
  public @ResponseBody Policy create(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = true) final String accessToken,
      @RequestBody(required = true) Policy policy) {
    if (policy.getId() != null) {
      throw new PostWithIdentifierException();
    }
    return policyService.create(policy);
  }

  @AdminScoped
  @RequestMapping(method = RequestMethod.PUT, value = "/{id}")
  @ApiResponses(
      value = {@ApiResponse(code = 200, message = "Updated Policy", response = Policy.class)})
  public @ResponseBody Policy update(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = true) final String accessToken,
      @RequestBody(required = true) Policy updatedPolicy) {
    return policyService.update(updatedPolicy);
  }

  @AdminScoped
  @RequestMapping(method = RequestMethod.DELETE, value = "/{id}")
  @ResponseStatus(value = HttpStatus.OK)
  public void delete(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = true) final String accessToken,
      @PathVariable(value = "id", required = true) String id) {
    policyService.delete(id);
  }

  @AdminScoped
  @RequestMapping(method = RequestMethod.POST, value = "/{id}/permission/group/{group_id}")
  @ApiResponses(
      value = {@ApiResponse(code = 200, message = "Add user permission", response = String.class)})
  public @ResponseBody String createGroupPermission(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = true) final String accessToken,
      @PathVariable(value = "id", required = true) String id,
      @PathVariable(value = "group_id", required = true) String groupId,
      @RequestBody(required = true) String mask) {
    val permission = new PolicyIdStringWithAccessLevel(id, mask);
    val list = new ArrayList<PolicyIdStringWithAccessLevel>();
    list.add(permission);
    groupService.addGroupPermissions(groupId, list);
    return "1 group permission added to ACL successfully";
  }

  @AdminScoped
  @RequestMapping(method = RequestMethod.POST, value = "/{id}/permission/user/{user_id}")
  @ApiResponses(
      value = {@ApiResponse(code = 200, message = "Add user permission", response = String.class)})
  public @ResponseBody String createUserPermission(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = true) final String accessToken,
      @PathVariable(value = "id", required = true) String id,
      @PathVariable(value = "user_id", required = true) String userId,
      @RequestBody(required = true) String mask) {
    val permission = new PolicyIdStringWithAccessLevel(id, mask);
    val list = new ArrayList<PolicyIdStringWithAccessLevel>();
    list.add(permission);
    userService.addUserPermissions(userId, list);

    return "1 user permission successfully added to ACL '" + id + "'";
  }

  @AdminScoped
  @RequestMapping(method = RequestMethod.GET, value = "/{id}/users")
  @ApiResponses(
      value = {
        @ApiResponse(
            code = 200,
            message = "Get list of user ids with given policy id",
            response = String.class)
      })
  public @ResponseBody List<PolicyResponse> findUserIds(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = true) final String accessToken,
      @PathVariable(value = "id", required = true) String id) {
    return userPermissionService.findByPolicy(id);
  }

  @AdminScoped
  @RequestMapping(method = RequestMethod.GET, value = "/{id}/groups")
  @ApiResponses(
      value = {
        @ApiResponse(
            code = 200,
            message = "Get list of user ids with given policy id",
            response = String.class)
      })
  public @ResponseBody List<PolicyResponse> findGroupIds(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = true) final String accessToken,
      @PathVariable(value = "id", required = true) String id) {
    return groupPermissionService.findByPolicy(id);
  }
}
