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

package org.overture.ego.token;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.*;
import org.overture.ego.view.Views;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@JsonView(Views.JWTAccessToken.class)
public abstract class TokenClaims {
  @NonNull
  protected Integer iat;

  @NonNull
  protected Integer exp;

  @NonNull
  @JsonIgnore
  protected Integer validDuration;

  @Getter
  protected String sub;

  @NonNull
  protected String iss;

  @Getter
  protected List<String> aud;

  /*
    Defaults
   */
  private String jti = UUID.randomUUID().toString();

  @Getter(AccessLevel.NONE)
  @Setter(AccessLevel.NONE)
  @JsonIgnore
  private long initTime = System.currentTimeMillis();

  public int getExp(){
    return ((int) ((this.initTime + validDuration)/ 1000L));
  }

  public int getIat(){
    return (int) (this.initTime / 1000L);
  }

}
