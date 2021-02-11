package bio.overture.ego.utils;

import static lombok.AccessLevel.PRIVATE;

import com.google.common.base.Joiner;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = PRIVATE)
public class Joiners {

  public static final Joiner COMMA = Joiner.on(",");
  public static final Joiner NEWLINE_COMMA = Joiner.on(",\n");
  public static final Joiner PRETTY_COMMA = Joiner.on(" , ");
  public static final Joiner PATH = Joiner.on("/");
  public static final Joiner AMPERSAND = Joiner.on("&");
  public static final Joiner BLANK = Joiner.on(" ");
}
