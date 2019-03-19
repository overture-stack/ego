package bio.overture.ego.service.association;

import bio.overture.ego.model.entity.Identifiable;
import bio.overture.ego.model.search.SearchFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface AssociationService<P extends Identifiable<ID>,
    C extends Identifiable<ID>, ID> {

  P associateParentWithChildren(ID parentId, Collection<ID> childIds);

  void disassociateParentFromChildren(ID parentId, Collection<ID> childIds);

  Page<P> findParentsForChild(FindRequest findRequest);

}
