package org.overture.ego.token;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Arrays;

public class JWTAuthenticationToken extends AbstractAuthenticationToken {

    String token = "";
    public JWTAuthenticationToken(String token){
        super(Arrays.asList(new SimpleGrantedAuthority("ROLE_USER")));
        this.token = token;
    }
    @Override
    public Object getCredentials() {
        return token;
    }

    @Override
    public Object getPrincipal() {
        return token;
    }
}
