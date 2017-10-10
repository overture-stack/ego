package org.overture.ego;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.core.annotation.Order;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableOAuth2Client;

@SpringBootApplication
@EnableOAuth2Client
@EnableAuthorizationServer
@Order(SecurityProperties.ACCESS_OVERRIDE_ORDER)
public class AuthorizationServiceMain  {

	public static void main(String[] args) {
		SpringApplication.run(AuthorizationServiceMain.class, args);
	}
}
