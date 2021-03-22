package bio.overture.ego.model.enums;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum LinkedinContactType {
  EMAIL,
  PHONE;

  @Override
  public String toString() {
    return this.name();
  }
}
