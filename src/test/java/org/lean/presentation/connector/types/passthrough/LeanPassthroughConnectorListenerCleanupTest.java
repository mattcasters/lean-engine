package org.lean.presentation.connector.types.passthrough;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import org.apache.hop.core.RowMetaAndData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lean.presentation.connector.ConnectorTestSupport;
import org.lean.presentation.connector.LeanConnector;
import org.lean.presentation.connector.types.list.LeanListConnector;
import org.lean.presentation.datacontext.PresentationDataContext;

/**
 * Verifies transform connectors detach from the source after {@code waitUntilFinished}, so a
 * reused source instance does not accumulate listeners across runs.
 */
class LeanPassthroughConnectorListenerCleanupTest {

  @BeforeEach
  void setUp() throws Exception {
    ConnectorTestSupport.initEnvironment();
  }

  @Test
  void secondRunDoesNotStackListenersOnSharedSource() throws Exception {
    LeanListConnector list = new LeanListConnector("v", Arrays.asList("a", "b"));

    // First pass
    assertEquals(2, runPassthrough(list).size());
    assertEquals(0, list.getRowListeners().size(), "listener should be detached after finish");

    // Second pass on the same source instance
    assertEquals(2, runPassthrough(list).size());
    assertEquals(0, list.getRowListeners().size(), "still no leftover listeners after second run");
  }

  private List<RowMetaAndData> runPassthrough(LeanListConnector sharedSource) throws Exception {
    LeanConnector sourceWrap = new LeanConnector("source", sharedSource);
    LeanPassthroughConnector transform = new LeanPassthroughConnector("source");
    LeanConnector transformWrap = new LeanConnector("pass", transform);
    PresentationDataContext ctx = ConnectorTestSupport.dataContext(sourceWrap, transformWrap);
    // retrieveRows uses dataContext.getConnector which copies connectors — that hides the bug.
    // Stream against the shared instance by calling the transform with a context whose
    // getConnector("source") returns the same wrapper each time (no copy).
    PresentationDataContext noCopy =
        new PresentationDataContext(ctx.getPresentation(), ctx.getMetadataProvider()) {
          @Override
          public LeanConnector getConnector(String name) {
            if ("source".equals(name)) {
              return sourceWrap;
            }
            if ("pass".equals(name)) {
              return transformWrap;
            }
            return null;
          }
        };
    return transformWrap.retrieveRows(noCopy);
  }
}
