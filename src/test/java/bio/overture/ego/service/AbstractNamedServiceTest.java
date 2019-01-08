package bio.overture.ego.service;

import lombok.NonNull;
import org.junit.Test;

public abstract class AbstractNamedServiceTest<T, ID> extends AbstractBaseServiceTest<T, ID> {

  private final NamedService<T, ID> service;

  public AbstractNamedServiceTest(@NonNull NamedService<T, ID> service) {
    super(service);
    this.service = service;
  }

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
