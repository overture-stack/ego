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
@Table(name = Tables.APPLICATION_PERMISSION)
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
    name = "application-permission-entity-with-relationships",
    attributeNodes = {
      @NamedAttributeNode(value = JavaFields.POLICY),
      @NamedAttributeNode(value = JavaFields.OWNER)
    })
public class ApplicationPermission extends AbstractPermission<Application> {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = SqlFields.APPID_JOIN, nullable = false)
  private Application owner;
}
