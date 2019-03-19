package bio.overture.ego.service.association;

import bio.overture.ego.model.entity.Identifiable;
import bio.overture.ego.model.search.SearchFilter;
import bio.overture.ego.repository.BaseRepository;
import bio.overture.ego.service.BaseService;
import com.google.common.collect.ImmutableSet;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.Repository;
import reactor.jarjar.jsr166e.ConcurrentHashMapV8;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import static bio.overture.ego.model.exceptions.MalformedRequestException.checkMalformedRequest;
import static bio.overture.ego.model.exceptions.NotFoundException.buildNotFoundException;
import static bio.overture.ego.model.exceptions.UniqueViolationException.checkUnique;
import static bio.overture.ego.utils.CollectionUtils.difference;
import static bio.overture.ego.utils.CollectionUtils.findDuplicates;
import static bio.overture.ego.utils.CollectionUtils.intersection;
import static bio.overture.ego.utils.Converters.convertToIds;
import static bio.overture.ego.utils.Joiners.PRETTY_COMMA;
import static com.google.common.base.Strings.isNullOrEmpty;

@Builder
@RequiredArgsConstructor
public class ManyToManyAssociationService<P extends Identifiable<UUID>, C extends Identifiable<UUID>> implements AssociationService<P, C, UUID> {

  private final Class<P> parentType;
  private final Class<C> childType;
  private final BaseRepository<P, UUID> parentRepository;
  private final BaseService<P, UUID> parentService;
  private final BaseService<C, UUID> childService;
  private final Function<P, Collection<C>> getChildrenFromParentFunction;
  private final Function<C, Collection<P>> getParentsFromChildFunction;
  private final Function<FindRequest, Specification<P>> parentFindRequestSpecificationCallback;

  @Override
  public P associateParentWithChildren(@NonNull UUID parentId, @NonNull Collection<UUID> childIds){
    // check duplicate childIds
    checkDuplicates(childType, childIds);

    // Get existing associated child ids with the parent
    val parentWithChildren = parentService.getWithRelationships(parentId);
    val children = getChildrenFromParentFunction.apply(parentWithChildren);
    val existingAssociatedChildIds = convertToIds(children);

    // Check there are no application ids that are already associated with the parent
    val existingAlreadyAssociatedChildIds = intersection(existingAssociatedChildIds, childIds);
    checkUnique(existingAlreadyAssociatedChildIds.isEmpty(),
        "The following %s ids are already associated with %s '%s': [%s]",
        childType.getSimpleName(),
        parentType.getSimpleName(),
        parentId,
        PRETTY_COMMA.join(existingAlreadyAssociatedChildIds));

    // Get all unassociated child ids. If they do not exist, an error is thrown
    val nonAssociatedChildIds = difference(childIds,existingAssociatedChildIds);
    val nonAssociatedChildren = childService.getMany(nonAssociatedChildIds);

    // Associate the existing children with the parent
    associate(parentWithChildren, nonAssociatedChildren);
    return parentRepository.save(parentWithChildren);
  }

  @Override
  public void disassociateParentFromChildren(@NonNull UUID parentId, @NonNull Collection<UUID> childIds){
    // check duplicate childIds
    checkDuplicates(childType, childIds);

    // Get existing associated child ids with the parent
    val parentWithChildren = parentService.getWithRelationships(parentId);
    val children = getChildrenFromParentFunction.apply(parentWithChildren);
    val existingAssociatedChildIds = convertToIds(children);

    // Get existing and non-existing non-associated child ids. Error out if there are existing and non-existing non-associated child ids
    val nonAssociatedChildIds = difference(childIds, existingAssociatedChildIds);
    if (!nonAssociatedChildIds.isEmpty()){
      childService.checkExistence(nonAssociatedChildIds);
      throw buildNotFoundException(
          "The following existing %s ids cannot be disassociated from %s '%s' "
              + "because they are not associated with it",
          childType.getSimpleName(), parentType.getSimpleName(), parentId);
    }

    // Since all child ids exist and are associated with the parent, disassociate them from eachother
    val childIdsToDisassociate = ImmutableSet.copyOf(childIds);
    disassociate(parentWithChildren, childIdsToDisassociate);
    parentRepository.save(parentWithChildren);
  }

  private static <T extends Identifiable<UUID>> void checkDuplicates(Class<T> childType, Collection<UUID> ids){
    // check duplicate childIds
    val duplicateChildIds = findDuplicates(ids);
    checkMalformedRequest(duplicateChildIds.isEmpty(),
        "The following %s ids contain duplicates: [%s]" ,
        childType.getSimpleName(), PRETTY_COMMA.join(duplicateChildIds));
  }

  private void associate(P parentWithChildren, Collection<C> children){
    getChildrenFromParentFunction.apply(parentWithChildren).addAll(children);
    children.stream()
        .map(getParentsFromChildFunction)
        .forEach(parents -> parents.add(parentWithChildren));
  }

  private void disassociate(P parentWithChildren, Collection<UUID> childIds){
    val children = getChildrenFromParentFunction.apply(parentWithChildren);
    children.stream()
        .filter(x -> childIds.contains(x.getId()))
        .map(getParentsFromChildFunction)
        .forEach(x -> x.remove(parentWithChildren));
    children.removeIf(x -> childIds.contains(x.getId()));
  }

  @Override
  public Page<P> findParentsForChild(FindRequest findRequest) {
    val spec = parentFindRequestSpecificationCallback.apply(findRequest);
    return parentRepository.findAll(spec, findRequest.getPageable());
  }

}