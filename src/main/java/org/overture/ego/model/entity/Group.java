package org.overture.ego.model.entity;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.Singular;

import java.util.Date;
import java.util.List;

@Data
@Builder
/*
    Represents an organization that has access to applications within Kids First Portal
 */
public class Group {
    String id;
    @NonNull String groupName;
    String description;
    String status;
    @Singular List<String> roles;
    @Singular List<String> applications;

}
