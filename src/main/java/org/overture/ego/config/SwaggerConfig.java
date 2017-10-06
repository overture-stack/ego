package org.overture.ego.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import static springfox.documentation.builders.PathSelectors.regex;

@EnableSwagger2
@Configuration
public class SwaggerConfig {

    @Bean
    public Docket productApi() {
        return new Docket(DocumentationType.SWAGGER_2)
                .select()
                .apis(RequestHandlerSelectors.basePackage("org.overture.ego.controller"))
                .build()
                .apiInfo(metaInfo());
    }

    private ApiInfo metaInfo() {

        return new ApiInfo(
                "eGO Service API",
                "Kids First Authorization API Docs",
                "0.01",
                "",
                "",
                "Apache License Version 2.0",
                ""
        );
    }

}
