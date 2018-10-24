package org.overture.ego.utils;

import lombok.val;
import org.overture.ego.model.entity.Application;
import org.overture.ego.model.entity.Group;
import org.overture.ego.model.entity.Policy;
import org.overture.ego.model.entity.User;
import org.overture.ego.model.enums.PolicyMask;

import java.util.*;

public class TestData {
  public Application song;
  public String songId;
  public String songAuth;

  public String scoreId;
  public Application score;
  public String scoreAuth;

  private Map<String, Policy> policyMap;
  private Group developers;

  public User user1, user2;

  public TestData(EntityGenerator entityGenerator) {
    songId="song";
    val songSecret="La la la!;";
    songAuth = authToken(songId, songSecret);

    song = entityGenerator.setupApplication(songId, songSecret);

    scoreId="score";
    val scoreSecret="She shoots! She scores!";
    scoreAuth = authToken(scoreId, scoreSecret);

    score = entityGenerator.setupApplication(scoreId, scoreSecret);

    val admin = entityGenerator.setupGroup("admin");
    developers = entityGenerator.setupGroup("developers");

    val allPolicies = list("song.upload", "song.download","id.create", "collab.upload", "collab.download");

    policyMap = new HashMap<>();
    for(val p:allPolicies) {
      val policy = entityGenerator.setupPolicy(p, admin.getId());
      policyMap.put(p, policy);
    }

    user1 = entityGenerator.setupUser("User One");
    user1.addNewGroup(developers);
    entityGenerator.addPermission(user1, PolicyMask.READ,
      policies("song.upload","song.download", "collab.upload", "collab.download", "id.create"));

    user2 = entityGenerator.setupUser("User Two");
    entityGenerator.addPermission(user2, PolicyMask.READ,
      policies("song.upload", "song.download"));
  }

  public HashSet<Policy> policies(String... policyNames) {
    val result = new HashSet<Policy>();
    for(val name: policyNames) {
      result.add(policyMap.get(name));
    }
    return result;
  }


  private String authToken(String clientId, String clientSecret) {
    val s = clientId + ":" + clientSecret;
    return "Basic " + Base64.getEncoder().encodeToString(s.getBytes());
  }

  private List<String> list(String... s) {
    return Arrays.asList(s);
  }

}