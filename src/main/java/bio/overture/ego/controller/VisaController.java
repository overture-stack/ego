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

  private final UserPermissionService userPermissionService;
  private final GroupPermissionService groupPermissionService;
  private final ApplicationPermissionService applicationPermissionService;

  @Autowired
  public VisaController(
      @NonNull VisaService visaService,
      @NonNull UserPermissionService userPermissionService,
      @NonNull GroupPermissionService groupPermissionService,
      @NonNull ApplicationPermissionService applicationPermissionService) {
    this.visaService = visaService;
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
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Get Visa by id")})
  @JsonView(Views.REST.class)
  public @ResponseBody Visa getVisa(
      @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = true)
          final String authorization,
      @PathVariable(value = "id", required = true) UUID id) {
    return visaService.getById(id);
  }

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

  @AdminScoped
  @RequestMapping(method = DELETE, value = "/{id}")
  @ResponseStatus(value = HttpStatus.OK)
  public void deleteVisa(
      @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = true)
          final String authorization,
      @PathVariable(value = "id", required = true) UUID id) {
    visaService.delete(id);
  }
}
