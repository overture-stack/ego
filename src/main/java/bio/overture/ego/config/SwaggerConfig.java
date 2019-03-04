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

import lombok.Getter;
import lombok.Setter;
import lombok.val;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.paths.RelativePathProvider;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@EnableSwagger2
@Configuration
public class SwaggerConfig {

  @Component
  @ConfigurationProperties(prefix="swagger")
  class SwaggerProperties {
    /**
     * Specify host if ego is running behind proxy.
     */
    @Setter @Getter private String host = "";

    /**
     * If there is url write rule, you may want to set this variable. This value requires host to be not empty.
     */
    @Setter @Getter private String baseUrl = "";
  }

  @Bean
  public Docket productApi(SwaggerProperties properties) {
    val docket = new Docket(DocumentationType.SWAGGER_2)
            .select()
            .apis(RequestHandlerSelectors.basePackage("bio.overture.ego.controller"))
            .build()
            .host(properties.host)
            .pathProvider(new RelativePathProvider(null) {
                            @Override
                            public String getApplicationBasePath() {
                              return properties.getBaseUrl();
                            }
                          })
            .apiInfo(metaInfo());

    return docket;
  }

  private ApiInfo metaInfo() {

    return new ApiInfo(
            "ego Service API",
            "ego API Documentation",
            "0.02",
            "",
            new Contact("", "",""),
            "Apache License Version 2.0",
            "");
  }
}
