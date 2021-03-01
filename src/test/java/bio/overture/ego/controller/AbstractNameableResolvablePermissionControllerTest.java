package bio.overture.ego.controller;

import bio.overture.ego.model.entity.AbstractPermission;
import bio.overture.ego.model.entity.NameableEntity;
import java.util.UUID;

public abstract class AbstractNameableResolvablePermissionControllerTest<
        O extends NameableEntity<UUID>, P extends AbstractPermission<O>>
    extends AbstractResolvablePermissionControllerTest<O, P> {}
