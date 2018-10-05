package org.overture.ego.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Set;

@Getter
@AllArgsConstructor
public class TokenRequest {
  private String grantType;
  private String clientId;
  private String clientSecret;
  private String userName;
  private Set<String> scopes;
}
