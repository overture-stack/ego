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

package bio.overture.ego.model.enums;

import java.util.Arrays;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;

@RequiredArgsConstructor
public enum AccessLevel {
  READ("READ"),
  WRITE("WRITE"),
  DENY("DENY");

  @NonNull private final String value;

  public static AccessLevel fromValue(String value) {
    for (val policyMask : values()) {
      if (policyMask.value.equalsIgnoreCase(value)) {
        return policyMask;
      }
    }
    throw new IllegalArgumentException(
        "Unknown enum type " + value + ", Allowed values are " + Arrays.toString(values()));
  }

  /**
   * Determine if we are allowed access to what we want, based upon what we have.
   *
   * @param have The PolicyMask we have.
   * @param want The PolicyMask we want.
   * @return true if we have access, false if not.
   */
  public static boolean allows(AccessLevel have, AccessLevel want) {
    // 1) If we're to be denied everything, or the permission is deny everyone, we're denied.
    if (have.equals(DENY) || want.equals(DENY)) {
      return false;
    }
    // 2) Otherwise, if we have exactly what we need, we're allowed access.
    if (have.equals(want)) {
      return true;
    }
    // 3) We're allowed access to READ if we have WRITE
    if (have.equals(WRITE) && want.equals(READ)) {
      return true;
    }
    // 4) Otherwise, we're denied access.
    return false;
  }

  @Override
  public String toString() {
    return value;
  }
}
