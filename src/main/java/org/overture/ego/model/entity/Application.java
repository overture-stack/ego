package org.overture.ego.model.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.Singular;

import java.util.List;

@Data
@Builder
@JsonPropertyOrder({ "id", "applicationName", "clientId", "clientSecret", "redirectUri", "description", "status" })
@JsonInclude(JsonInclude.Include.ALWAYS)
public class Application {
    String id;
    @NonNull String applicationName;
    @NonNull String clientId;
    @NonNull String clientSecret;
    String redirectUri;
    String description;
    String status;

}
