package bio.overture.ego.service.association.impl_old;

import bio.overture.ego.model.entity.Identifiable;
import bio.overture.ego.service.BaseService;
import com.google.common.collect.ImmutableSet;
import lombok.Builder;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.data.repository.CrudRepository;

import java.util.Collection;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static bio.overture.ego.model.exceptions.MalformedRequestException.checkMalformedRequest;
import static bio.overture.ego.model.exceptions.NotFoundException.buildNotFoundException;
import static bio.overture.ego.model.exceptions.UniqueViolationException.checkUnique;
import static bio.overture.ego.utils.CollectionUtils.difference;
import static bio.overture.ego.utils.CollectionUtils.findDuplicates;
import static bio.overture.ego.utils.CollectionUtils.intersection;
import static bio.overture.ego.utils.Collectors.toImmutableSet;
import static bio.overture.ego.utils.Converters.convertToIds;
import static bio.overture.ego.utils.Joiners.PRETTY_COMMA;
import static com.google.common.collect.Sets.newHashSet;

@Builder
@RequiredArgsConstructor
public class FunctionalAssociationService<P extends Identifiable<UUID>, C extends Identifiable<UUID>> {

  @NonNull private final Class<P> parentType;
  @NonNull private final Class<C> childType;
  @NonNull private final Function<P, Collection<C>> getChildrenCallback;
  @NonNull private final Function<UUID, P> getParentWithRelationshipsCallback;
  @NonNull private final BiConsumer<P, Collection<C>> associationCallback;
  @NonNull private final BiConsumer<P, Collection<C>> disassociationCallback;
  @NonNull private final BaseService<C, UUID> childService;
  @NonNull private final CrudRepository<P, UUID> parentRepository;

  public P associateParentWithChildren(@NonNull UUID parentId, @NonNull Collection<UUID> childIds){
    // check duplicate childIds
    val duplicateChildIds = findDuplicates(childIds);
    checkMalformedRequest(duplicateChildIds.isEmpty(),
        "The following %s ids contain duplicates: [%s]" ,
        childType.getSimpleName(), PRETTY_COMMA.join(duplicateChildIds));

    // Get existing associated child ids with the parent
    val parentWithChildren = getParentWithRelationshipsCallback.apply(parentId);
    val existingAssociatedChildIds = convertToIds(getChildrenCallback.apply(parentWithChildren));

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
    val unassociatedChildren = childService.getMany(nonAssociatedChildIds);

    // Associate the existing children with the parent
    associationCallback.accept(parentWithChildren, unassociatedChildren);
    return parentRepository.save(parentWithChildren);
  }

  public void disassociateParentFromChildren(@NonNull UUID parentId, @NonNull Collection<UUID> childIds){
    // check duplicate childIds
    val duplicateChildIds = findDuplicates(childIds);
    checkMalformedRequest(duplicateChildIds.isEmpty(),
        "The following %s ids contain duplicates: [%s]" ,
        childType.getSimpleName(), PRETTY_COMMA.join(duplicateChildIds));

    // Get existing associated child ids with the parent
    val parentWithChildren = getParentWithRelationshipsCallback.apply(parentId);
    val children = getChildrenCallback.apply(parentWithChildren);
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
    val childrenToDisassociate = children.stream()
        .filter(x -> childIdsToDisassociate.contains(x.getId()))
        .collect(toImmutableSet());
    disassociationCallback.accept(parentWithChildren, childrenToDisassociate);
    parentRepository.save(parentWithChildren);
  }

}
