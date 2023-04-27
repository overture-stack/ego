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

package bio.overture.ego.config;

import static bio.overture.ego.utils.SwaggerConstants.*;

import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.apache.http.client.utils.URIBuilder;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

/** Open API Configuration Bean */
@Configuration
public class SwaggerConfig {

  private final BuildProperties buildProperties;

  @Autowired
  public SwaggerConfig(@NonNull BuildProperties buildProperties) {
    this.buildProperties = buildProperties;
  }

  @Bean
  public OpenAPI productApi(SwaggerProperties swaggerProperties) {

    URIBuilder uriBuilder = null;
    try {
      uriBuilder = new URIBuilder(swaggerProperties.host);
      uriBuilder.setPath(swaggerProperties.baseUrl).build().normalize();
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }

    return new OpenAPI()
        .info(metaInfo())
        .servers(List.of(new Server().url(uriBuilder.toString())))
        .components(new Components().addSecuritySchemes(SECURITY_SCHEME_NAME, securityScheme()));
  }

  private Info metaInfo() {

    return new Info()
        .title("Ego Service API")
        .description("Ego API Documentation")
        .version(buildProperties.getVersion())
        .contact(new Contact())
        .license(new License().name("GNU Affero General Public License v3.0"));
  }

  private static SecurityScheme securityScheme() {
    return new SecurityScheme()
        .name(SECURITY_SCHEME_NAME)
        .type(SecurityScheme.Type.HTTP)
        .scheme("bearer")
        .bearerFormat("JWT");
  }

  private static boolean isApplicationScopedPath(@NonNull String path) {
    return APPLICATION_SCOPED_PATHS.contains(path);
  }

  private static List<Parameter> generatePostAccessTokenParameters() {
    return POST_ACCESS_TOKEN_PARAMS.stream()
        .map(
            name ->
                new Parameter()
                    .schema(new Schema().type("string"))
                    .name(name)
                    .in(ParameterIn.QUERY.toString())
                    .required(true))
        .collect(Collectors.toList());
  }

  @Component
  @ConfigurationProperties(prefix = "swagger")
  @Setter
  @Getter
  class SwaggerProperties {
    /** Specify host if ego is running behind proxy. */
    private String host = "";

    /**
     * If there is url write rule, you may want to set this variable. This value requires host to be
     * not empty.
     */
    private String baseUrl = "";
  }

  @Bean
  public OpenApiCustomizer openApiCustomiser() {
    return openApi -> {
      openApi
          .getPaths()
          .forEach(
              (path, pathItem) -> {

                // We want the default Bearer auth applied only for non-ApplicationScoped endpoints.
                // For ApplicationScoped endpoints, an explicit RequestHeader
                if (!isApplicationScopedPath(path)) {
                  pathItem
                      .readOperations()
                      .forEach(
                          operation -> {
                            operation.addSecurityItem(
                                new SecurityRequirement().addList(SECURITY_SCHEME_NAME));
                          });
                }
              });

      // generate access token parameters
      PathItem accessTokenPath =
          new PathItem()
              .post(
                  new Operation()
                      .addTagsItem("Auth")
                      .parameters(generatePostAccessTokenParameters()));
      openApi.getPaths().addPathItem("/oauth/token", accessTokenPath);
    };
  }
}
