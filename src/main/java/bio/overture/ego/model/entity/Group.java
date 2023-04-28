/*
 * Copyright (c) 2018. The Ontario Institute for Cancer Research. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package bio.overture.ego.model.entity;

import static com.google.common.collect.Sets.newHashSet;

import bio.overture.ego.model.enums.JavaFields;
import bio.overture.ego.model.enums.SqlFields;
import bio.overture.ego.model.enums.StatusType;
import bio.overture.ego.model.enums.Tables;
import bio.overture.ego.model.join.GroupApplication;
import bio.overture.ego.model.join.UserGroup;
import bio.overture.ego.view.Views;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonView;
import com.vladmihalcea.hibernate.type.basic.PostgreSQLEnumType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = Tables.GROUP)
@JsonView(Views.REST.class)
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"userGroups", "groupApplications", "permissions"})
@JsonPropertyOrder({
  JavaFields.ID,
  JavaFields.NAME,
  JavaFields.DESCRIPTION,
  JavaFields.STATUS,
  JavaFields.GROUPAPPLICATIONS,
  JavaFields.GROUPPERMISSIONS
})
public class Group implements PolicyOwner, NameableEntity<UUID> {

  @Id
  @GeneratedValue(generator = "group_uuid")
  @Column(name = SqlFields.ID, updatable = false, nullable = false)
  @GenericGenerator(name = "group_uuid", strategy = "org.hibernate.id.UUIDGenerator")
  private UUID id;

  @NotNull
  @Column(name = SqlFields.NAME, nullable = false, unique = true)
  private String name;

  @Column(name = SqlFields.DESCRIPTION)
  private String description;

  @NotNull
  @Type(PostgreSQLEnumType.class)
  @Enumerated(EnumType.STRING)
  @Column(name = SqlFields.STATUS, nullable = false)
  private StatusType status;

  // TODO: [rtisma] rename this to groupPermissions.
  // Ensure anything using JavaFields.PERMISSIONS is also replaced with JavaFields.GROUPPERMISSIONS
  @JsonIgnore
  @Builder.Default
  @OneToMany(
      mappedBy = JavaFields.OWNER,
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.LAZY)
  private Set<GroupPermission> permissions = newHashSet();

  @JsonIgnore
  @Builder.Default
  @OneToMany(
      mappedBy = JavaFields.GROUP,
      cascade = CascadeType.ALL,
      fetch = FetchType.LAZY,
      orphanRemoval = true)
  private Set<GroupApplication> groupApplications = newHashSet();

  @JsonIgnore
  @Builder.Default
  @OneToMany(
      mappedBy = JavaFields.GROUP,
      cascade = CascadeType.ALL,
      fetch = FetchType.LAZY,
      orphanRemoval = true)
  private Set<UserGroup> userGroups = newHashSet();
}
