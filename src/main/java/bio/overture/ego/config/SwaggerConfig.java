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

import static springfox.documentation.builders.RequestHandlerSelectors.basePackage;
import static springfox.documentation.spi.DocumentationType.SWAGGER_2;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.ApiKey;
import springfox.documentation.service.AuthorizationScope;
import springfox.documentation.service.Contact;
import springfox.documentation.service.SecurityReference;
import springfox.documentation.service.VendorExtension;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.paths.RelativePathProvider;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@EnableSwagger2
@Configuration
public class SwaggerConfig {

  private static final Set<String> APPLICATION_SCOPED_PATHS =
      Set.of(
          "/o/check_api_key",
          "/o/check_token",
          "/transaction/group_permissions",
          "/transaction/mass_delete");

  private final BuildProperties buildProperties;

  @Autowired
  public SwaggerConfig(@NonNull BuildProperties buildProperties) {
    this.buildProperties = buildProperties;
  }

  @Bean
  public Docket productApi(SwaggerProperties swaggerProperties) {
    val docket =
        new Docket(SWAGGER_2)
            .select()
            .apis(basePackage("bio.overture.ego.controller"))
            .build()
            .host(swaggerProperties.host)
            .pathProvider(
                new RelativePathProvider(null) {
                  @Override
                  public String getApplicationBasePath() {
                    return swaggerProperties.getBaseUrl();
                  }
                })
            .securitySchemes(List.of(apiKey()))
            .securityContexts(List.of(securityContext()))
            .apiInfo(metaInfo())
            .produces(Set.of("application/json"))
            .consumes(Set.of("application/json"));
    return docket;
  }

  private ApiInfo metaInfo() {

    return new ApiInfo(
        "Ego Service API",
        "Ego API Documentation",
        buildProperties.getVersion(),
        "",
        new Contact("", "", ""),
        "GNU Affero General Public License v3.0",
        "",
        new ArrayList<VendorExtension>());
  }

  private static ApiKey apiKey() {
    return new ApiKey("Bearer", "Authorization", "header");
  }

  private static SecurityContext securityContext() {
    return SecurityContext.builder()
        .securityReferences(List.of(securityReference()))
        // We want the default Bearer auth applied only for non-ApplicationScoped endpoints.
        // For ApplicationScoped endpoints, an explicit RequestHeader
        // fields will be present in the ui
        .forPaths(x -> !isApplicationScopedPath(x))
        .build();
  }

  private static boolean isApplicationScopedPath(@NonNull String path) {
    return APPLICATION_SCOPED_PATHS.contains(path);
  }

  private static SecurityReference securityReference() {
    return SecurityReference.builder()
        .reference("Bearer")
        .scopes(new AuthorizationScope[0])
        .build();
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
}
