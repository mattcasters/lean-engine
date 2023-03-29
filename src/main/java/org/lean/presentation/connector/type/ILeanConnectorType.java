package org.lean.presentation.connector.type;

import org.apache.hop.core.gui.plugin.ITypeMetadata;
import org.apache.hop.metadata.api.IHopMetadata;
import org.lean.presentation.connector.LeanConnector;

public class ILeanConnectorType implements ITypeMetadata {
  @Override
  public Class<? extends IHopMetadata> getMetadataClass() {
    return LeanConnector.class;
  }
}
