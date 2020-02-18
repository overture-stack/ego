package bio.overture.ego.controller;

import bio.overture.ego.AuthorizationServiceMain;
import bio.overture.ego.model.dto.PermissionRequest;
import bio.overture.ego.model.entity.Policy;
import bio.overture.ego.model.entity.Group;
import bio.overture.ego.model.entity.GroupPermission;
import bio.overture.ego.model.enums.AccessLevel;
import bio.overture.ego.utils.EntityGenerator;
import lombok.extern.slf4j.Slf4j;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static java.lang.String.format;

@Slf4j
@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@TestExecutionListeners(listeners = DependencyInjectionTestExecutionListener.class)
@SpringBootTest(
    classes = AuthorizationServiceMain.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class GroupPermissionControllerTest2 extends AbstractPermissionControllerTest2<Group, GroupPermission> {

  @Autowired
  private EntityGenerator entityGenerator;

  @Value("${logging.test.controller.enable}")
  private boolean enableLogging;

  @Override
  protected List<Group> setupOwners(String... names) {
    return entityGenerator.setupGroups(names);
  }

  @Override
  protected void createPermissionsForOwners(UUID policyId, AccessLevel mask, Collection<Group> owners) {
    owners.forEach(u ->
        initStringRequest()
            .endpoint(getAddPermissionsEndpoint(u.getId()))
            .body(List.of(PermissionRequest.builder()
                .mask(mask)
                .policyId(policyId)
                .build()))
            .postAnd()
            .assertOk());
  }

  @Override
  protected Class<Group> getOwnerType() {
    return Group.class;
  }

  @Override
  protected Class<GroupPermission> getPermissionType() {
    return GroupPermission.class;
  }

  @Override
  protected String getAddPermissionsEndpoint(UUID ownerId) {
    return format("groups/%s/permissions", ownerId);
  }

  @Override
  protected String getOwnersForPolicyEndpoint(UUID policyId) {
    return format("policies/%s/groups", policyId);
  }

  @Override
  protected boolean enableLogging() {
    return enableLogging;
  }

  @Override
  protected Policy createPolicy(String name) {
    return entityGenerator.setupSinglePolicy(name);
  }
}
