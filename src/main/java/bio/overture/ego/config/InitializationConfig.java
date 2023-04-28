package bio.overture.ego.config;

import bio.overture.ego.model.enums.ApplicationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Setter
@Getter
@Component
@ConfigurationProperties("initialization")
public class InitializationConfig {

  private boolean enabled;
  private final List<InitialApplication> applications = new ArrayList<>();

  @Getter
  @Setter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class InitialApplication {
    @NotBlank private String name;
    @NotNull private ApplicationType type;

    @NotBlank
    @Size(min = 3)
    private String clientId;

    @NotBlank
    @Size(min = 15)
    private String clientSecret;

    private String redirectUri;
    private String description;
  }
}
