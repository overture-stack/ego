package bio.overture.ego.model.enums;

import static bio.overture.ego.utils.Joiners.COMMA;
import static bio.overture.ego.utils.Streams.stream;
import static java.lang.String.format;

import bio.overture.ego.model.exceptions.ForbiddenException;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum ProviderType {
  GOOGLE,
  FACEBOOK,
  LINKEDIN,
  GITHUB,
  ORCID;

  // TODO: verify these are the correct accessor keys for each provider
  public static String getIdAccessor(ProviderType provider) {
    if (provider.equals(GOOGLE) || provider.equals(ORCID)) {
      return "sub";
    }

    if (provider.equals(FACEBOOK)) {
      return "user_id";
    }

    // from LinkedIn: "Each member id is unique to the context of your application only. Sharing a
    // person ID across applications will not work and result in a 404 error."
    // https://docs.microsoft.com/en-us/linkedin/shared/integrations/people/profile-api#person-id
    if (provider.equals(LINKEDIN) || provider.equals(GITHUB)) {
      return "id";
    }

    throw new ForbiddenException(format("Provider '%s' is not a valid providerType", provider));
  }

  public static ProviderType resolveProviderType(@NonNull String providerType) {
    return stream(values())
        .filter(x -> x.toString().equals(providerType))
        .findFirst()
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    format(
                        "The provider type '%s' cannot be resolved. Must be one of: [%s]",
                        providerType, COMMA.join(values()))));
  }

  @Override
  public String toString() {
    return this.name();
  }
}
