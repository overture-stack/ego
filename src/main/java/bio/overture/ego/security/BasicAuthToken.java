package bio.overture.ego.security;

import static bio.overture.ego.utils.Splitters.COLON_SPLITTER;
import static java.lang.String.format;

import java.util.Base64;
import java.util.Optional;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

@Slf4j
@Data
public class BasicAuthToken {
  private final String clientId;
  private final String clientSecret;

  public static final String TOKEN_PREFIX = "Basic ";

  public static Optional<BasicAuthToken> decode(String token) {
    log.debug("Decoding basic auth token: '" + token + "'");
    val base64encoding = removeTokenPrefix(token);
    String contents;
    try {
      contents = new String(Base64.getDecoder().decode(base64encoding));
    } catch (Exception exception) {
      log.error("Couldn't decode basic auth token: '" + token + "', " + exception.getMessage());
      return Optional.empty();
    }
    val parts = COLON_SPLITTER.splitToList(contents);
    if (parts.size() != 2) {
      log.error("Basic auth token '" + token + "' should have 2 parts, not " + parts.size());
      return Optional.empty();
    }

    val clientId = parts.get(0);
    val clientSecret = parts.get(1);

    log.info(format("Extracted client id '%s'", clientId));
    return Optional.of(new BasicAuthToken(clientId, clientSecret));
  }

  private static String removeTokenPrefix(String token) {
    return token.replace(TOKEN_PREFIX, "").trim();
  }
}
