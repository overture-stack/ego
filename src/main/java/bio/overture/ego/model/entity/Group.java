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

import static bio.overture.ego.model.enums.AccessLevel.EGO_ENUM;
import static com.google.common.collect.Sets.newHashSet;

import bio.overture.ego.model.enums.JavaFields;
import bio.overture.ego.model.enums.LombokFields;
import bio.overture.ego.model.enums.SqlFields;
import bio.overture.ego.model.enums.StatusType;
import bio.overture.ego.model.enums.Tables;
import bio.overture.ego.view.Views;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonView;
import com.vladmihalcea.hibernate.type.basic.PostgreSQLEnumType;
import java.util.Set;
import java.util.UUID;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.NamedAttributeNode;
import javax.persistence.NamedEntityGraph;
import javax.persistence.NamedSubgraph;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = Tables.GROUP)
@JsonView(Views.REST.class)
@EqualsAndHashCode(of = {LombokFields.id})
@TypeDef(name = EGO_ENUM, typeClass = PostgreSQLEnumType.class)
@ToString(exclude = {LombokFields.users, LombokFields.applications, LombokFields.permissions})
@JsonPropertyOrder({
  JavaFields.ID,
  JavaFields.NAME,
  JavaFields.DESCRIPTION,
  JavaFields.STATUS,
  JavaFields.APPLICATIONS,
  JavaFields.GROUPPERMISSIONS
})
@NamedEntityGraph(
    name = "group-entity-with-relationships",
    attributeNodes = {
      @NamedAttributeNode(value = JavaFields.USERS, subgraph = "users-subgraph"),
      @NamedAttributeNode(value = JavaFields.PERMISSIONS, subgraph = "permissions-subgraph"),
      @NamedAttributeNode(value = JavaFields.APPLICATIONS, subgraph = "applications-subgraph")
    },
    subgraphs = {
      @NamedSubgraph(
          name = "permissions-subgraph",
          attributeNodes = {@NamedAttributeNode(JavaFields.POLICY)}),
      @NamedSubgraph(
          name = "applications-subgraph",
          attributeNodes = {@NamedAttributeNode(JavaFields.GROUPS)}),
      @NamedSubgraph(
          name = "users-subgraph",
          attributeNodes = {@NamedAttributeNode(JavaFields.GROUPS)})
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
  @Type(type = EGO_ENUM)
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

  @ManyToMany(
      fetch = FetchType.LAZY,
      cascade = {CascadeType.PERSIST, CascadeType.MERGE})
  @JoinTable(
      name = Tables.GROUP_APPLICATION,
      joinColumns = {@JoinColumn(name = SqlFields.GROUPID_JOIN)},
      inverseJoinColumns = {@JoinColumn(name = SqlFields.APPID_JOIN)})
  @JsonIgnore
  @Builder.Default
  private Set<Application> applications = newHashSet();

  @ManyToMany(
      fetch = FetchType.LAZY,
      cascade = {CascadeType.PERSIST, CascadeType.MERGE})
  @JoinTable(
      name = Tables.GROUP_USER,
      joinColumns = {@JoinColumn(name = SqlFields.GROUPID_JOIN)},
      inverseJoinColumns = {@JoinColumn(name = SqlFields.USERID_JOIN)})
  @JsonIgnore
  @Builder.Default
  private Set<User> users = newHashSet();
}
