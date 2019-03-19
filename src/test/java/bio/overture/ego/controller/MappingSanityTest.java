package bio.overture.ego.controller;

import bio.overture.ego.AuthorizationServiceMain;
import bio.overture.ego.model.entity.Application;
import bio.overture.ego.model.entity.Group;
import bio.overture.ego.model.entity.GroupPermission;
import bio.overture.ego.model.entity.Policy;
import bio.overture.ego.model.enums.AccessLevel;
import bio.overture.ego.repository.ApplicationRepository;
import bio.overture.ego.repository.GroupPermissionRepository;
import bio.overture.ego.repository.GroupRepository;
import bio.overture.ego.repository.PolicyRepository;
import bio.overture.ego.service.ApplicationService;
import bio.overture.ego.service.GroupService;
import bio.overture.ego.utils.EntityGenerator;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

import static bio.overture.ego.model.enums.ApplicationType.ADMIN;
import static bio.overture.ego.model.enums.StatusType.APPROVED;
import static bio.overture.ego.model.enums.StatusType.PENDING;
import static bio.overture.ego.utils.EntityGenerator.generateNonExistentName;
import static bio.overture.ego.utils.EntityGenerator.randomStringNoSpaces;
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@TestExecutionListeners(listeners = DependencyInjectionTestExecutionListener.class)
@SpringBootTest(
    classes = AuthorizationServiceMain.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class MappingSanityTest {

  @Autowired private ApplicationService applicationService;
  @Autowired private GroupService groupService;
  @Autowired private GroupRepository groupRepository;
  @Autowired private PolicyRepository policyRepository;
  @Autowired private GroupPermissionRepository groupPermissionRepository;
  @Autowired private EntityGenerator entityGenerator;
  @Autowired private ApplicationRepository applicationRepository;

  @Test
  public void sanityCRUD_Group(){
    // Create group
    val group = groupRepository.save(
        Group.builder()
            .name(generateNonExistentName(groupService))
            .status(APPROVED)
            .build()
    );

    // Create application
    val app = applicationRepository.save(
        Application.builder()
            .name(generateNonExistentName(applicationService))
            .clientId(randomStringNoSpaces(10))
            .clientSecret(randomStringNoSpaces(20))
            .redirectUri("https://ego.org/"+randomStringNoSpaces(7))
            .status(PENDING)
            .type(ADMIN)
            .build());


    // Add application to group
    group.addApplication(app);
    app.getGroups().add(group);
    groupRepository.save(group);


    // Check existence
    assertThat(groupRepository.existsById(group.getId())).isTrue();
    assertThat(applicationRepository.existsById(app.getId())).isTrue();

    // Assert group has only that one application
    val fullGroupResult = groupRepository.getGroupByNameIgnoreCase(group.getName());
    assertThat(fullGroupResult).isPresent();
    val fullGroup = fullGroupResult.get();
    assertThat(fullGroup.getApplications()).hasSize(1);
    val a1 = fullGroup.getApplications().stream().findFirst().get();
    assertThat(a1.getId()).isEqualTo(app.getId());
    assertThat(a1.getGroups()).hasSize(1);

    // disassociate
    val fullGroupResult2 = groupRepository.getGroupByNameIgnoreCase(group.getName());
    val app2 = applicationRepository.findById(app.getId()).get();
    val fullGroup2 = fullGroupResult2.get();
    assertThat(fullGroup2.getApplications()).hasSize(1);
    val appToRemove = fullGroup2.getApplications().stream().findFirst().get();
    appToRemove.getGroups().remove(fullGroup2);
    fullGroup2.getApplications().remove(appToRemove);
    groupRepository.save(fullGroup2);

    // Assert group has only that 0 applications
    val fullGroupResult3 = groupRepository.getGroupByNameIgnoreCase(group.getName());
    assertThat(fullGroupResult3).isPresent();
    val fullGroup3 = fullGroupResult.get();
    assertThat(fullGroup3.getApplications()).isEmpty();

    // assert application still exists
    assertThat(applicationRepository.existsById(app.getId())).isTrue();

  }

  @Test
  public void sanityCRUD_GroupPermissions() {
    // Create group
    val group =
        Group.builder().name("myGroup").status(APPROVED).build();
    groupRepository.save(group);

    // Create policy
    val policy = Policy.builder().name("myPol").build();
    policyRepository.save(policy);

    // Create group permission
    val perm = new GroupPermission();
    perm.setOwner(group);
    perm.setPolicy(policy);
    perm.setAccessLevel(AccessLevel.READ);
    groupPermissionRepository.save(perm);

    // Check existence
    assertThat(groupRepository.existsById(group.getId())).isTrue();
    assertThat(policyRepository.existsById(policy.getId())).isTrue();
    assertThat(groupPermissionRepository.existsById(perm.getId())).isTrue();

    // Assert group has only that one permission
    val fullGroupResult = groupRepository.getGroupByNameIgnoreCase(group.getName());
    assertThat(fullGroupResult).isPresent();
    val fullGroup = fullGroupResult.get();
    assertThat(fullGroup.getPermissions()).hasSize(1);
    val p1 = fullGroup.getPermissions().stream().findFirst().get();
    assertThat(p1.getId()).isEqualTo(perm.getId());

    // Assert policy has only that one permission
    val fullPolicyResult = policyRepository.getPolicyByNameIgnoreCase(policy.getName());
    assertThat(fullPolicyResult).isPresent();
    val fullPolicy = fullPolicyResult.get();
    assertThat(fullPolicy.getGroupPermissions()).hasSize(1);
    val p2 = fullPolicy.getGroupPermissions().stream().findFirst().get();
    assertThat(p2.getId()).isEqualTo(perm.getId());

    // Assert group permission has the correct group and policy
    val permResult = groupPermissionRepository.findById(perm.getId());
    assertThat(permResult).isPresent();
    val perm1 = permResult.get();
    assertThat(perm1.getOwner().getId()).isEqualTo(group.getId());
    assertThat(perm1.getPolicy().getId()).isEqualTo(policy.getId());

    //    No need to disassociate policy and group from permission and vice versa, becuase delete is
    // all that is needed for OneToMany
    //    fullGroup.getPermissions().remove(perm1);
    //    fullPolicy.getGroupPermissions().remove(perm1);
    //    perm1.setOwner(null);
    //    perm1.setPolicy(null);

    // Delete group permission
    assertThat(groupPermissionRepository.existsById(perm.getId())).isTrue();
    groupPermissionRepository.deleteById(perm.getId());

    // Assert that deletion was successfull
    assertThat(groupPermissionRepository.existsById(perm.getId())).isFalse();

    // Assert group does not contain permission
    val fullGroupResult2 = groupRepository.getGroupByNameIgnoreCase(group.getName());
    assertThat(fullGroupResult2).isPresent();
    val fullGroup2 = fullGroupResult2.get();
    assertThat(fullGroup2.getPermissions()).doesNotContain(perm);

    // Assert policy does not contain permission
    val fullPolicyResult2 = policyRepository.getPolicyByNameIgnoreCase(policy.getName());
    assertThat(fullPolicyResult2).isPresent();
    val fullPolicy2 = fullPolicyResult2.get();
    assertThat(fullPolicy2.getGroupPermissions()).doesNotContain(perm);
  }
}
