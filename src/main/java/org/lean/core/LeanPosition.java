package org.lean.core;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.hop.metadata.api.HopMetadataProperty;

@Getter
@Setter
@NoArgsConstructor
public class LeanPosition {

  @HopMetadataProperty private int x;
  @HopMetadataProperty private int y;

  public LeanPosition(int x, int y) {
    this.x = x;
    this.y = y;
  }

  @Override
  public String toString() {
    return "LeanPosition(" + x + "," + y + ")";
  }
}
