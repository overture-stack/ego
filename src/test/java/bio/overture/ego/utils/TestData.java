package bio.overture.ego.utils;

import static bio.overture.ego.utils.CollectionUtils.listOf;
import static bio.overture.ego.utils.CollectionUtils.mapToSet;

import bio.overture.ego.model.dto.Scope;
import bio.overture.ego.model.entity.Application;
import bio.overture.ego.model.entity.Policy;
import bio.overture.ego.model.entity.User;
import bio.overture.ego.model.enums.ApplicationType;
import bio.overture.ego.model.params.ScopeName;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import lombok.val;

public class TestData {
  public Application song;
  public String songId;
  public String songAuth;

  public String scoreId;
  public Application score;
  public String scoreAuth;

  private Map<String, Policy> policyMap;

  public User user1, user2, regularUser;

  public TestData(EntityGenerator entityGenerator) {
    songId = "song";
    val songSecret = "La la la!;";
    songAuth = authToken(songId, songSecret);

    song = entityGenerator.setupApplication(songId, songSecret, ApplicationType.CLIENT);

    scoreId = "score";
    val scoreSecret = "She shoots! She scores!";
    scoreAuth = authToken(scoreId, scoreSecret);

    score = entityGenerator.setupApplication(scoreId, scoreSecret, ApplicationType.CLIENT);
    val developers = entityGenerator.setupGroup("developers");

    val allPolicies = listOf("song", "id", "collab", "aws", "portal");

    policyMap = new HashMap<>();
    for (val p : allPolicies) {
      val policy = entityGenerator.setupPolicy(p, "admin");
      policyMap.put(p, policy);
    }

    user1 = entityGenerator.setupUser("User One");
    // user1.addNewGroup(developers);
    entityGenerator.addPermissions(
        user1, getScopes("id.WRITE", "song.WRITE", "collab.WRITE", "portal.READ"));

    user2 = entityGenerator.setupUser("User Two");
    entityGenerator.addPermissions(user2, getScopes("song.READ", "collab.READ", "id.WRITE"));

    regularUser = entityGenerator.setupUser("Regular User");
    regularUser.setUserType("USER");
    regularUser.setStatus("Approved");
    entityGenerator.addPermissions(regularUser, getScopes("song.READ", "collab.READ"));
  }

  public Set<Scope> getScopes(String... scopeNames) {
    return mapToSet(listOf(scopeNames), this::getScope);
  }

  public Scope getScope(String name) {
    val s = new ScopeName(name);
    return new Scope(policyMap.get(s.getName()), s.getAccessLevel());
  }

  private String authToken(String clientId, String clientSecret) {
    val s = clientId + ":" + clientSecret;
    return "Basic " + Base64.getEncoder().encodeToString(s.getBytes());
  }
}
