package bio.overture.ego.model.domain;

import static bio.overture.ego.model.enums.StatusType.APPROVED;

import bio.overture.ego.model.entity.RefreshToken;
import bio.overture.ego.model.entity.User;
import bio.overture.ego.model.exceptions.ForbiddenException;
import bio.overture.ego.repository.RefreshTokenRepository;
import io.jsonwebtoken.Claims;
import lombok.*;
import org.springframework.security.oauth2.common.exceptions.InvalidRequestException;

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

  private boolean isNotExpired() {
    return refreshToken.getSecondsUntilExpiry() > 0;
  }

  public boolean validate() {
    if (!hasApprovedUser()) {
      throw new ForbiddenException("User does not have approved status, rejecting.");
    } else if (
      this.userMatches() &
        this.jtiMatches() &
        this.isNotExpired()
    ) {
      return true;
    } else {
      throw new InvalidRequestException("Unknown type of authentication."); // customize?
    }
  }

}

//    RefreshTokenService method takes in bearer and refreshId, gets validations from context
// and returns
//    if (nonValidUser) { 403 response }
//    if (nonValidContext) { 401 response } -> login
//    if (validContext) { 200 response }
// default response?
// validations:
// jwt user + jti + refresh token id (tokenClaims + cookie) matches db row
// refresh token is not expired
// client id passed as query parameter to help verify cookie
// check for scope changes
// when to renew the access token? on 400 errors - i.e. when the access token expires but the
// refresh token is still valid
// when the refresh token is used for a new accessToken, regenerate a new token uuid, delete old row and replace
// in the db

// login request comes in
// goes to oauth, gets accessToken jwt from provider
// comes back to ego. take the accessToken and attach a refresh token to that user.
// add all those ids into the REFRESHTOKEN table
// pass this refresh token id back in the response (in session cookie), with the jwt

// find by refresh id
// user id matches
// claims -> jti matches
// refresh expiry is valid
// client id matches. client id passed in request
// user status is approved
// user scope has not changed