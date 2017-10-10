package org.overture.ego.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/oauth")
public class AuthController {

    @RequestMapping(method = RequestMethod.GET, value = "/google/login")
    @ResponseStatus(value = HttpStatus.OK)
    public void loginWithGoogle() {

    }

    @RequestMapping(method = RequestMethod.GET, value = "/google/token")
    @ResponseStatus(value = HttpStatus.OK)
    public void exchangeGoogleTokenForAuth(
            @RequestHeader(value = "id_token", required = true) final String googleToken) {


    }
}
