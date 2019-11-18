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

import bio.overture.ego.model.entity.ApiKey;
import bio.overture.ego.model.entity.User;
import java.util.Set;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class ApiKeyEventsPublisher {

  private ApplicationEventPublisher applicationEventPublisher;

  @Autowired
  public ApiKeyEventsPublisher(ApplicationEventPublisher applicationEventPublisher) {
    this.applicationEventPublisher = applicationEventPublisher;
  }

  public void requestApiKeyCleanupByUsers(@NonNull final Set<User> users) {
    applicationEventPublisher.publishEvent(new CleanupUserApiKeysEvent(this, users));
  }

  public void requestApiKeyCleanup(@NonNull final Set<ApiKey> apiKeys) {
    applicationEventPublisher.publishEvent(new RevokeApiKeysEvent(this, apiKeys));
  }
}
