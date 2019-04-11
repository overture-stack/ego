package bio.overture.ego.config;

import static bio.overture.ego.model.enums.StatusType.PENDING;
import static bio.overture.ego.model.enums.StatusType.resolveStatusType;
import static bio.overture.ego.model.enums.UserType.USER;
import static bio.overture.ego.model.enums.UserType.resolveUserType;
import static org.springframework.util.StringUtils.isEmpty;

import bio.overture.ego.model.enums.StatusType;
import bio.overture.ego.model.enums.UserType;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UserDefaultsConfig {

  @Getter private final UserType defaultUserType;

  @Getter private final StatusType defaultUserStatus;

  public UserDefaultsConfig(
      @Value("${default.user.type}") String userType,
      @Value("${default.user.status}") String userStatus) {
    this.defaultUserType = isEmpty(userType) ? USER : resolveUserType(userType);
    this.defaultUserStatus = isEmpty(userStatus) ? PENDING : resolveStatusType(userStatus);
  }
}
