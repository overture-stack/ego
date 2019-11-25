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

package bio.overture.ego.security;

import bio.overture.ego.service.ApplicationService;
import bio.overture.ego.utils.Redirects;
import java.net.URI;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

// The filter it's better to add to security chain, reference
// https://spring.io/guides/topicals/spring-security-architecture/
@Component
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorsFilter implements Filter {
  @Autowired private ApplicationService applicationService;

  @Override
  @SneakyThrows
  public void doFilter(ServletRequest req, ServletResponse res, FilterChain filterChain) {
    final HttpServletResponse response = (HttpServletResponse) res;
    final HttpServletRequest request = (HttpServletRequest) req;

    String clientId = request.getParameter("client_id");
    String redirectUri = request.getParameter("redirect_uri");

    // allow ego app access token at /oauth/ego-token
    if (clientId != null) {
      try {
        val app = applicationService.getByClientId(clientId);
        val uri = new URI(Redirects.getRedirectUri(app, redirectUri));
        response.setHeader(
            "Access-Control-Allow-Origin",
            uri.getScheme()
                + "://"
                + uri.getHost()
                + (uri.getPort() == -1 ? "" : ":" + uri.getPort()));
      } catch (NullPointerException ex) {
        log.warn(ex.getMessage());
      }
    } else {
      response.addHeader("Access-Control-Allow-Origin", "*");
    }

    response.addHeader(
        "Access-Control-Allow-Methods", "GET, POST, DELETE, PUT, PATCH, HEAD, OPTIONS");
    response.addHeader(
        "Access-Control-Allow-Headers",
        "Origin, Accept, X-Requested-With, Content-Type, Access-Control-Request-Method, "
            + "Access-Control-Request-Headers, token, AUTHORIZATION");
    response.addHeader(
        "Access-Control-Expose-Headers",
        "Access-Control-Allow-Origin, Access-Control-Allow-Credentials");
    response.addHeader("Access-Control-Allow-Credentials", "true");
    response.addIntHeader("Access-Control-Max-Age", 10);
    if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
      response.setStatus(HttpServletResponse.SC_OK);
    } else {
      filterChain.doFilter(request, response);
    }
  }

  @Override
  public void init(FilterConfig filterConfig) {}

  @Override
  public void destroy() {}
}
