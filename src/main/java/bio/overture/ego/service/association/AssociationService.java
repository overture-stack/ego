package bio.overture.ego.service.association;

import bio.overture.ego.model.entity.Identifiable;
import java.util.Collection;
import org.springframework.data.domain.Page;

public interface AssociationService<P extends Identifiable<ID>, C extends Identifiable<ID>, ID> {

  P associateParentWithChildren(ID parentId, Collection<ID> childIds);

  void disassociateParentFromChildren(ID parentId, Collection<ID> childIds);

  Page<P> findParentsForChild(FindRequest findRequest);
}
