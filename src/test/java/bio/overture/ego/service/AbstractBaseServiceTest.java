package bio.overture.ego.service;

import lombok.Setter;
import org.junit.Ignore;
import org.junit.Test;

@Ignore
public abstract class AbstractBaseServiceTest<T, ID, S extends BaseService<T, ID>> {

  @Setter private S service;

  @Test
  public void entityDeletion_WhenExisting_SuccessfullyDeleted(){
  }

  @Test
  public void entityDeletion_WhenNotExisting_ThrowsNotFoundError(){
  }

  @Test
  public void entityExistence_WhenExisting_True(){
  }

  @Test
  public void entityExistence_WhenNotExisting_False(){
  }

  @Test
  public void findEntityById_WhenExisting_Present(){
  }

  @Test
  public void findEntityById_WhenNotExisting_NotPresent(){
  }

  @Test
  public void getEntityById_WhenExisting_Success(){
  }

  @Test
  public void getEntityById_WhenNotExisting_ThrowsNotFoundError(){
  }

  @Test
  public void getEntityName_OfCurrentEntityClass_Matching(){
  }

  @Test
  public void checkEntityExistence_WhenExisting_Nothing(){
  }

  @Test
  public void checkEntityExistence_WhenNotExisting_ThrowsNotFoundError(){
  }

  @Test
  public void getManyEntities_WhenAllExisting_AllMatching(){
  }

  @Test
  public void getManyEntities_WhenSomeExisting_AllExistingAreMatching(){
  }

  @Test
  public void getManyEntities_WhenNoneExisting_EmptyCollection(){
  }

}
