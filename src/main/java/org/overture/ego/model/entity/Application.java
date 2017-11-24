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

package org.overture.ego.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.*;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

import javax.persistence.*;
import java.util.HashSet;
import java.util.List;

@Entity
@Table(name = "egoapplication")
@Data
@ToString(exclude={"groups","users"})
@JsonPropertyOrder({"id", "name", "clientId", "clientSecret", "redirectUri", "description", "status"})
@JsonInclude(JsonInclude.Include.ALWAYS)
@EqualsAndHashCode(of={"id"})
@NoArgsConstructor
@RequiredArgsConstructor
public class Application {

  @Id
  @Column(nullable = false, name = "id", updatable = false)
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  int id;

  @NonNull
  @Column(nullable = false, name = "name")
  String name;

  @NonNull
  @Column(nullable = false, name = "clientid")
  String clientId;

  @NonNull
  @Column(nullable = false, name = "clientsecret")
  String clientSecret;

  @Column(name = "redirecturi")
  String redirectUri;

  @Column(name = "description")
  String description;

  @Column(name = "status")
  String status;

  @ManyToMany(mappedBy = "applications", cascade = CascadeType.ALL)
  @LazyCollection(LazyCollectionOption.FALSE)
  @JsonIgnore
  List<Group> groups;

  @ManyToMany(mappedBy = "applications", cascade = CascadeType.ALL)
  @LazyCollection(LazyCollectionOption.FALSE)
  @JsonIgnore
  List<User> users;

  @JsonIgnore
  public HashSet<String> getURISet(){
    val output = new HashSet<String>();
    output.add(this.redirectUri);
    return output;
  }
}
