package bio.overture.ego.model.entity;

import bio.overture.ego.model.enums.LombokFields;
import bio.overture.ego.model.enums.SqlFields;
import bio.overture.ego.model.enums.Tables;
import bio.overture.ego.view.Views;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

@Entity
@Table(name = Tables.GROUP_PERMISSION)
@Data
@JsonInclude()
@AllArgsConstructor
@NoArgsConstructor
@JsonView(Views.REST.class)
@ToString(callSuper = true)
@EqualsAndHashCode(
    callSuper = true,
    of = {LombokFields.id})
public class GroupPermission extends AbstractPermission {

  //Owning side
  @NotNull
  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = SqlFields.GROUPID_JOIN, nullable = false)
  private Group owner;
}
