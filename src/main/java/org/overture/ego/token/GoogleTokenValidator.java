package org.overture.ego.token;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
public class GoogleTokenValidator {


    HttpTransport transport;
    JsonFactory jsonFactory;
    GoogleIdTokenVerifier verifier;
    @Value("${google.client.Ids}")
    private String clientIDs;

    public GoogleTokenValidator(){
        transport = new NetHttpTransport();
        jsonFactory = new JacksonFactory();
    }


    public boolean validToken(String token) {
        if(verifier == null)
            initVerifier();
        GoogleIdToken idToken = null;
        try {
             idToken = verifier.verify(token);
        } catch (GeneralSecurityException gEX){
            log.error("Error while verifying google token: {}", gEX);
        } catch (IOException ioEX){
            log.error("Error while verifying google token: {}", ioEX);
        } catch (Exception ex){
            log.error("Error while verifying google token: {}", ex);
        }

        return (idToken != null);
    }

    private void initVerifier() {
        List<String> targetAudience;
        if(clientIDs.contains(","))
            targetAudience = Arrays.asList(clientIDs.split(","));
        else {
            targetAudience = new ArrayList<String>();
            targetAudience.add(clientIDs);
        }
        verifier =
                new GoogleIdTokenVerifier.Builder(transport, jsonFactory)
                        .setAudience(targetAudience)
                        .build();
    }
}
