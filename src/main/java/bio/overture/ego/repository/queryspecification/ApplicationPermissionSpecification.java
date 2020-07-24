package bio.overture.ego.repository.queryspecification;

import bio.overture.ego.model.entity.ApplicationPermission;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ApplicationPermissionSpecification
    extends AbstractPermissionSpecification<ApplicationPermission> {}
