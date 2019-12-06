package bio.overture.ego.service;

// import bio.overture.ego.model.dto.CreateRefreshTokenRequest;
import static bio.overture.ego.model.exceptions.NotFoundException.checkNotFound;

import bio.overture.ego.model.domain.RefreshContext;
import bio.overture.ego.model.entity.RefreshToken;
import bio.overture.ego.model.entity.User;
import bio.overture.ego.model.enums.StatusType;
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
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.stereotype.Service;
import static bio.overture.ego.model.exceptions.UniqueViolationException.checkUnique;
import static java.lang.String.format;

@Slf4j
@Service
public class RefreshTokenService extends AbstractBaseService<RefreshToken, UUID> {

  private RefreshTokenRepository refreshTokenRepository;
  private UserService userService;
  private TokenService tokenService;
  /** Configuration */
  private int durationInSeconds;

  @Autowired
  public RefreshTokenService(
      @NonNull RefreshTokenRepository refreshTokenRepository,
      @NonNull UserService userService,
      @NonNull TokenService tokenService,
      @Value("${refreshToken.durationInSeconds}") int durationInSeconds) {
    super(RefreshToken.class, refreshTokenRepository);
    this.refreshTokenRepository = refreshTokenRepository;
    this.userService = userService;
    this.tokenService = tokenService;
    this.durationInSeconds = durationInSeconds;
  }

  @SuppressWarnings("unchecked")
  public RefreshToken get(@NonNull UUID userId, boolean fetchUser) {
    val result =
        (Optional<RefreshToken>)
            getRepository()
                .findOne(
                    new RefreshTokenSpecificationBuilder().fetchUser(fetchUser).buildById(userId));
    checkNotFound(result.isPresent(), "The refreshToken for userId '%s' does not exist", userId);
    return result.get();
  }

  @Override
  public RefreshToken getWithRelationships(@NonNull UUID userId) {
    return get(userId, true);
  }

  private RefreshToken createTransientToken(UUID jti) {
    val now = Instant.now();
    val expiry = now.plus(durationInSeconds, ChronoUnit.SECONDS);
    return RefreshToken.builder()
        .jti(jti)
        .issueDate(Date.from(now))
        .expiryDate(Date.from(expiry))
        .build();
  }

  // you are creating a fresh token with the bearer token (jwt) that is coming from ego-token
  // on post-login requests, you will still be using the bearer token from the request headers to create the assoc
  // check userId unique before create. do i need to check refresh id is unique too?
  // check unique outside create
  public RefreshToken createRefreshToken(@NonNull String bearerToken) {
    val user = tokenService.getTokenUserInfo(bearerToken);
    val jti = tokenService.getTokenClaims(bearerToken).getId();
    val refreshToken = createTransientToken(UUID.fromString(jti));
    refreshToken.associateWithUser(user);
    associateUserAndRefreshToken(user, refreshToken);
    return refreshTokenRepository.save(refreshToken);
  }

  // create association between user and token
  private static void associateUserAndRefreshToken(User user, RefreshToken refreshToken) {
    refreshToken.setUser(user);
    user.setRefreshToken(refreshToken);
  }

  private void checkUniqueByUserId(UUID userId) {
    checkUnique(
      !refreshTokenRepository.existsById(userId), "A refresh token already exists for this user id");
  }

  public RefreshContext createRefreshContext(String refreshTokenId, String bearerToken) {
    val refreshTokenOpt = refreshTokenRepository.findById(UUID.fromString(refreshTokenId));
    val tokenClaims = tokenService.getTokenClaims(bearerToken);
    val user = tokenService.getTokenUserInfo(bearerToken);
    if (refreshTokenOpt == null || refreshTokenOpt.isEmpty()) {
      throw new InvalidTokenException(format("RefreshToken \"%s\" is not found. ", refreshTokenId));
    }
    val refreshToken = refreshTokenOpt.get();
    // do you want to just return a context instance here, or return validate method which will throw an exception if created context is invalid for any reason?
    return RefreshContext.builder().refreshToken(refreshToken).tokenClaims(tokenClaims).user(user).build();
  }

//  public RefreshContext validateContext(String refreshTokenId, String bearerToken) {
//
//  }
}

// first call needed when you get here from refresh endpoint is:
// validate the request, otherwise you shouldn't proceed: validate jwt, validate refreshId exists
// should validate refresh id exists ALSO happen in context? seems like it should belong there???
// validate user Status
// validate with RefreshContext
// after validation, delete refresh token
// if validation passes, proceed to create. if it fails, return 401
// before create, check unique byUserId
// create and associate
// maybe have 2 main validations from context, for valid User and valid context? to differentiate
// response
//  val refreshToken = refreshTokenService.getById(UUID.fromString(refreshId));
//  val requestClaims = tokenService.getTokenClaims(bearer);
//  val userFromClaims = tokenService.getTokenUserInfo(requestClaims.getSubject());
//
//  val userStatus = userFromClaims.getStatus();
//  val refreshContext = new RefreshContext(refreshToken, userFromClaims, requestClaims);

// service will need:
// create
// associate
// check user is unique. check refresh id is unique?
// validate from RefreshContext