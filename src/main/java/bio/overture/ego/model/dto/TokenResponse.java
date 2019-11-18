/*
 * Copyright (c) 2019. The Ontario Institute for Cancer Research. All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package bio.overture.ego.model.dto;

import bio.overture.ego.view.Views;
import com.fasterxml.jackson.annotation.JsonView;
import java.util.Set;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Deprecated
@Value
@Builder
@JsonView(Views.REST.class)
public class TokenResponse {
  @NonNull private final String accessToken;
  @NonNull private final Set<String> scope;
  @NonNull private final Long exp;
  private String description;
}
