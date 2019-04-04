package bio.overture.ego.utils;

import com.google.common.base.Joiner;
import lombok.NoArgsConstructor;

import static lombok.AccessLevel.PRIVATE;

@NoArgsConstructor(access = PRIVATE)
public class Joiners {

  public static final Joiner COMMA = Joiner.on(",");
  public static final Joiner NEWLINE_COMMA = Joiner.on(",\n");
  public static final Joiner PRETTY_COMMA = Joiner.on(" , ");
  public static final Joiner PATH = Joiner.on("/");
  public static final Joiner AMPERSAND = Joiner.on("&");
}
