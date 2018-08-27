/*
 * Copyright (c) 2018. The Ontario Institute for Cancer Research. All rights reserved.
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

package org.overture.ego.model.enums;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;

import java.util.Arrays;

@RequiredArgsConstructor
public enum PolicyMask {
  READ("READ"),
  WRITE("WRITE"),
  DENY("DENY");

  @NonNull
  private final String value;

  @Override
  public String toString() {
    return value;
  }

  public static PolicyMask fromValue(String value) {
    for (val aclMask : values()) {
      if (aclMask.value.equalsIgnoreCase(value)) {
        return aclMask;
      }
    }
    throw new IllegalArgumentException(
        "Unknown enum type " + value + ", Allowed values are " + Arrays.toString(values()));
  }
}
