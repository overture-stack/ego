package bio.overture.ego.model.enums;

import lombok.NoArgsConstructor;

import static lombok.AccessLevel.PRIVATE;

/**
 * Note: When using Lombok annoation with field names (for example @EqualsAndHashCode(ids = {LombokFields.id})
 * lombok does not look at the variable's value, but instead takes the variables name as the value.
 * https://github.com/rzwitserloot/lombok/issues/1094
 */
@NoArgsConstructor(access = PRIVATE)
public class LombokFields {

  public static final String id = "doesn't matter, lombok doesnt use this string";

}
