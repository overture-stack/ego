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

package bio.overture.ego.utils;

import static java.util.Objects.isNull;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FieldUtils {

  public static List<Field> getStaticFieldList(Class c) {
    return Arrays.stream(c.getDeclaredFields()).map(f -> f).collect(Collectors.toList());
  }

  public static List<String> getStaticFieldValueList(Class c) {
    return Arrays.stream(c.getDeclaredFields())
        .map(f -> getFieldValue(f))
        .collect(Collectors.toList());
  }

  public static String getFieldValue(Field field) {
    try {
      return field.get(null).toString();
    } catch (IllegalAccessException ex) {
      log.warn(
          "Illegal access exception. Variable: {} is either private or non-static",
          field.getName());
      return "";
    }
  }

  /**
   * returns true if the updated value is different than the original value, otherwise false. If the
   * updated value is null, then it returns false
   */
  public static <T> boolean isUpdated(T originalValue, T updatedValue) {
    return !isNull(updatedValue) && !updatedValue.equals(originalValue);
  }

  public static <T> void onUpdateDetected(
      T originalValue, T updatedValue, @NonNull Runnable callback) {
    if (isUpdated(originalValue, updatedValue)) {
      callback.run();
    }
  }
}
