package org.overture.ego.token;

import java.security.Key;
import java.security.KeyPair;
import java.util.Optional;

public interface TokenSigner {

  Optional<Key> getKey();
  Optional<KeyPair> getKeyPair();
  Optional<String> getEncodedPublicKey();
}
