package bio.overture.ego.service;

//import bio.overture.ego.model.dto.CreateRefreshTokenRequest;
import bio.overture.ego.model.entity.RefreshToken;
import bio.overture.ego.model.entity.User;
import bio.overture.ego.repository.RefreshTokenRepository;
import bio.overture.ego.repository.UserRepository;
import bio.overture.ego.repository.queryspecification.builder.RefreshTokenSpecificationBuilder;
import bio.overture.ego.token.user.UserJWTAccessToken;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static bio.overture.ego.model.exceptions.NotFoundException.checkNotFound;
import static bio.overture.ego.model.exceptions.UniqueViolationException.checkUnique;

@Slf4j
@Service
public class RefreshTokenService extends AbstractBaseService<RefreshToken, UUID> {

  private RefreshTokenRepository refreshTokenRepository;

  /** Configuration */
  private int durationInSeconds;

  @Autowired
  public RefreshTokenService(
    @NonNull RefreshTokenRepository refreshTokenRepository,
    @NonNull UserRepository userRepository,
    @NonNull UserService userService,
    @Value("${refreshToken.durationInSeconds}") int durationInSeconds
    ) {
    super(RefreshToken.class, refreshTokenRepository);
    this.durationInSeconds = durationInSeconds;
    this.refreshTokenRepository = refreshTokenRepository;
  }

  @SuppressWarnings("unchecked")
  public RefreshToken get(
    @NonNull UUID userId,
    boolean fetchUser
  ) {
    val result =
      (Optional<RefreshToken>)
        getRepository()
          .findOne(
            new RefreshTokenSpecificationBuilder()
              .fetchUser(fetchUser)
              .buildById(userId));
    checkNotFound(result.isPresent(), "The refreshToken for userId '%s' does not exist", userId);
    return result.get();
  }

  @Override
  public RefreshToken getWithRelationships(@NonNull UUID userId) {
    return get(userId,true);
  }

  // refactor these next 3 methods?
  // create refreshToken "blueprint"
  private RefreshToken createTransientToken(User user) {
    // need user here to get jti?
//    val user = accessToken.getTokenClaims();
    val now = Instant.now();
    val expiry = now.plus(durationInSeconds, ChronoUnit.SECONDS);
    return RefreshToken.builder()
      .jti(UUID.randomUUID())
      .issueDate(Date.from(now))
      .expiryDate(Date.from(expiry))
      .build();
  }

  public RefreshToken createRefreshToken(@NonNull User user) {
    val refreshToken = createTransientToken(user);
    associateUserAndRefreshToken(user, refreshToken);
    return refreshTokenRepository.save(refreshToken);
  }

  // create association between user and token
  private static void associateUserAndRefreshToken(User user, RefreshToken refreshToken) {
    refreshToken.setUser(user);
    user.setRefreshToken(refreshToken);
  }







//  public RefreshToken create(@NonNull CreateRefreshTokenRequest request) {
//    this.checkUserIdUnique(request.getUserId()); // where should validation be?
//    return null;
//  }

//  private void checkUserIdUnique(UUID userId) {
//    checkUnique(
//      !refreshTokenRepository.existsById(userId), "A refresh token already exists for this user id");
//  }


//  @Override
//  public String getEntityTypeName() {
//    return null;
//  }

//  public User create(@NonNull CreateUserRequest request) {
//    validateCreateRequest(request);
//    val user = USER_CONVERTER.convertToUser(request);
//    return getRepository().save(user);
//  }

}