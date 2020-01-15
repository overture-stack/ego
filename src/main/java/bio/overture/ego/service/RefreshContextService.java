package bio.overture.ego.service;

import static bio.overture.ego.model.enums.JavaFields.REFRESH_ID;
import static bio.overture.ego.model.enums.StatusType.APPROVED;
import static bio.overture.ego.model.exceptions.NotFoundException.checkNotFound;
import static bio.overture.ego.model.exceptions.UniqueViolationException.checkUnique;

import bio.overture.ego.model.domain.RefreshContext;
import bio.overture.ego.model.entity.RefreshToken;
import bio.overture.ego.model.entity.User;
import bio.overture.ego.model.exceptions.ForbiddenException;
import bio.overture.ego.repository.RefreshTokenRepository;
import bio.overture.ego.repository.queryspecification.builder.RefreshTokenSpecificationBuilder;
import java.sql.Date;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import javax.servlet.http.Cookie;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class RefreshContextService extends AbstractBaseService<RefreshToken, UUID> {

  private RefreshTokenRepository refreshTokenRepository;
  private TokenService tokenService;
  /** Configuration */
  private int durationMs;

  private boolean cookieIsSecure;

  @Autowired
  public RefreshContextService(
      @NonNull RefreshTokenRepository refreshTokenRepository,
      @NonNull TokenService tokenService,
      @Value("${refreshToken.durationMs:43200000}") int durationMs,
      @Value("${refreshToken.cookieIsSecure}") boolean cookieIsSecure) {
    super(RefreshToken.class, refreshTokenRepository);
    this.refreshTokenRepository = refreshTokenRepository;
    this.tokenService = tokenService;
    this.durationMs = durationMs;
    this.cookieIsSecure = cookieIsSecure;
  }

  @SuppressWarnings("unchecked")
  public RefreshToken get(@NonNull UUID id, boolean fetchUser) {
    val result =
        (Optional<RefreshToken>)
            getRepository()
                .findOne(new RefreshTokenSpecificationBuilder().fetchUser(fetchUser).buildById(id));
    checkNotFound(result.isPresent(), "RefreshToken '%s' does not exist", id);
    return result.get();
  }

  @Override
  public RefreshToken getWithRelationships(@NonNull UUID userId) {
    return get(userId, true);
  }

  private RefreshToken createTransientToken(UUID jti) {
    val now = Instant.now();
    val expiry = now.plus(durationMs, ChronoUnit.MILLIS);
    return RefreshToken.builder()
        .jti(jti)
        .issueDate(Date.from(now))
        .expiryDate(Date.from(expiry))
        .build();
  }

  private Optional<RefreshToken> getRefreshTokenByUser(User user) {
    return refreshTokenRepository.getByUser(user);
  }

  private void checkUniqueByUser(User user) {
    checkUnique(
        getRefreshTokenByUser(user).isEmpty(),
        String.format("A refresh token already exists for %s", user.getId()));
  }

  public void disassociateUserAndDelete(User user) {
    val refreshTokenOpt = this.getRefreshTokenByUser(user);
    if (refreshTokenOpt.isPresent()) {
      log.debug("Refresh token exists, deleting...");
      user.setRefreshToken(null);
      val existingRefreshToken = refreshTokenOpt.get();
      refreshTokenRepository.delete(existingRefreshToken);
    }
  }

  public RefreshToken createRefreshToken(@NonNull String bearerToken) {
    val user = tokenService.getTokenUserInfo(bearerToken);
    this.checkUniqueByUser(user);

    val jti = tokenService.getTokenClaims(bearerToken).getId();
    val refreshToken = createTransientToken(UUID.fromString(jti));
    refreshToken.associateWithUser(user);
    return refreshTokenRepository.save(refreshToken);
  }

  public RefreshContext createRefreshContext(String refreshTokenId, String bearerToken) {
    val refreshTokenOpt = refreshTokenRepository.findById(UUID.fromString(refreshTokenId));
    val tokenClaims = tokenService.getTokenClaimsIgnoreExpiry(bearerToken);

    checkNotFound(refreshTokenOpt.isPresent(), "RefreshToken %s is not found.", refreshTokenId);

    val refreshToken = refreshTokenOpt.get();
    val user = refreshToken.getUser();

    return RefreshContext.builder()
        .refreshToken(refreshToken)
        .tokenClaims(tokenClaims)
        .user(user)
        .build();
  }

  public RefreshContext createInitialRefreshContext(@NonNull String bearerToken) {
    val user = tokenService.getTokenUserInfo(bearerToken);
    disassociateUserAndDelete(user);

    if (tokenService.getTokenUserInfo(bearerToken).getStatus() != APPROVED) {
      throw new ForbiddenException("User does not have approved status, rejecting.");
    }

    val newRefreshToken = createRefreshToken(bearerToken);
    return createRefreshContext(newRefreshToken.getId().toString(), bearerToken);
  }

  public String validateAndReturnNewUserToken(String refreshId, String bearerToken) {
    val incomingRefreshContext = createRefreshContext(refreshId, bearerToken);
    disassociateUserAndDelete(incomingRefreshContext.getUser());

    incomingRefreshContext.validate();

    val newUserToken = tokenService.generateUserToken(incomingRefreshContext.getUser());
    createRefreshToken(newUserToken);
    return newUserToken;
  }

  private Cookie createCookie(String cookieName, String cookieValue, Integer maxAge) {
    Cookie cookie = new Cookie(cookieName, cookieValue);
    // where to access the accepted domain?
    cookie.setDomain("localhost");
    // disable setSecure while testing locally in browser, or will not show in cookies
    cookie.setSecure(cookieIsSecure);
    cookie.setHttpOnly(true);
    cookie.setMaxAge(maxAge);
    cookie.setPath("/");

    return cookie;
  }

  public Cookie createRefreshCookie(RefreshToken refreshToken) {
    return createCookie(
        REFRESH_ID,
        refreshToken.getId().toString(),
        refreshToken.getSecondsUntilExpiry().intValue());
  }

  public Cookie deleteRefreshTokenAndCookie(String refreshId) {
    val incomingRefreshToken = getById(UUID.fromString(refreshId));
    disassociateUserAndDelete(incomingRefreshToken.getUser());
    return createCookie(REFRESH_ID, "", 0);
  }
}
