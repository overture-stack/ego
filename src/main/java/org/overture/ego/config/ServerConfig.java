package org.overture.ego.config;

import org.overture.ego.security.CorsFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;


@Configuration
public class ServerConfig extends WebSecurityConfigurerAdapter {


    @Bean
    CorsFilter corsFilter(){
        return new CorsFilter();
    }

    //TODO: Configure security
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.antMatcher("/**")
                .authorizeRequests()
                .antMatchers("/**", "/swagger**").permitAll()
                .and().csrf().disable();


    }

}
