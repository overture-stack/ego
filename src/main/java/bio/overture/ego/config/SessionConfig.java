/*
 * Copyright (c) 2020. The Ontario Institute for Cancer Research. All rights reserved.
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

package bio.overture.ego.config;

import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.session.MapSessionRepository;
import org.springframework.session.config.annotation.web.http.EnableSpringHttpSession;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

@EnableSpringHttpSession
public class SessionConfig {

  private final LoginNonceProperties properties;

  public SessionConfig(@Autowired LoginNonceProperties properties) {
    this.properties = properties;
  }

  /**
   * Use in memory data store. TODO: Add support for redis and psql data store configurable behind
   * profiles.
   *
   * @return SessionRepository implementation.
   */
  @Bean
  public MapSessionRepository sessionRepository() {
    return new MapSessionRepository(new ConcurrentHashMap<>());
  }

  @Bean
  public CookieSerializer cookieSerializer() {
    DefaultCookieSerializer serializer = new DefaultCookieSerializer();

    // These can be user configured
    serializer.setCookieName(properties.getName());
    serializer.setSameSite(properties.getSameSite());
    serializer.setUseSecureCookie(properties.isSecure());
    serializer.setCookieMaxAge(properties.getMaxAge());

    // These shouldn't be user configurable. ALWAYS make sure its HTTP Only.
    serializer.setUseHttpOnlyCookie(true);
    serializer.setCookiePath("/");
    serializer.setDomainNamePattern("^.+?\\.(\\w+\\.[a-z]+)$");
    return serializer;
  }
}
