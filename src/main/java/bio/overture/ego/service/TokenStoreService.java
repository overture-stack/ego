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

import static bio.overture.ego.model.exceptions.NotFoundException.checkNotFound;

import bio.overture.ego.model.dto.CreateTokenRequest;
import bio.overture.ego.model.entity.Token;
import bio.overture.ego.repository.TokenStoreRepository;
import bio.overture.ego.repository.queryspecification.builder.TokenSpecificationBuilder;
import java.util.Optional;
import java.util.UUID;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
public class TokenStoreService extends AbstractNamedService<Token, UUID> {

  /** Dependencies */
  private final TokenStoreRepository tokenRepository;

  @Autowired
  public TokenStoreService(@NonNull TokenStoreRepository repository) {
    super(Token.class, repository);
    this.tokenRepository = repository;
  }

  @Override
  public Token getWithRelationships(@NonNull UUID id) {
    return get(id, true, true);
  }

  @SuppressWarnings("unchecked")
  public Token get(@NonNull UUID id, boolean fetchOwner, boolean fetchTokenScopes) {
    val result =
        (Optional<Token>)
            getRepository()
                .findOne(
                    new TokenSpecificationBuilder()
                        .fetchOwner(fetchOwner)
                        .fetchTokenScopes(fetchTokenScopes)
                        .buildById(id));
    checkNotFound(result.isPresent(), "The tokenId '%s' does not exist", id);
    return result.get();
  }

  public Token create(@NonNull CreateTokenRequest createTokenRequest) {
    throw new NotImplementedException();
  }

  @Deprecated
  public Token create(@NonNull Token scopedAccessToken) {
    Token res = tokenRepository.save(scopedAccessToken);
    tokenRepository.revokeRedundantTokens(scopedAccessToken.getOwner().getId());
    return res;
  }

  public Optional<Token> findByTokenName(String tokenName) {
    return tokenRepository.findByName(tokenName);
  }
}
