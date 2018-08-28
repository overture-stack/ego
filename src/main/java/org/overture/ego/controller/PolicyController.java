package org.overture.ego.controller;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.overture.ego.model.dto.PageDTO;
import org.overture.ego.model.entity.Policy;
import org.overture.ego.model.exceptions.PostWithIdentifierException;
import org.overture.ego.model.params.Scope;
import org.overture.ego.model.search.Filters;
import org.overture.ego.model.search.SearchFilter;
import org.overture.ego.security.AdminScoped;
import org.overture.ego.service.GroupService;
import org.overture.ego.service.PolicyService;
import org.overture.ego.service.UserService;
import org.overture.ego.view.Views;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/policies")
public class PolicyController {

  @Autowired
  private PolicyService policyService;
  private GroupService groupService;
  private UserService userService;

  @AdminScoped
  @RequestMapping(method = RequestMethod.GET, value = "/{id}")
  @ApiResponses(
    value = {
      @ApiResponse(code = 200, message = "Get policy by id", response = Policy.class)
    }
  )
  @JsonView(Views.REST.class)
  public @ResponseBody
  Policy get(
    @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = true) final String accessToken,
    @PathVariable(value = "id", required = true) String applicationId) {
    return policyService.get(applicationId);
  }

  @AdminScoped
  @RequestMapping(method = RequestMethod.GET, value = "")
  @ApiImplicitParams({
    @ApiImplicitParam(name = "limit", dataType = "string", paramType = "query",
      value = "Number of results to retrieve"),
    @ApiImplicitParam(name = "offset", dataType = "string", paramType = "query",
      value = "Index of first result to retrieve"),
    @ApiImplicitParam(name = "sort", dataType = "string", paramType = "query",
      value = "Field to sort on"),
    @ApiImplicitParam(name = "sortOrder", dataType = "string", paramType = "query",
      value = "Sorting order: ASC|DESC. Default order: DESC"),
    @ApiImplicitParam(name = "status", dataType = "string", paramType = "query",
      value = "Filter by status. " +
        "You could also specify filters on any field of the entity being queried as " +
        "query parameters in this format: name=something")

  })
  @ApiResponses(
    value = {
      @ApiResponse(code = 200, message = "Page of Policies", response = PageDTO.class)
    }
  )
  @JsonView(Views.REST.class)
  public @ResponseBody
  PageDTO<Policy> getPolicies(
    @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = true) final String accessToken,
    @ApiIgnore @Filters List<SearchFilter> filters,
    Pageable pageable) {
    return new PageDTO<>(policyService.listAclEntities(filters, pageable));
  }

  @AdminScoped
  @RequestMapping(method = RequestMethod.POST, value = "")
  @ApiResponses(
    value = {
      @ApiResponse(code = 200, message = "New ACL Entity", response = Policy.class),
      @ApiResponse(code = 400, message = PostWithIdentifierException.reason, response = Policy.class)
    }
  )
  public @ResponseBody
  Policy create(
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
    value = {
      @ApiResponse(code = 200, message = "Updated Scope", response = Policy.class)
    }
  )
  public @ResponseBody
  Policy update(
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

  public @ResponseBody
  String createGroupPermission(
    @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = true) final String accessToken,
    @PathVariable(value = "id", required = true) String id,
    @PathVariable(value = "group_id", required = true) String groupId,
    @RequestBody(required = true) String mask
  ) {
    val permission = new Scope(id, mask);
    val list = new ArrayList<Scope>();
    list.add(permission);
    groupService.addGroupPermissions(groupId, list);
    return "1 group permission added to ACL successfully";
  }

  @AdminScoped
  @RequestMapping(method = RequestMethod.POST, value = "/{id}/permission/user/{user_id}")
  @ApiResponses(
    value = {
      @ApiResponse(code = 200, message = "Add user permission", response = String.class)
    }
  )
  public @ResponseBody
  String createUserPermission(
    @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = true) final String accessToken,
    @PathVariable(value = "id", required = true) String id,
    @PathVariable(value = "user_id", required = true) String userId,
    @RequestBody(required = true) String mask
  ) {
    val permission = new Scope(id, mask);
    val list = new ArrayList<Scope>();
    list.add(permission);
    userService.addUserPermissions(userId, list);

    return "1 user permission successfully added to ACL '" + id + "'";
  }

}
