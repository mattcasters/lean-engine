package org.lean.presentation.theme;

import org.apache.hop.core.gui.plugin.ITypeMetadata;
import org.apache.hop.metadata.api.IHopMetadata;

public class ILeanThemeType implements ITypeMetadata {
  @Override
  public Class<? extends IHopMetadata> getMetadataClass() {
    return LeanTheme.class;
  }
}
