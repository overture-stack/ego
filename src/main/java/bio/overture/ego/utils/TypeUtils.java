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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.regex.Pattern;
import lombok.val;

public class TypeUtils {
  public static <T> T convertToAnotherType(
      Object fromObject, Class<T> tClass, Class<?> serializationView) throws IOException {
    val mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    mapper.configure(JsonGenerator.Feature.IGNORE_UNKNOWN, true);
    mapper.configure(MapperFeature.DEFAULT_VIEW_INCLUSION, false);
    val serializedValue = mapper.writerWithView(serializationView).writeValueAsBytes(fromObject);
    return mapper.readValue(serializedValue, tClass);
  }

  public static <T> T convertToAnotherType(Object fromObject, Class<T> tClass) {
    val mapper = new ObjectMapper();
    mapper.configure(JsonGenerator.Feature.IGNORE_UNKNOWN, true);
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    return mapper.convertValue(fromObject, tClass);
  }

  private static final Pattern UUID_REGEX_PATTERN =
      Pattern.compile("^[{]?[0-9a-fA-F]{8}-([0-9a-fA-F]{4}-){3}[0-9a-fA-F]{12}[}]?$");

  public static boolean isValidUUID(String str) {
    if (str == null) {
      return false;
    }
    return UUID_REGEX_PATTERN.matcher(str).matches();
  }
}
