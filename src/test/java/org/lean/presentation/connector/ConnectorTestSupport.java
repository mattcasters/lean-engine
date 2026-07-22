package org.lean.presentation.connector;

import java.util.List;
import org.apache.hop.core.RowMetaAndData;
import org.apache.hop.metadata.api.IHopMetadataProvider;
import org.apache.hop.metadata.serializer.memory.MemoryMetadataProvider;
import org.lean.core.LeanEnvironment;
import org.lean.core.exception.LeanException;
import org.lean.presentation.LeanPresentation;
import org.lean.presentation.connector.type.ILeanConnector;
import org.lean.presentation.datacontext.PresentationDataContext;

/** Shared helpers for connector streaming tests. */
public final class ConnectorTestSupport {

  private ConnectorTestSupport() {}

  public static void initEnvironment() throws LeanException {
    LeanEnvironment.init();
  }

  public static PresentationDataContext dataContext(LeanConnector... connectors) {
    LeanPresentation presentation = new LeanPresentation();
    presentation.setName("connector-test");
    presentation.setDescription("unit test presentation");
    for (LeanConnector connector : connectors) {
      presentation.getConnectors().add(connector);
    }
    IHopMetadataProvider metadataProvider = new MemoryMetadataProvider();
    return new PresentationDataContext(presentation, metadataProvider);
  }

  public static LeanConnector wrap(String name, ILeanConnector connector) {
    return new LeanConnector(name, connector);
  }

  public static List<RowMetaAndData> retrieve(LeanConnector connector, PresentationDataContext ctx)
      throws LeanException {
    return connector.retrieveRows(ctx);
  }
}
