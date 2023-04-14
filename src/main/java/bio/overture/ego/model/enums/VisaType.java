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

package bio.overture.ego.model.enums;

import static bio.overture.ego.utils.Joiners.COMMA;
import static bio.overture.ego.utils.Streams.stream;
import static java.lang.String.format;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum VisaType {
  STANDARD_VISA_TYPE,
  CUSTOM_VISA_TYPE;

  public static VisaType resolveStatusType(@NonNull String statusType) {
    return stream(values())
        .filter(x -> x.toString().equals(statusType))
        .findFirst()
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    format(
                        "The status type '%s' cannot be resolved. Must be one of: [%s]",
                        statusType, COMMA.join(values()))));
  }

  @Override
  public String toString() {
    return this.name();
  }
}
