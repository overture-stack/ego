package org.overture.ego.token.app;

import io.jsonwebtoken.Claims;
import lombok.val;
import org.overture.ego.token.TokenService;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2RefreshToken;

import java.util.*;

public class AppJWTAccessToken implements OAuth2AccessToken {

  private Claims tokenClaims = null;
  private String token = null;

  public AppJWTAccessToken(String token, TokenService tokenService) {
    this.token = token;
    this.tokenClaims = tokenService.getTokenClaims(token);
  }

  @Override
  public Map<String, Object> getAdditionalInformation() {
    val output = new HashMap<String, Object>();
    output.put("groups", getApp().get("groups"));
    return output;
  }

  @Override
  public Set<String> getScope() {
    return new HashSet<String>((Arrays.asList(AppTokenClaims.SCOPES)));
  }

  @Override
  public OAuth2RefreshToken getRefreshToken() {
    return null;
  }

  @Override
  public String getTokenType() {
    return "Bearer";
  }

  @Override
  public boolean isExpired() {
    return getExpiresIn() <= 0;
  }

  @Override
  public Date getExpiration() {
    return tokenClaims.getExpiration();
  }

  @Override
  public int getExpiresIn() {
    return (int) ((System.currentTimeMillis() - tokenClaims.getExpiration().getTime()) / 1000L);
  }

  @Override
  public String getValue() {
    return token;
  }

  private Map getApp() {
    return (Map) ((Map) tokenClaims.get("context")).get("application");
  }
}
