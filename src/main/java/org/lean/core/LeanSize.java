package org.lean.core;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.hop.metadata.api.HopMetadataProperty;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class LeanSize {

  @HopMetadataProperty private int width;
  @HopMetadataProperty private int height;

  public LeanSize(int width, int height) {
    this.width = width;
    this.height = height;
  }

  public LeanSize(LeanSize size) {
    this(size.width, size.height);
  }

  @Override
  public String toString() {
    return "LeanSize(" + width + "x" + height + ")";
  }

  @JsonIgnore
  public boolean isDefined() {
    return width > 0 && height > 0;
  }
}
