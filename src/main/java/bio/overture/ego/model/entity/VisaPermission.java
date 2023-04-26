package bio.overture.ego.model.entity;

import bio.overture.ego.model.enums.JavaFields;
import bio.overture.ego.model.enums.SqlFields;
import bio.overture.ego.model.enums.Tables;
import bio.overture.ego.view.Views;
import com.fasterxml.jackson.annotation.JsonView;
import javax.persistence.*;
import lombok.*;
import lombok.experimental.FieldNameConstants;

@Entity
@Table(name = Tables.ACLVISAPERMISSION)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonView(Views.REST.class)
@ToString(callSuper = true)
@FieldNameConstants
@EqualsAndHashCode(
    callSuper = true,
    of = {"id"})
@NamedEntityGraph(
    name = "visa-permission-entity-with-relationships",
    attributeNodes = {
      @NamedAttributeNode(value = JavaFields.POLICY),
      @NamedAttributeNode(value = JavaFields.VISA)
    })
public class VisaPermission extends AbstractPermission<Visa> {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = SqlFields.VISA_ID, nullable = false)
  private Visa visa;

  @Override
  public Visa getOwner() {
    return null;
  }

  @Override
  public void setOwner(Visa owner) {}
}
