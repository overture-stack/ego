package bio.overture.ego.service;

//import bio.overture.ego.model.dto.CreateRefreshTokenRequest;
import bio.overture.ego.model.entity.RefreshToken;
import bio.overture.ego.repository.RefreshTokenRepository;
import bio.overture.ego.repository.UserRepository;
import bio.overture.ego.repository.queryspecification.builder.RefreshTokenSpecificationBuilder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

import static bio.overture.ego.model.exceptions.NotFoundException.checkNotFound;
import static bio.overture.ego.model.exceptions.UniqueViolationException.checkUnique;

@Slf4j
@Service
public class RefreshTokenService extends AbstractBaseService<RefreshToken, UUID> {
  /*
   * Dependencies
   */
  private RefreshTokenRepository refreshTokenRepository;

  /** Configuration */
  @Value("${refreshToken.duration:10800000}")
  private int DURATION;

  @Autowired
  public RefreshTokenService(
    @NonNull RefreshTokenRepository refreshTokenRepository,
    @NonNull UserRepository userRepository,
    @NonNull UserService userService) {
    super(RefreshToken.class, refreshTokenRepository);
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


  // create
  // custom gets? getByJti, getByRefreshId
  // getWithRelationships - implement as just refreshToken = true
  // update by userId - replace refresh token id + new jti from access token, renew issue/expiry
  // delete - this would delete the whole row, but leave the user intact
  // the other delete is from the parent user getting deleted, by orphanRemoval
  // entity currently implements getId as get refreshId, do we want another for getUserId?


}