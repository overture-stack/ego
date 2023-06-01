package bio.overture.ego.controller;

import static org.springframework.web.bind.annotation.RequestMethod.*;

import bio.overture.ego.model.dto.*;
import bio.overture.ego.model.entity.*;
import bio.overture.ego.security.AdminScoped;
import bio.overture.ego.service.*;
import bio.overture.ego.view.Views;
import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/visa")
@Tag(name = "Visa")
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
   * This method is used to fetch visa using type and value
   * @param type String
   * @param value String
   * @return visas List<Visa>
   */
  @AdminScoped
  @RequestMapping(method = GET, value = "/{type}/{value}")
  @ApiResponses(
      value = {@ApiResponse(responseCode = "200", description = "Get Visa Using Type and Value")})
  @JsonView(Views.REST.class)
  public @ResponseBody List<Visa> getVisaByTypeAndValue(
      @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = true)
          final String authorization,
      @PathVariable(value = "type", required = true) String type,
      @PathVariable(value = "value", required = true) String value) {
    return visaService.getByTypeAndValue(type, value);
  }

  /*
   * This method is used to list all visas
   * @return visas List<Visa>
   */
  @AdminScoped
  @RequestMapping(method = GET, value = "")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "All Visas")})
  @JsonView(Views.REST.class)
  public @ResponseBody PageDTO<Visa> listVisa(
      @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = true)
          final String authorization,
      @Parameter(hidden = true) Pageable pageable) {
    return new PageDTO<>(visaService.listVisa(pageable));
  }

  /*
   * This method is used to create visa using visa create request
   * @param visaRequest VisaRequest
   * @return Visa visa
   */
  @AdminScoped
  @RequestMapping(method = POST, value = "")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "New Visa"),
      })
  public @ResponseBody Visa createVisa(
      @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = true)
          final String authorization,
      @RequestBody(required = true) VisaRequest visaRequest) {
    return visaService.create(visaRequest);
  }

  /*
   * This method is used to update visa using visa id and update request
   * @param visaId UUID
   * @param visaRequest VisaRequest
   * @return Visa visa
   */
  @AdminScoped
  @RequestMapping(method = PUT, value = "/{id}")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Update Visa")})
  public @ResponseBody Visa updateVisa(
      @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = true)
          final String authorization,
      @PathVariable(value = "id", required = true) UUID id,
      @RequestBody(required = true) VisaRequest visaRequest) {
    return visaService.partialUpdate(id, visaRequest);
  }

  /*
   * This method is used to delete visa using visa id
   * @param visaId UUID
   */
  @AdminScoped
  @RequestMapping(method = DELETE, value = "/{type}/{value}")
  @ResponseStatus(value = HttpStatus.OK)
  public void deleteVisa(
      @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = true)
          final String authorization,
      @PathVariable(value = "type", required = true) String type,
      @PathVariable(value = "value", required = true) String value) {
    visaService.delete(type, value);
  }

  /*
   * This method is used to fetch visa permissions using type and value
   * @param type String
   * @param value String
   * @return visas List<VisaPermissions>
   */
  @AdminScoped
  @RequestMapping(method = GET, value = "/permissions/{type}/{value}")
  @ApiResponses(
      value = {@ApiResponse(responseCode = "200", description = "Get Visa Using Type and Value")})
  @JsonView(Views.REST.class)
  public @ResponseBody List<VisaPermission> getVisaPermissionsByTypeAndValue(
      @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = true)
          final String authorization,
      @PathVariable(value = "type", required = true) String type,
      @PathVariable(value = "value", required = true) String value) {
    return visaPermissionService.getPermissionsForVisa(visaService.getByTypeAndValue(type, value));
  }

  /*
   * This method is used to fetch visa permissions using policy id
   * @param policyId UUID
   * @return visaPermissions List<VisaPermissions>
   */
  @AdminScoped
  @RequestMapping(method = GET, value = "/permissions/policyId/{id}")
  @ApiResponses(
      value = {@ApiResponse(responseCode = "200", description = "Get VisaPermissions by policyId")})
  @JsonView(Views.REST.class)
  public @ResponseBody List<VisaPermission> getPermissionsByPolicyId(
      @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = true)
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
  @ApiResponses(
      value = {@ApiResponse(responseCode = "200", description = "Create or Update VisaPermission")})
  @JsonView(Views.REST.class)
  public @ResponseBody VisaPermission createOrUpdatePermissions(
      @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = true)
          final String authorization,
      @RequestBody(required = true) VisaPermissionRequest visaPermissionRequest) {
    return visaPermissionService.createOrUpdatePermissions(visaPermissionRequest);
  }

  /*
   * This method is used to delete/remove visa permissions
   * @param visaPermissionRequest VisaPermissionRequest
   */
  @AdminScoped
  @RequestMapping(method = DELETE, value = "/permissions/{policyId}/{visaId}")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Remove VisaPermission")})
  @JsonView(Views.REST.class)
  public @ResponseBody void removePermissions(
      @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = true)
          final String authorization,
      @PathVariable(value = "policyId", required = true) UUID policyId,
      @PathVariable(value = "visaId", required = true) UUID visaId) {
    visaPermissionService.removePermission(policyId, visaId);
  }
}
