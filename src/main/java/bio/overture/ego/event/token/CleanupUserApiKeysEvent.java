/*
 * Copyright (c) 2019. The Ontario Institute for Cancer Research. All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package bio.overture.ego.event.token;

import bio.overture.ego.model.entity.User;
import java.util.Set;
import lombok.Getter;
import lombok.NonNull;
import org.springframework.context.ApplicationEvent;

public class CleanupUserApiKeysEvent extends ApplicationEvent {

  @Getter private Set<User> users;

  public CleanupUserApiKeysEvent(@NonNull Object source, Set<User> users) {
    super(source);
    this.users = users;
  }
}
