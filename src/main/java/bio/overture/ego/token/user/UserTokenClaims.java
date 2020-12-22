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

package bio.overture.ego.token.user;

import bio.overture.ego.model.entity.Application;
import bio.overture.ego.model.join.UserApplication;
import bio.overture.ego.token.TokenClaims;
import bio.overture.ego.view.Views;
import com.fasterxml.jackson.annotation.JsonView;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.springframework.util.StringUtils;

@Data
@NoArgsConstructor
@JsonView(Views.JWTAccessToken.class)
public class UserTokenClaims extends TokenClaims {

  @NonNull private UserTokenContext context;

  @Getter protected List<String> aud;

  public String getSub() {
    if (StringUtils.isEmpty(sub)) {
      return String.valueOf(this.context.getUserInfo().getId());
    } else {
      return sub;
    }
  }

  public List<String> getAud() {
    return this.context.getUserInfo().getUserApplications().stream()
        .map(UserApplication::getApplication)
        .map(Application::getName)
        .collect(Collectors.toList());
  }
}
