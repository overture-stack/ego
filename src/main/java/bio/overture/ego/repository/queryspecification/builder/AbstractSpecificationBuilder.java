package bio.overture.ego.repository.queryspecification.builder;

import static bio.overture.ego.model.enums.JavaFields.NAME;

import bio.overture.ego.model.entity.Identifiable;
import bio.overture.ego.model.enums.JavaFields;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import lombok.NonNull;
import lombok.val;
import org.springframework.data.jpa.domain.Specification;

public abstract class AbstractSpecificationBuilder<T extends Identifiable<ID>, ID> {

  protected abstract Root<T> setupFetchStrategy(Root<T> root);

  public Specification<T> buildByNameIgnoreCase(@NonNull String name) {
    return (fromUser, query, builder) -> {
      val root = setupFetchStrategy(fromUser);
      return equalsNameIgnoreCasePredicate(root, builder, name);
    };
  }

  public Specification<T> buildById(@NonNull ID id) {
    return (fromUser, query, builder) -> {
      val root = setupFetchStrategy(fromUser);
      return equalsIdPredicate(root, builder, id);
    };
  }

  private Predicate equalsIdPredicate(Root<T> root, CriteriaBuilder builder, ID id) {
    return builder.equal(root.get(JavaFields.ID), id);
  }

  private Predicate equalsNameIgnoreCasePredicate(
      Root<T> root, CriteriaBuilder builder, String name) {
    return builder.equal(builder.upper(root.get(NAME)), builder.upper(builder.literal(name)));
  }
}
