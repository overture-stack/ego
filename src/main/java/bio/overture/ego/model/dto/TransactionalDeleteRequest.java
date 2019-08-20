package bio.overture.ego.model.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
/** * A request to delete all of the given groups and policies in a single transaction. */
public class TransactionalDeleteRequest {
  List<String> groupNames;
  List<String> policyNames;
}
