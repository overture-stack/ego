package org.overture.ego.controller;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.overture.ego.model.dto.PageDTO;
import org.overture.ego.model.entity.AclEntity;
import org.overture.ego.model.entity.Application;
import org.overture.ego.model.exceptions.PostWithIdentifierException;
import org.overture.ego.model.search.Filters;
import org.overture.ego.model.search.SearchFilter;
import org.overture.ego.security.AdminScoped;
import org.overture.ego.service.AclEntityService;
import org.overture.ego.view.Views;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/acl-entity")
public class AclEntityController {

  @Autowired
  private AclEntityService aclEntityService;

  @AdminScoped
  @RequestMapping(method = RequestMethod.GET, value = "/{id}")
  @ApiResponses(
    value = {
      @ApiResponse(code = 200, message = "Get resource by id", response = AclEntity.class)
    }
  )
  @JsonView(Views.REST.class)
  public @ResponseBody
  AclEntity get(
    @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = true) final String accessToken,
    @PathVariable(value = "id", required = true) String applicationId) {
    return aclEntityService.get(applicationId);
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
          @ApiResponse(code = 200, message = "Page of ACL Entities", response = PageDTO.class)
      }
  )
  @JsonView(Views.REST.class)
  public @ResponseBody
  PageDTO<AclEntity> getAclEntityList(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = true) final String accessToken,
      @ApiIgnore @Filters List<SearchFilter> filters,
      Pageable pageable) {
    return new PageDTO<>(aclEntityService.listAclEntities(filters, pageable));
  }

  @AdminScoped
  @RequestMapping(method = RequestMethod.POST, value = "")
  @ApiResponses(
        value = {
          @ApiResponse(code = 200, message = "New ACL Entity", response = AclEntity.class),
          @ApiResponse(code = 400, message = PostWithIdentifierException.reason, response=AclEntity.class)
      }
  )
  public @ResponseBody
  AclEntity create(
    @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = true) final String accessToken,
    @RequestBody(required = true) AclEntity aclEntity ) {
    if (aclEntity.getId() != null) {
      throw new PostWithIdentifierException();
    }
    return aclEntityService.create(aclEntity);
  }

  @AdminScoped
  @RequestMapping(method = RequestMethod.PUT, value = "/{id}")
  @ApiResponses(
      value = {
          @ApiResponse(code = 200, message = "Updated ACL Entity", response = AclEntity.class)
      }
  )
  public @ResponseBody
  AclEntity update(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = true) final String accessToken,
      @RequestBody(required = true) AclEntity updatedAclEntity) {
    return aclEntityService.update(updatedAclEntity);
  }

  @AdminScoped
  @RequestMapping(method = RequestMethod.DELETE, value = "/{id}")
  @ResponseStatus(value = HttpStatus.OK)
  public void delete(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = true) final String accessToken,
      @PathVariable(value = "id", required = true) String id) {
    aclEntityService.delete(id);
  }
}
