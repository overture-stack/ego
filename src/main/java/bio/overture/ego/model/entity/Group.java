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

import bio.overture.ego.model.enums.Fields;
import bio.overture.ego.model.enums.JavaFields;
import bio.overture.ego.model.enums.LombokFields;
import bio.overture.ego.model.enums.SqlFields;
import bio.overture.ego.model.enums.Tables;
import bio.overture.ego.view.Views;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
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
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = Tables.GROUP)
@JsonView(Views.REST.class)
@EqualsAndHashCode(of = {LombokFields.id})
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
      @NamedAttributeNode(value = "users", subgraph = "users-subgraph"),
      @NamedAttributeNode(value = "applications", subgraph = "applications-subgraph"),
    },
    subgraphs = {
        @NamedSubgraph( name = "applications-subgraph", attributeNodes = {@NamedAttributeNode("groups")}),
        @NamedSubgraph( name = "users-subgraph", attributeNodes = {@NamedAttributeNode("groups")})
    })
public class Group implements PolicyOwner, Identifiable<UUID> {

  @Id
  @GeneratedValue(generator = "group_uuid")
  @Column(name = Fields.ID, updatable = false, nullable = false)
  @GenericGenerator(name = "group_uuid", strategy = "org.hibernate.id.UUIDGenerator")
  private UUID id;

  @NotNull
  @Column(name = SqlFields.NAME, nullable = false, unique = true)
  private String name;

  @Column(name = SqlFields.DESCRIPTION)
  private String description;

  // TODO: [rtisma] replace with Enum similar to AccessLevel
  @NotNull
  @Column(name = SqlFields.STATUS, nullable = false)
  private String status;

  @ManyToMany(
      fetch = FetchType.LAZY,
      cascade = {CascadeType.PERSIST, CascadeType.MERGE})
  @JoinTable(
      name = Tables.GROUP_APPLICATION,
      joinColumns = {@JoinColumn(name = Fields.GROUPID_JOIN)},
      inverseJoinColumns = {@JoinColumn(name = Fields.APPID_JOIN)})
  @JsonIgnore
  @Builder.Default
  private Set<Application> applications = new HashSet<>();

  @ManyToMany(
      fetch = FetchType.LAZY,
      cascade = {CascadeType.PERSIST, CascadeType.MERGE})
  @JoinTable(
      name = Tables.GROUP_USER,
      joinColumns = {@JoinColumn(name = Fields.GROUPID_JOIN)},
      inverseJoinColumns = {@JoinColumn(name = Fields.USERID_JOIN)})
  @JsonIgnore
  @Builder.Default
  private Set<User> users = new HashSet<>();

  @JsonIgnore
  @Builder.Default
  @JoinColumn(name = Fields.OWNER)
  @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
  private Set<Policy> policies = new HashSet<>();

  @JsonIgnore
  @Builder.Default
  @JoinColumn(name = Fields.GROUPID_JOIN)
  @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
  private Set<GroupPermission> permissions = new HashSet<>();
}
