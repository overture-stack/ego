package bio.overture.ego.model.enums;

import bio.overture.ego.model.exceptions.ForbiddenException;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum IdProviderType {
  GOOGLE,
  FACEBOOK,
  LINKEDIN,
  GITHUB,
  ORCID;

  // TODO: verify these are the correct accessor keys for each provider
  public static String getIdAccessor(IdProviderType provider) {
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

    throw new ForbiddenException(
        String.format("Provider '%s' is not a valid providerType", provider));
  }

  @Override
  public String toString() {
    return this.name();
  }
}
