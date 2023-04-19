package bio.overture.ego.service;

import bio.overture.ego.model.entity.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
public class VisaPermissionService {

  /** Dependencies */
  @Autowired private VisaService visaService;
}
