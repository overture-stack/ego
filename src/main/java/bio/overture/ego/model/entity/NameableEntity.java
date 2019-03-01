package bio.overture.ego.model.entity;

public interface NameableEntity<ID> extends Identifiable<ID> {

  String getName();
}
