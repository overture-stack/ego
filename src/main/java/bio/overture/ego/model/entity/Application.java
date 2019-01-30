/*
 * Copyright (c) 2017. The Ontario Institute for Cancer Research. All rights reserved.
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

import static bio.overture.ego.utils.Collectors.toImmutableList;
import static com.google.common.collect.Sets.newHashSet;

import bio.overture.ego.model.enums.*;
import bio.overture.ego.view.Views;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonView;
import com.vladmihalcea.hibernate.type.basic.PostgreSQLEnumType;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.persistence.*;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

@Entity
@Table(name = Tables.APPLICATION)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@JsonView(Views.REST.class)
@ToString(exclude = {LombokFields.groups, LombokFields.users})
@EqualsAndHashCode(of = {LombokFields.id})
@JsonPropertyOrder({
  JavaFields.ID,
  JavaFields.NAME,
  JavaFields.CLIENTID,
  JavaFields.CLIENTSECRET,
  JavaFields.REDIRECTURI,
  JavaFields.DESCRIPTION,
  JavaFields.STATUS
})
@TypeDef(name = "ego_role_enum", typeClass = PostgreSQLEnumType.class)
@JsonInclude(JsonInclude.Include.CUSTOM)
@NamedEntityGraph(
    name = "application-entity-with-relationships",
    attributeNodes = {
      @NamedAttributeNode(value = JavaFields.USERS, subgraph = "users-subgraph"),
      @NamedAttributeNode(value = JavaFields.TOKENS, subgraph = "tokens-subgraph"),
      @NamedAttributeNode(value = JavaFields.GROUPS, subgraph = "groups-subgraph")
    },
    subgraphs = {
      @NamedSubgraph(
          name = "groups-subgraph",
          attributeNodes = {@NamedAttributeNode(JavaFields.APPLICATIONS)}),
      @NamedSubgraph(
          name = "tokens-subgraph",
          attributeNodes = {@NamedAttributeNode(JavaFields.APPLICATIONS)}),
      @NamedSubgraph(
          name = "users-subgraph",
          attributeNodes = {@NamedAttributeNode(JavaFields.APPLICATIONS)})
    })
public class Application implements Identifiable<UUID> {

  @Id
  @Column(name = SqlFields.ID, updatable = false, nullable = false)
  @GenericGenerator(name = "application_uuid", strategy = "org.hibernate.id.UUIDGenerator")
  @GeneratedValue(generator = "application_uuid")
  private UUID id;

  @NotNull
  @JsonView({Views.JWTAccessToken.class, Views.REST.class})
  @Column(name = SqlFields.NAME, nullable = false)
  private String name;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Type(type = "ego_role_enum")
  @Column(name = SqlFields.ROLE, nullable = false)
  private UserRole role;

  @NotNull
  @JsonView({Views.JWTAccessToken.class, Views.REST.class})
  @Column(name = SqlFields.CLIENTID, nullable = false, unique = true)
  private String clientId;

  @NotNull
  @Column(name = SqlFields.CLIENTSECRET, nullable = false)
  private String clientSecret;

  @JsonView({Views.JWTAccessToken.class, Views.REST.class})
  @Column(name = SqlFields.REDIRECTURI)
  private String redirectUri;

  @JsonView({Views.JWTAccessToken.class, Views.REST.class})
  @Column(name = SqlFields.DESCRIPTION)
  private String description;

  // TODO: [rtisma] replace with Enum similar to AccessLevel
  @NotNull
  @JsonView(Views.JWTAccessToken.class)
  @Column(name = SqlFields.STATUS, nullable = false)
  private String status;

  @JsonIgnore
  @Builder.Default
  @ManyToMany(
      mappedBy = JavaFields.APPLICATIONS,
      fetch = FetchType.LAZY,
      cascade = {CascadeType.PERSIST, CascadeType.MERGE})
  private Set<Group> groups = newHashSet();

  @JsonIgnore
  @Builder.Default
  @ManyToMany(
      mappedBy = JavaFields.APPLICATIONS,
      fetch = FetchType.LAZY,
      cascade = {CascadeType.PERSIST, CascadeType.MERGE})
  private Set<User> users = newHashSet();

  @JsonIgnore
  @Builder.Default
  @ManyToMany(
      mappedBy = JavaFields.APPLICATIONS,
      fetch = FetchType.LAZY,
      cascade = {CascadeType.PERSIST, CascadeType.MERGE})
  private Set<Token> tokens = newHashSet();

  @JsonView(Views.JWTAccessToken.class)
  public List<String> getGroupNames() {
    return getGroups().stream().map(Group::getName).collect(toImmutableList());
  }
}
