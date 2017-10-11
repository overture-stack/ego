package org.overture.ego.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.method.configuration.GlobalMethodSecurityConfiguration;
import org.springframework.security.oauth2.provider.expression.OAuth2MethodSecurityExpressionHandler;

@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true)
@Order(SecurityProperties.ACCESS_OVERRIDE_ORDER)
public class AuthorizationStrategyConfig  extends GlobalMethodSecurityConfiguration {

    @Autowired
    private ApplicationContext context;

    @Override
    protected MethodSecurityExpressionHandler createExpressionHandler() {
        OAuth2MethodSecurityExpressionHandler handler = new OAuth2MethodSecurityExpressionHandler();
        handler.setApplicationContext(context);
        return handler;
    }

    @Bean
    public AuthorizationManager authorizationManager() {
        return new AuthorizationManager();
    }

}
