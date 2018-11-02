package org.overture.ego.utils;

import lombok.val;
import org.overture.ego.model.dto.Scope;
import org.overture.ego.model.entity.*;
import org.overture.ego.model.params.ScopeName;

import java.util.*;

import static org.overture.ego.utils.CollectionUtils.listOf;
import static org.overture.ego.utils.CollectionUtils.mapToSet;

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
    developers = entityGenerator.setupGroup("developers");

    val allPolicies = listOf("song.upload", "song.download","id.create", "collab.upload", "collab.download");

    policyMap = new HashMap<>();
    for(val p:allPolicies) {
      val policy = entityGenerator.setupPolicy(p, "admin");
      policyMap.put(p, policy);
    }

    user1 = entityGenerator.setupUser("User One");
    // user1.addNewGroup(developers);
    entityGenerator.addPermissions(user1,
      getScopes("song.upload:WRITE", "song.download:WRITE",
      "collab.upload:WRITE", "collab.download:WRITE", "id.create:WRITE"));

    user2 = entityGenerator.setupUser("User Two");
    entityGenerator.addPermissions(user2, getScopes(
     "song.upload:READ", "song.download:WRITE"));
  }

  public Set<Scope> getScopes(String... scopeNames) {
    return mapToSet(listOf(scopeNames), this::getScope);
  }

  public Scope getScope(String name) {
    val s = new ScopeName(name);
    return new Scope(policyMap.get(s.getName()),s.getMask());
  }

  private String authToken(String clientId, String clientSecret) {
    val s = clientId + ":" + clientSecret;
    return "Basic " + Base64.getEncoder().encodeToString(s.getBytes());
  }
}
