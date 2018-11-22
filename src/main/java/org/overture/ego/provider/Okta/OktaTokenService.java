package org.overture.ego.provider.Okta;

import com.okta.jwt.Jwt;
import com.okta.jwt.JwtHelper;
import lombok.SneakyThrows;
import lombok.val;
import org.overture.ego.token.IDToken;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OktaTokenService {

    @Value("${okta.clientId}")
    private String clientId;
    @Value("${okta.issuerUrl}")
    private String issuerUrl;
    @Value("${okta.audience}")
    private String audience;

    @SneakyThrows
    public IDToken verify(String jwtString) {
        val jwtVerifier = new JwtHelper()
                .setIssuerUrl(issuerUrl)
                .setAudience(audience)
                .setConnectionTimeout(2000)
                .setReadTimeout(2000)
                .setClientId(clientId)
                .build();

        val jwt = jwtVerifier.decodeIdToken(jwtString, null);
        val claims = jwt.getClaims();
        return IDToken.builder().email(claims.get("email").toString()).build();
    }

}