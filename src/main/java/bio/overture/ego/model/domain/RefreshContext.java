package bio.overture.ego.model.domain;

import static bio.overture.ego.model.enums.StatusType.APPROVED;

import bio.overture.ego.model.entity.RefreshToken;
import bio.overture.ego.model.entity.User;
import bio.overture.ego.model.exceptions.ForbiddenException;
import bio.overture.ego.model.exceptions.UnauthorizedException;
import bio.overture.ego.service.RefreshContextService;
import io.jsonwebtoken.Claims;
import java.util.UUID;
import lombok.*;

@Data
@Builder
@AllArgsConstructor
public class RefreshContext {

  @NonNull private RefreshToken refreshToken; // from db
  @NonNull private User user; // from refreshToken -> User assoc
  @NonNull private Claims tokenClaims; // from jwt

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
      throw new UnauthorizedException(
          String.format("RefreshToken %s is expired", refreshToken.getId()));
    }
    if (this.userMatches() & this.jtiMatches()) {
      return true;
    } else {
      throw new ForbiddenException(
          String.format("Invalid token claims for refreshId %s.", refreshToken.getId()));
    }
  }
}
