package bio.overture.ego.repository.queryspecification.builder;

import static bio.overture.ego.model.enums.JavaFields.NAME;

import bio.overture.ego.model.enums.JavaFields;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.Collection;
import lombok.NonNull;
import lombok.val;
import org.springframework.data.jpa.domain.Specification;

public abstract class AbstractSpecificationBuilder<T, ID> {

  protected abstract Root<T> setupFetchStrategy(Root<T> root);

  public Specification<T> buildByNameIgnoreCase(@NonNull String name) {
    return (fromUser, query, builder) -> {
      val root = setupFetchStrategy(fromUser);
      return equalsNameIgnoreCasePredicate(root, builder, name);
    };
  }

  public Specification<T> listAll() {
    return (fromUser, query, builder) -> {
      val root = setupFetchStrategy(fromUser);
      return null;
    };
  }

  public Specification<T> buildById(@NonNull ID id) {
    return (fromUser, query, builder) -> {
      val root = setupFetchStrategy(fromUser);
      return equalsIdPredicate(root, builder, id);
    };
  }

  public Specification<T> buildByIds(@NonNull Collection<ID> ids) {
    return (fromUser, query, builder) -> {
      val root = setupFetchStrategy(fromUser);
      return whereInIdsPredicate(root, ids);
    };
  }

  private Predicate whereInIdsPredicate(Root<T> root, Collection<ID> ids) {
    return root.get(JavaFields.ID).in(ids);
  }

  private Predicate equalsIdPredicate(Root<T> root, CriteriaBuilder builder, ID id) {
    return builder.equal(root.get(JavaFields.ID), id);
  }

  private Predicate equalsNameIgnoreCasePredicate(
      Root<T> root, CriteriaBuilder builder, String name) {
    return builder.equal(builder.upper(root.get(NAME)), builder.upper(builder.literal(name)));
  }
}
