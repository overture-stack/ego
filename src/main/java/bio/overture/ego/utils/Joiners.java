package bio.overture.ego.utils;

import com.google.common.base.Joiner;
import lombok.NoArgsConstructor;

import static lombok.AccessLevel.PRIVATE;

@NoArgsConstructor(access = PRIVATE)
public class Joiners {

  public static final Joiner COMMA = Joiner.on(",");

}
