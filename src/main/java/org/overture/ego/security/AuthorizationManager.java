package org.overture.ego.security;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.overture.ego.token.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;


@Slf4j
public class AuthorizationManager {

    @Autowired
    TokenService tokenService;

    public boolean authorize(@NonNull Authentication authentication) {

        //String tokenPayload = request.getHeader(HttpHeaders.AUTHORIZATION);
        String tokenPayload = authentication.getPrincipal().toString();
        // remove Bearer from token
        if(tokenPayload == null || tokenPayload.isEmpty()) return false;
        if(!tokenPayload.contains("Bearer ")) return false;
        tokenPayload = tokenPayload.split("Bearer ")[1];
        return tokenService.validateToken(tokenPayload);

    }
}
