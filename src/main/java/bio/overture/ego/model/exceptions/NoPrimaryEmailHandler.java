package bio.overture.ego.model.exceptions;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

@Slf4j
public class NoPrimaryEmailHandler implements AccessDeniedHandler {

  @Override
  public void handle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, AccessDeniedException e) throws IOException, ServletException {
    //httpServletResponse.sendError(403, "There was email error");
    log.info("I'm trying I promise...");
    httpServletResponse.sendRedirect(httpServletRequest.getContextPath() + "/goaway/");
  }

}
