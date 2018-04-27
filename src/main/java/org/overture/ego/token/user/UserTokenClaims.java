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

package org.overture.ego.token.user;


import com.fasterxml.jackson.annotation.JsonView;
import lombok.*;
import org.overture.ego.token.TokenClaims;
import org.overture.ego.view.Views;
import org.springframework.util.StringUtils;

import java.util.List;


@Data
@NoArgsConstructor
@JsonView(Views.JWTAccessToken.class)
public class UserTokenClaims extends TokenClaims {

  @NonNull
  private UserTokenContext context;

  public String getSub(){
    if(StringUtils.isEmpty(sub)) {
      return String.valueOf(this.context.getUserInfo().getId());
    } else {
      return sub;
    }
  }

  public List<String> getAud(){
    return this.context.getUserInfo().getApplications();
  }

}
