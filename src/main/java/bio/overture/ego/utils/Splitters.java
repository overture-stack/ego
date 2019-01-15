package bio.overture.ego.utils;

import com.google.common.base.Splitter;
import lombok.NoArgsConstructor;

import static lombok.AccessLevel.PRIVATE;

@NoArgsConstructor(access = PRIVATE)
public class Splitters {

  public static final Splitter COMMA_SPLITTER = Splitter.on(',');

}
