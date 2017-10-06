package org.overture.ego.model.entity;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.Singular;

import java.util.List;

@Data
@Builder
public class Application {
    String id;
    @NonNull String applicationName;
    @NonNull String clientId;
    @NonNull String clientSecret;
    String redirectUri;
    String description;
    String status;

}
