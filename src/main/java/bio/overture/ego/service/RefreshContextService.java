package bio.overture.ego.service;

import static bio.overture.ego.model.exceptions.NotFoundException.checkNotFound;
import static bio.overture.ego.model.exceptions.UniqueViolationException.checkUnique;

import bio.overture.ego.model.domain.RefreshContext;
import bio.overture.ego.model.entity.RefreshToken;
import bio.overture.ego.model.entity.User;
import bio.overture.ego.model.exceptions.NotFoundException;
import bio.overture.ego.repository.RefreshTokenRepository;
import bio.overture.ego.repository.queryspecification.builder.RefreshTokenSpecificationBuilder;
import java.sql.Date;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
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
  private int duration;

  @Autowired
  public RefreshContextService(
      @NonNull RefreshTokenRepository refreshTokenRepository,
      @NonNull TokenService tokenService,
      @Value("${refreshToken.duration:43200000}") int duration) {
    super(RefreshToken.class, refreshTokenRepository);
    this.refreshTokenRepository = refreshTokenRepository;
    this.tokenService = tokenService;
    this.duration = duration;
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
    val expiry = now.plus(duration, ChronoUnit.MILLIS);
    return RefreshToken.builder()
        .jti(jti)
        .issueDate(Date.from(now))
        .expiryDate(Date.from(expiry))
        .build();
  }

  public Optional<RefreshToken> getRefreshTokenByUser(User user) {
    return refreshTokenRepository.getByUser(user);
  }

  private void checkUniqueByUser(User user) {
    checkUnique(
        !getRefreshTokenByUser(user).isPresent(),
        String.format("A refresh token already exists for %s", user.getId()));
  }

  public void disassociateUserAndDelete(String userToken) {
    val user = tokenService.getTokenUserInfo(userToken);

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
    val tokenClaims = tokenService.getTokenClaims(bearerToken);
    val user = tokenService.getTokenUserInfo(bearerToken);
    if (refreshTokenOpt == null || refreshTokenOpt.isEmpty()) {
      throw new NotFoundException(String.format("RefreshToken %s is not found.", refreshTokenId));
    }
    val refreshToken = refreshTokenOpt.get();
    return RefreshContext.builder()
        .refreshToken(refreshToken)
        .tokenClaims(tokenClaims)
        .user(user)
        .build();
  }
}