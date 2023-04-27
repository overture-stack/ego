package bio.overture.ego.model.entity;


import bio.overture.ego.model.enums.AccessLevel;
import bio.overture.ego.model.enums.SqlFields;
import bio.overture.ego.model.enums.Tables;
import bio.overture.ego.view.Views;
import com.fasterxml.jackson.annotation.JsonView;
import com.vladmihalcea.hibernate.type.basic.PostgreSQLEnumType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.util.Collection;
import java.util.UUID;
import lombok.*;
import lombok.experimental.FieldNameConstants;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;

@Entity
@Table(name = Tables.ACLVISAPERMISSION)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonView(Views.REST.class)
@ToString(callSuper = true)
@FieldNameConstants
public class VisaPermission extends AbstractPermission<Visa> {

  @Id
  @Column(name = SqlFields.ID, updatable = false, nullable = false)
  @GenericGenerator(name = "aclp_uuid", strategy = "org.hibernate.id.UUIDGenerator")
  @GeneratedValue(generator = "aclp_uuid")
  private UUID id;

  @JoinColumn(name = SqlFields.ID, nullable = false)
  private UUID entity;

  @JoinColumn(name = SqlFields.ID, nullable = false)
  private UUID visaId;

  @NotNull
  @Column(name = SqlFields.MASK, nullable = false)
  @Enumerated(EnumType.STRING)
  @Type(value = PostgreSQLEnumType.class)
  private AccessLevel mask;

  @ManyToMany
  @JoinTable(
      name = "ga4ghvisa",
      joinColumns = @JoinColumn(name = "aclp_id", referencedColumnName = "visaId"),
      inverseJoinColumns = @JoinColumn(name = "visa_id", referencedColumnName = "id"))
  private Collection<Visa> visas;

  @Override
  public Visa getOwner() {
    return null;
  }

  @Override
  public void setOwner(Visa owner) {}
}
