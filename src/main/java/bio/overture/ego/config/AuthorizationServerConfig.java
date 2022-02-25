package bio.overture.ego.config;

import bio.overture.ego.token.signer.TokenSigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.authorization.config.ProviderSettings;

@Configuration
public class AuthorizationServerConfig {

  // Soruce of this method :
  // https://github.com/spring-projects/spring-security-samples/blob/main/servlet/spring-boot/java/oauth2/authorization-server/src/main/java/example/OAuth2AuthorizationServerSecurityConfiguration.java
  @Bean
  public JWKSource<SecurityContext> jwkSource(@Autowired TokenSigner tokenSigner) {
    val keyPair =
        tokenSigner.getKeyPair().orElseThrow(() -> new RuntimeException("no key pair found"));
    RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
    RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
    // @formatter:off
    RSAKey rsaKey =
        new RSAKey.Builder(publicKey)
            .privateKey(privateKey)
            .keyID(UUID.randomUUID().toString())
            .build();
    // @formatter:on
    JWKSet jwkSet = new JWKSet(rsaKey);
    return new ImmutableJWKSet<>(jwkSet);
  }

  @Bean
  public JwtDecoder jwtDecoder(@Autowired TokenSigner tokenSigner) {
    val keyPair =
        tokenSigner.getKeyPair().orElseThrow(() -> new RuntimeException("no key pair found"));
    return NimbusJwtDecoder.withPublicKey((RSAPublicKey) keyPair.getPublic()).build();
  }

  @Bean
  public ProviderSettings providerSettings(@Value("${token.issuer}") String issuer) {
    return ProviderSettings.builder().tokenEndpoint("/oauth/token").issuer(issuer).build();
  }
}
