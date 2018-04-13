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

package org.overture.ego.token.signer;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import sun.misc.BASE64Encoder;

import javax.annotation.PostConstruct;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Optional;

@Slf4j
@Service
@Profile("!jks")
public class DefaultTokenSigner implements TokenSigner {


  /*
  Constants
 */
  private static final String KEYFACTORY_TYPE= "RSA";
  /*
    Dependencies
   */
  @Value("${token.private-key}")
  private String encodedPrivKey;

  @Value("${token.public-key}")
  private String encodedPubKey;

  /*
  Variables
  */
  private KeyFactory keyFactory;
  private PrivateKey privateKey;
  private PublicKey publicKey;


  @PostConstruct
  @SneakyThrows
  private void init(){
    keyFactory = KeyFactory.getInstance(KEYFACTORY_TYPE);
    try {
      val decodedpriv = Base64.getDecoder().decode(encodedPrivKey);
      val decodedPub =  Base64.getDecoder().decode(encodedPubKey);
      X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(decodedPub);
      PKCS8EncodedKeySpec privKeySpec = new PKCS8EncodedKeySpec(decodedpriv);
      publicKey = keyFactory.generatePublic(pubKeySpec);
      privateKey = keyFactory.generatePrivate(privKeySpec);
    } catch (InvalidKeySpecException specEx){
      log.error("Error loading keys:{}", specEx);
    }
  }
  @Override
  public Optional<Key> getKey() {
    return Optional.of(privateKey);
  }

  @Override
  public Optional<KeyPair> getKeyPair() {
    return Optional.of(new KeyPair(publicKey, privateKey));
  }

  @Override
  public Optional<String> getEncodedPublicKey() {
    if(publicKey != null){
      val b64 = new BASE64Encoder();
      String encodedKey = b64.encodeBuffer(publicKey.getEncoded());
      encodedKey= "-----BEGIN PUBLIC KEY-----\r\n" + encodedKey + "-----END PUBLIC KEY-----";
      return Optional.of(encodedKey);
    } else {
      return Optional.empty();
    }

  }
}
