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

package bio.overture.ego.token.signer;

import jakarta.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.*;
import java.util.Base64;
import java.util.Optional;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Profile("jks")
public class JKSTokenSigner implements TokenSigner {

  /*
   Constants
  */
  private static final String KEYSTORE_TYPE = "JKS";
  /*
   Dependencies
  */
  @Value("${token.key-store}")
  private String keyStorePath;

  @Value("${token.keystore-password}")
  private String keyStorePwd;

  @Value("${token.key-alias}")
  private String keyalias;
  /*
  Variables
   */
  private KeyStore keyStore;

  @PostConstruct
  @SneakyThrows
  private void init() {
    keyStore = KeyStore.getInstance(KEYSTORE_TYPE);
    try (val keyStoreFile = new FileInputStream(keyStorePath)) {
      keyStore.load(keyStoreFile, keyStorePwd.toCharArray());
    } catch (IOException ioex) {
      log.error("Error loading keystore:{}", ioex);
    }
  }

  public Optional<Key> getKey() {
    try {
      return Optional.of(keyStore.getKey(keyalias, keyStorePwd.toCharArray()));
    } catch (Exception ex) {
      log.error("Error getting the key:{}", ex);
      return Optional.empty();
    }
  }

  public Optional<KeyPair> getKeyPair() {
    val key = this.getKey();
    val publicKey = this.getPublicKey();
    if (key.isPresent() && publicKey.isPresent()) {
      return Optional.of(new KeyPair(publicKey.get(), (PrivateKey) key.get()));
    } else {
      return Optional.empty();
    }
  }

  public Optional<PublicKey> getPublicKey() {
    try {
      val cert = keyStore.getCertificate(keyalias);
      val publicKey = cert.getPublicKey();
      return Optional.of(publicKey);
    } catch (Exception ex) {
      log.error("Error getting the public key:{}", ex);
      return Optional.empty();
    }
  }

  @SneakyThrows
  public Optional<String> getEncodedPublicKey() {
    val publicKey = this.getPublicKey();
    if (publicKey.isPresent()) {
      val b64 = Base64.getEncoder();
      String encodedKey = b64.encodeToString(publicKey.get().getEncoded());
      encodedKey = "-----BEGIN PUBLIC KEY-----\r\n" + encodedKey + "\r\n-----END PUBLIC KEY-----";
      return Optional.of(encodedKey);
    } else {
      return Optional.empty();
    }
  }
}
