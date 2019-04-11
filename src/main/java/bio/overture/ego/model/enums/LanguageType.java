package bio.overture.ego.model.enums;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum LanguageType {
  ENGLISH,
  FRENCH,
  SPANISH;

  @Override
  public String toString() {
    return this.name();
  }
}
