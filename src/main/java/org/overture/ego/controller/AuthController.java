package org.overture.ego.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.overture.ego.service.UserService;
import org.overture.ego.token.GoogleTokenValidator;
import org.overture.ego.token.TokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.jwt.JwtHelper;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/oauth")
public class AuthController {

    @Autowired
    TokenUtil tokenUtil;
    @Autowired
    UserService userService;
    @Autowired
    GoogleTokenValidator tokenValidator;


    @RequestMapping(method = RequestMethod.GET, value = "/google/token")
    @ResponseStatus(value = HttpStatus.OK)
    @SneakyThrows
    public @ResponseBody  String exchangeGoogleTokenForAuth(
            @RequestHeader(value = "id_token", required = true) final String idToken) {
        if(!tokenValidator.validToken(idToken))
            throw new Exception("Invalid user token:" + idToken);
        val tokenDecoded = JwtHelper.decode(idToken);
        val authInfo = new ObjectMapper().readValue(tokenDecoded.getClaims(), Map.class);
        val userName = authInfo.get("email").toString();
        val user = userService.get(userName);
        if(user == null) throw new Exception("User doesn't exist: "+ userName);
        return tokenUtil.generateToken(user);

    }

    @RequestMapping(method = RequestMethod.GET, value = "/token/verify")
    @ResponseStatus(value = HttpStatus.OK)
    @SneakyThrows
    public @ResponseBody  boolean verifyJWToken(
            @RequestHeader(value = "token", required = true) final String token) {
        if(token == null || token.isEmpty()) return false;
        return tokenUtil.validateToken(token);
    }
}
