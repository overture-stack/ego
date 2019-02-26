package bio.overture.ego.controller;

import bio.overture.ego.AuthorizationServiceMain;
import bio.overture.ego.model.entity.Group;
import bio.overture.ego.model.entity.GroupPermission;
import bio.overture.ego.model.entity.Policy;
import bio.overture.ego.model.enums.AccessLevel;
import bio.overture.ego.model.enums.ApplicationStatus;
import bio.overture.ego.repository.GroupPermissionRepository;
import bio.overture.ego.repository.GroupRepository;
import bio.overture.ego.repository.PolicyRepository;
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

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@TestExecutionListeners(listeners = DependencyInjectionTestExecutionListener.class)
@SpringBootTest(
    classes = AuthorizationServiceMain.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class MappingSanityTest {


  @Autowired private GroupRepository groupRepository;
  @Autowired private PolicyRepository policyRepository;
  @Autowired private GroupPermissionRepository groupPermissionRepository;


  @Test
  public void sanityCRUD_GroupPermissions() {
    //Create group
    val group = Group.builder()
        .name("myGroup")
        .status(ApplicationStatus.APPROVED.toString())
        .build();
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

//    No need to disassociate policy and group from permission and vice versa, becuase delete is all that is needed for OneToMany
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
