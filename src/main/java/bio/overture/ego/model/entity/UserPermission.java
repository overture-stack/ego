package bio.overture.ego.model.entity;

import bio.overture.ego.model.enums.JavaFields;
import bio.overture.ego.model.enums.SqlFields;
import bio.overture.ego.model.enums.Tables;
import bio.overture.ego.view.Views;
import com.fasterxml.jackson.annotation.JsonView;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedAttributeNode;
import javax.persistence.NamedEntityGraph;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = Tables.USER_PERMISSION)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonView(Views.REST.class)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NamedEntityGraph(
    name = "user-permission-entity-with-relationships",
    attributeNodes = {
      @NamedAttributeNode(value = JavaFields.POLICY),
      @NamedAttributeNode(value = JavaFields.OWNER)
    })
public class UserPermission extends AbstractPermission<User> {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = SqlFields.USERID_JOIN, nullable = false)
  private User owner;
}
