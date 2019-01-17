package bio.overture.ego.model.entity;

import bio.overture.ego.model.enums.Fields;
import bio.overture.ego.model.enums.LombokFields;
import bio.overture.ego.model.enums.Tables;
import bio.overture.ego.view.Views;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = Tables.USER_PERMISSION)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonView(Views.REST.class)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true, of = { LombokFields.id })
public class UserPermission extends AbstractPermission {

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(nullable = false, name = Fields.USERID_JOIN)
  private User owner;

}
