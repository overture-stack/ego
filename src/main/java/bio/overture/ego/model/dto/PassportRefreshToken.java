package bio.overture.ego.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.val;

import java.util.Calendar;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PassportRefreshToken {
  private String iss;
  private String aud;
  private Long exp; // in seconds
  private String jti;

  public Long getSecondsUntilExpiry() {
    val seconds = this.exp - Calendar.getInstance().getTime().getTime() / 1000L;
    return seconds > 0 ? seconds : 0;
  }
}
