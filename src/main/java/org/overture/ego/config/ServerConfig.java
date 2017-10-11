package org.overture.ego.config;

import org.overture.ego.security.AuthorizationManager;
import org.overture.ego.security.CorsFilter;
import org.overture.ego.security.StatelessFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.oauth2.client.filter.OAuth2ClientContextFilter;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;


@Configuration
public class ServerConfig extends WebSecurityConfigurerAdapter {


    @Bean
    CorsFilter corsFilter(){
        return new CorsFilter();
    }

    //@Bean
    //StatelessFilter statelessFilter() {return new StatelessFilter("/users**");}

    //TODO: Configure security
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.antMatcher("/**")
                .authorizeRequests()
                .antMatchers("/**", "/swagger**").permitAll()
                .and().csrf().disable().authorizeRequests();
//                .and().addFilterAfter(new OAuth2ClientContextFilter(), AbstractPreAuthenticatedProcessingFilter.class)
//                .addFilterAfter(statelessFilter(), OAuth2ClientContextFilter.class);

    }

}
