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
import bio.overture.ego.model.enums.Tables;
import bio.overture.ego.view.Views;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonView;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import javax.persistence.*;
import javax.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = Tables.GROUP)
@JsonView(Views.REST.class)
@EqualsAndHashCode(of = {"id"})
@ToString(exclude = {"users", "applications", "permissions"})
@JsonPropertyOrder({"id", "name", "description", "status", "applications", "groupPermissions"})
@NamedEntityGraph(
        name = "group-entity-with-relationships",
        attributeNodes = {
                @NamedAttributeNode("id"),
                @NamedAttributeNode("name"),
                @NamedAttributeNode("description"),
                @NamedAttributeNode("status"),
                @NamedAttributeNode(value = "users", subgraph = "users-subgraph"),
                @NamedAttributeNode(value = "applications", subgraph = "relationship-subgraph"),
        },
        subgraphs = {
                @NamedSubgraph(
                        name = "relationship-subgraph",
                        attributeNodes = {
                                @NamedAttributeNode("id")
                        }
                )
        }
)
public class Group implements PolicyOwner, Identifiable<UUID> {

  @Id
  @GeneratedValue(generator = "group_uuid")
  @Column(nullable = false, name = Fields.ID, updatable = false)
  @GenericGenerator(name = "group_uuid", strategy = "org.hibernate.id.UUIDGenerator")
  private UUID id;

  @NotNull
  @Column(name = Fields.NAME)
  private String name;

  @Column(name = Fields.DESCRIPTION)
  private String description;

  @NotNull
  @Column(name = Fields.STATUS)
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
