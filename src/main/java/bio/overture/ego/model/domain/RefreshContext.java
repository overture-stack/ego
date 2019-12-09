package bio.overture.ego.model.domain;

import static bio.overture.ego.model.enums.StatusType.APPROVED;

import bio.overture.ego.model.entity.RefreshToken;
import bio.overture.ego.model.entity.User;
import bio.overture.ego.model.exceptions.ForbiddenException;
import io.jsonwebtoken.Claims;
import lombok.*;
import org.springframework.security.oauth2.common.exceptions.UnauthorizedClientException;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
public class RefreshContext {

  // context one side from request -> refresh token found in db with id in cookie. user id from associated user in db row. claims in request jwt
  // compare with what we have stored for that entry in the db (if one is found)
  // so the refresh context is "self-validating" -> if what is contained in the RefreshContext instance lines up, it can be said to be valid
  // so is it, refreshToken + User == Claims?
  @NonNull private RefreshToken refreshToken; // found in db by id in cookie
  @NonNull private User user; // comes from refreshToken -> User assoc
  @NonNull private Claims tokenClaims; // from jwt

//  public RefreshContext(String refreshId, String bearerToken) {
//    this.refreshToken = refreshTokenRepository.findById(UUID.fromString(refreshId));
//  }
  public boolean hasApprovedUser() {
    return user.getStatus() == APPROVED;
  }

  private boolean userMatches() {
    return user.getId().equals(UUID.fromString(tokenClaims.getSubject()));
  }

  private boolean jtiMatches() {
    return refreshToken.getJti().equals(UUID.fromString(tokenClaims.getId()));
  }

  private boolean isExpired() {
    return refreshToken.getSecondsUntilExpiry() <= 0;
  }

  public boolean validate() {
    if (!hasApprovedUser()) {
      throw new ForbiddenException("User does not have approved status, rejecting.");
    }
    if (this.isExpired()) {
      throw new UnauthorizedClientException(String.format("RefreshToken %s is expired", refreshToken.getId()));
    }
    if (
      this.userMatches() &
        this.jtiMatches()
    ) {
      return true;
    } else {
      throw new ForbiddenException(String.format("Invalid token claims for refreshId %s.", refreshToken.getId()));
    }
  }

}