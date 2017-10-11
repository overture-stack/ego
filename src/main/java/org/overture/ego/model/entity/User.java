package org.overture.ego.model.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.*;

import java.util.Date;
import java.util.List;

@Builder
@Data
@JsonPropertyOrder({ "id", "userName", "email", "role", "status", "groups",
        "applications", "firstName", "lastName", "createdAt", "lastLogin", "preferredLanguage"})
@JsonInclude(JsonInclude.Include.ALWAYS)
public class User {

    String id;
    @NonNull String userName; // TODO: not sure
    @NonNull String email;
    @NonNull String role; // TODO: not sure
    String status;
    String firstName;
    String lastName;
    String createdAt;
    String lastLogin;
    String preferredLanguage;
}
