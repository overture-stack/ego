package org.overture.ego.model.entity;

import lombok.*;

import java.util.Date;
import java.util.List;

@Builder
@Data
public class User {

    String id;
    @NonNull String userName; // TODO: not sure
    @NonNull String email;
    @NonNull String role; // TODO: not sure
    @NonNull String status;
    @Singular List<String> groups;
    @Singular List<String> applications;
    String firstName;
    String lastName;
    Date createdAt;
    Date lastLogin;
    String preferredLanguage;
}
