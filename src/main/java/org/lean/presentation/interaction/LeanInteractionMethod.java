package org.lean.presentation.interaction;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.hop.metadata.api.HopMetadataProperty;

/**
 * A Lean interaction method describes the way a user can interact with any part of a presentation.
 */
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class LeanInteractionMethod {

  public static final LeanInteractionMethod SingleClick = new LeanInteractionMethod(true, false);
  public static final LeanInteractionMethod DoubleClick = new LeanInteractionMethod(false, true);

  @HopMetadataProperty private boolean mouseClick;

  @HopMetadataProperty private boolean mouseDoubleClick;

  public LeanInteractionMethod(boolean mouseClick, boolean mouseDoubleClick) {
    this.mouseClick = mouseClick;
    this.mouseDoubleClick = mouseDoubleClick;
  }

  public LeanInteractionMethod(LeanInteractionMethod method) {
    this.mouseClick = method.mouseClick;
    this.mouseDoubleClick = method.mouseDoubleClick;
  }
}
