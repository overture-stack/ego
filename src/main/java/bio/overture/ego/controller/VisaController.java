package bio.overture.ego.controller;

import static bio.overture.ego.controller.resolver.PageableResolver.*;
import static org.springframework.web.bind.annotation.RequestMethod.*;

import bio.overture.ego.model.dto.*;
import bio.overture.ego.model.entity.*;
import bio.overture.ego.security.AdminScoped;
import bio.overture.ego.service.*;
import bio.overture.ego.view.Views;
import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.annotations.*;
import java.util.List;
import java.util.UUID;
import javax.validation.constraints.NotNull;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

@Slf4j
@RestController
@RequestMapping("/visa")
@Api(tags = "Visa")
public class VisaController {

  /** Dependencies */
  private final VisaService visaService;

  private final VisaPermissionService visaPermissionService;

  private final UserPermissionService userPermissionService;
  private final GroupPermissionService groupPermissionService;
  private final ApplicationPermissionService applicationPermissionService;

  @Autowired
  public VisaController(
      @NonNull VisaService visaService,
      @NotNull VisaPermissionService visaPermissionService,
      @NonNull UserPermissionService userPermissionService,
      @NonNull GroupPermissionService groupPermissionService,
      @NonNull ApplicationPermissionService applicationPermissionService) {
    this.visaService = visaService;
    this.visaPermissionService = visaPermissionService;
    this.groupPermissionService = groupPermissionService;
    this.userPermissionService = userPermissionService;
    this.applicationPermissionService = applicationPermissionService;
  }

  /*
   * This method is used to fetch visa using id
   * @param id UUID
   * @return visa Visa
   */
  @AdminScoped
  @RequestMapping(method = GET, value = "/{id}")
  @ApiResponses(
      value = {@ApiResponse(code = 200, message = "Get Visa by id", response = Visa.class)})
  @JsonView(Views.REST.class)
  public @ResponseBody Visa getVisa(
      @ApiIgnore @RequestHeader(value = "Authorization", required = true)
          final String authorization,
      @PathVariable(value = "id", required = true) UUID id) {
    return visaService.getById(id);
  }

  @AdminScoped
  @RequestMapping(method = GET, value = "")
  @ApiResponses(value = {@ApiResponse(code = 200, message = "All Visas")})
  @JsonView(Views.REST.class)
  public @ResponseBody PageDTO<Visa> listVisa(
      @ApiIgnore @RequestHeader(value = "Authorization", required = true)
          final String authorization,
      @ApiIgnore Pageable pageable) {
    return new PageDTO<>(visaService.listVisa(pageable));
  }

  @AdminScoped
  @RequestMapping(method = POST, value = "")
  @ApiResponses(
      value = {
        @ApiResponse(code = 200, message = "New Visa", response = Visa.class),
      })
  public @ResponseBody Visa createVisa(
      @ApiIgnore @RequestHeader(value = "Authorization", required = true)
          final String authorization,
      @RequestBody(required = true) VisaRequest visaRequest) {
    return visaService.create(visaRequest);
  }

  @AdminScoped
  @RequestMapping(method = PUT, value = "/{id}")
  @ApiResponses(value = {@ApiResponse(code = 200, message = "Update Visa", response = Visa.class)})
  public @ResponseBody Visa updateVisa(
      @ApiIgnore @RequestHeader(value = "Authorization", required = true)
          final String authorization,
      @PathVariable(value = "id", required = true) UUID id,
      @RequestBody(required = true) VisaRequest visaRequest) {
    return visaService.partialUpdate(id, visaRequest);
  }

  @AdminScoped
  @RequestMapping(method = DELETE, value = "/{id}")
  @ResponseStatus(value = HttpStatus.OK)
  public void deleteVisa(
      @ApiIgnore @RequestHeader(value = "Authorization", required = true)
          final String authorization,
      @PathVariable(value = "id", required = true) UUID id) {
    visaService.delete(id);
  }

  /*
   * This method is used to fetch visa permissions using visa id
   * @param visaId UUID
   * @return visaPermissions List<VisaPermissions>
   */
  @AdminScoped
  @RequestMapping(method = GET, value = "/permissions/visaId/{id}")
  @ApiResponses(value = {@ApiResponse(code = 200, message = "Get VisaPermissions by visaId")})
  @JsonView(Views.REST.class)
  public @ResponseBody List<VisaPermission> getPermissionsByVisaId(
      @ApiIgnore @RequestHeader(value = "Authorization", required = true)
          final String authorization,
      @PathVariable(value = "id", required = true) UUID id) {
    return visaPermissionService.getPermissionsByVisaId(id);
  }

  /*
   * This method is used to fetch visa permissions using policy id
   * @param policyId UUID
   * @return visaPermissions List<VisaPermissions>
   */
  @AdminScoped
  @RequestMapping(method = GET, value = "/permissions/policyId/{id}")
  @ApiResponses(value = {@ApiResponse(code = 200, message = "Get VisaPermissions by policyId")})
  @JsonView(Views.REST.class)
  public @ResponseBody List<VisaPermission> getPermissionsByPolicyId(
      @ApiIgnore @RequestHeader(value = "Authorization", required = true)
          final String authorization,
      @PathVariable(value = "id", required = true) UUID id) {
    return visaPermissionService.getPermissionsByPolicyId(id);
  }

  /*
   * This method is used to create/update visa permissions
   * @param visaPermissionRequest VisaPermissionRequest
   * @return visaPermission VisaPermission
   */
  @AdminScoped
  @RequestMapping(method = POST, value = "/permissions")
  @ApiResponses(value = {@ApiResponse(code = 200, message = "Create or Update VisaPermission")})
  @JsonView(Views.REST.class)
  public @ResponseBody VisaPermission createOrUpdatePermissions(
      @ApiIgnore @RequestHeader(value = "Authorization", required = true)
          final String authorization,
      @RequestBody(required = true) VisaPermissionRequest visaPermissionRequest) {
    return visaPermissionService.createOrUpdatePermissions(visaPermissionRequest);
  }
}
