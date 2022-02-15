package bio.overture.ego.security;

import java.util.Collection;
import java.util.Map;
import lombok.Builder;
import lombok.NonNull;
import org.apache.commons.lang.NotImplementedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;

@Builder
// This class is to unify OidcUsers and Oauth2 users since different providers
// have different attributes and non oidc users can have random fields but ego expects specific set
// of fields in its
// IDToken class
public class CustomOAuth2User implements OidcUser {
  private String givenName = "";
  private String familyName = "";
  @NonNull private String subjectId;
  private String email;
  private OAuth2User oauth2User;

  @Override
  public Map<String, Object> getAttributes() {
    return oauth2User.getAttributes();
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return oauth2User.getAuthorities();
  }

  @Override
  public String getName() {
    return oauth2User.getName();
  }

  public String getGivenName() {
    return this.givenName;
  }

  public String getFamilyName() {
    return this.familyName;
  }

  public String getSubjectId() {
    return oauth2User.getAttributes().containsKey(IdTokenClaimNames.SUB)
        ? oauth2User.getAttributes().get(IdTokenClaimNames.SUB).toString()
        : subjectId;
  }

  public String getEmail() {
    return oauth2User.getAttribute("email");
  }

  @Override
  public Map<String, Object> getClaims() {
    return oauth2User.getAttributes();
  }

  @Override
  public OidcUserInfo getUserInfo() {
    return new OidcUserInfo(oauth2User.getAttributes());
  }

  @Override
  public OidcIdToken getIdToken() {
    throw new NotImplementedException("not implemented");
  }
}
