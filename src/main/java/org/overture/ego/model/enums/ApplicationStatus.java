package org.overture.ego.model.enums;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum ApplicationStatus {
  APPROVED("Approved"),
  DISABLED("Disabled"),
  PENDING("Pending"),
  REJECTED("Rejected"),;

  @NonNull
  private final String value;

  @Override
  public String toString() {
    return value;
  }
}
