/*
 * Copyright (c) 2017. The Ontario Institute for Cancer Research. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package bio.overture.ego.token.app;

import bio.overture.ego.token.TokenClaims;
import bio.overture.ego.view.Views;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.util.StringUtils;

@Data
@NoArgsConstructor
@JsonView(Views.JWTAccessToken.class)
public class AppTokenClaims extends TokenClaims {

  /*
   Constants
  */
  public static final AuthorizationGrantType[] AUTHORIZED_GRANT_TYPES = {
    AuthorizationGrantType.AUTHORIZATION_CODE,
    AuthorizationGrantType.CLIENT_CREDENTIALS,
    AuthorizationGrantType.REFRESH_TOKEN
  };
  public static final String ROLE = "ROLE_CLIENT";

  @NonNull private AppTokenContext context;

  public String getSub() {
    if (StringUtils.isEmpty(sub)) {
      return String.valueOf(this.context.getAppInfo().getId());
    } else {
      return sub;
    }
  }
}
