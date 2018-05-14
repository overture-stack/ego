package org.overture.ego.service;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.overture.ego.model.entity.Application;

public class ApplicationServiceTest {

  @Mock
  private Application application;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testCreate() {
    Assert.assertEquals(1, 1);
  }
}
