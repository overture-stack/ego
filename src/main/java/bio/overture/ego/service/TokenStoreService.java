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

package bio.overture.ego.service;

import bio.overture.ego.model.entity.Token;
import bio.overture.ego.repository.TokenStoreRepository;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@Transactional
public class TokenStoreService extends BaseServiceImpl<Token> {

  private final TokenStoreRepository tokenRepository;

  @Autowired
  public TokenStoreService(@NonNull TokenStoreRepository repository) {
    super(Token.class, repository);
    this.tokenRepository = repository;
  }

  public Token create(@NonNull Token scopedAccessToken) {
    return tokenRepository.save(scopedAccessToken);
  }

  public Optional<Token> findByTokenString(String token) {
    return tokenRepository.getTokenByTokenIgnoreCase(token);
  }
}
