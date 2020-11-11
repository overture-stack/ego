package bio.overture.ego.model.enums;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum IdProviderType {
  GOOGLE,
  FACEBOOK,
  LINKEDIN,
  GITHUB,
  ORCID;

  public static String getIdAccessor(IdProviderType provider) {
    if (provider.equals(GOOGLE) || provider.equals(ORCID)) {
      return "sub";
    }

    if (provider.equals(FACEBOOK)) {
      return "user_id";
    }

    // from LinkedIn: "Each member id is unique to the context of your application only. Sharing a
    // person ID across applications will not work and result in a 404 error."
    if (provider.equals(LINKEDIN) || provider.equals(GITHUB)) {
      return "id";
    }

    // TODO: correct error in getIdAccessor if provider not found
    return null;
  }

  @Override
  public String toString() {
    return this.name();
  }
}
