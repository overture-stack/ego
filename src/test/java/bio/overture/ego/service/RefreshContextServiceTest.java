package bio.overture.ego.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

// this should test the service layer calls -> create, update, delete
@Slf4j
@SpringBootTest
@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@Transactional
public class RefreshContextServiceTest {

  @Test
  public void refreshTokenIsDeletedAfterUse() {
  }

  @Test
  public void userWithRefreshTokenIsUnique() {}

//  @Test
//  public void

}

// need to test create by itself here?
// usedRefreshTokenIsDeleted
