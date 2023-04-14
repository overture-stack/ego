package bio.overture.ego.model.entity;

import bio.overture.ego.model.enums.JavaFields;
import bio.overture.ego.model.enums.SqlFields;
import bio.overture.ego.model.enums.Tables;
import bio.overture.ego.view.Views;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonView;
import java.util.UUID;
import javax.persistence.*;
import javax.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldNameConstants;
import org.hibernate.annotations.GenericGenerator;

@Entity
@Table(name = Tables.GA4GHVISA)
@JsonInclude()
@JsonPropertyOrder({
  JavaFields.ID,
  JavaFields.TYPE,
  JavaFields.SOURCE,
  JavaFields.VALUE,
  JavaFields.BY
})
@JsonView(Views.REST.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants
@EqualsAndHashCode(of = {"id"})
@NamedEntityGraph(
    name = "policy-entity-with-relationships",
    attributeNodes = {
      @NamedAttributeNode(value = JavaFields.USERPERMISSIONS),
      @NamedAttributeNode(value = JavaFields.GROUPPERMISSIONS),
    })
public class Visa implements Identifiable<UUID> {

  @Id
  @Column(name = SqlFields.ID, updatable = false, nullable = false)
  @GenericGenerator(name = "visa_uuid", strategy = "org.hibernate.id.UUIDGenerator")
  @GeneratedValue(generator = "visa_uuid")
  private UUID id;

  @NotNull
  @Column(name = SqlFields.TYPE, nullable = false)
  @JsonView({Views.JWTAccessToken.class, Views.REST.class})
  private String type;

  @NotNull
  @JsonView({Views.JWTAccessToken.class, Views.REST.class})
  @Column(name = SqlFields.SOURCE)
  private String source;

  @NotNull
  @JsonView({Views.JWTAccessToken.class, Views.REST.class})
  @Column(name = SqlFields.VALUE)
  private String value;

  @NotNull
  @JsonView({Views.JWTAccessToken.class, Views.REST.class})
  @Column(name = SqlFields.BY)
  private String by;
}
