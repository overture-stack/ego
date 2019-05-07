package bio.overture.ego.service;

import static java.lang.String.format;

import bio.overture.ego.model.exceptions.NotFoundException;
import bio.overture.ego.repository.queryspecification.builder.AbstractSpecificationBuilder;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.NonNull;
import lombok.val;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

public interface BaseService<T, ID> {

  String getEntityTypeName();

  default T getById(@NonNull ID id) {
    val entity = findById(id);
    return entity.orElseThrow(
        () ->
            new NotFoundException(
                format(
                    "The '%s' entity with id '%s' does not exist",
                    getEntityTypeName(), id.toString())));
  }

  Optional<T> findById(ID id);

  boolean isExist(ID id);

  void delete(ID id);

  Page<T> findAll(Specification specification, Pageable pageable);

  Page<T> findAll(AbstractSpecificationBuilder<T, ID> specificationBuilder, Pageable pageable);

  List<T> getMany(Collection<ID> ids, AbstractSpecificationBuilder<T, ID> specificationBuilder);

  Set<T> getMany(Collection<ID> ids);

  T getWithRelationships(ID id);

  void checkExistence(Collection<ID> ids);

  void checkExistence(ID id);
}
