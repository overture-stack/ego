package bio.overture.ego.token;

import bio.overture.ego.model.entity.User;
import bio.overture.ego.service.TokenService;
import bio.overture.ego.service.UserService;
import bio.overture.ego.utils.EntityGenerator;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@Slf4j
@SpringBootTest
@RunWith(SpringRunner.class)
@ActiveProfiles("test")
public class LastloginTest {

    @Autowired
    private TokenService tokenService;

    @Autowired
    private UserService userService;

    @Autowired
    private EntityGenerator entityGenerator;

    @Test
    public void testLastloginUpdate(){
        IDToken idToken = new IDToken();
        idToken.setFamily_name("foo");
        idToken.setGiven_name("bar");
        idToken.setEmail("foobar@domain.com");
        User user = userService.create(entityGenerator.createUser("foo", "bar"));

        assertNull(" Verify before generatedUserToken, last login after fetching the user should be null. ",
                userService.getByName(idToken.getEmail()).getLastLogin());

        tokenService.generateUserToken(idToken);
        user = userService.getByName(idToken.getEmail());

        assertNotNull("Verify after generatedUserToken, last login is not null.",
                userService.getByName(idToken.getEmail()).getLastLogin());

        // Must manually delete user. Using @Transactional will
        // trigger exception, as there are two
        // threads involved, new thread will try to find user in an empty repo which
        // will cause exception.
        userService.delete(user.getId().toString());
    }
}
