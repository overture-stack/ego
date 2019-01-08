package bio.overture.ego.service;

import org.junit.Ignore;
import org.junit.Test;


@Ignore
public abstract class AbstractNamedServiceTest<T, ID, S extends NamedService<T, ID>> extends AbstractBaseServiceTest<T, ID, S> {

  @Test
  public void getEntityByName_WhenExisting_Success() {
  }

  @Test
  public void getEntityByName_WhenNotExisting_ThrowsNotFoundError() {
  }

  @Test
  public void findEntityByName_WhenExisting_PresentAndMatching() {
  }

  @Test
  public void findEntityByName_WhenNotExisting_NotPresent() {
  }

}
