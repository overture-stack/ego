package bio.overture.ego.utils;

import static lombok.AccessLevel.PRIVATE;

import com.google.common.base.Splitter;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = PRIVATE)
public class Splitters {

  public static final Splitter COMMA_SPLITTER = Splitter.on(',');

}
