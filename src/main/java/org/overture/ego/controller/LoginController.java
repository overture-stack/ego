package org.overture.ego.controller;

import lombok.extern.slf4j.Slf4j;
import org.overture.ego.security.ProjectCodeScoped;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/login")
public class LoginController {

    @ProjectCodeScoped
    @RequestMapping(method = RequestMethod.GET, value = "/google")
    @ResponseStatus(value = HttpStatus.OK)
    public void loginWithGoogle() {

    }
}
