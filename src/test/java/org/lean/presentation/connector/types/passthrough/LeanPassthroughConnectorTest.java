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

class LeanPassthroughConnectorTest {

  @BeforeEach
  void setUp() throws Exception {
    ConnectorTestSupport.initEnvironment();
  }

  @Test
  void passesAllSourceRowsUnchanged() throws Exception {
    LeanListConnector source = new LeanListConnector("v", Arrays.asList("one", "two", "three"));
    LeanConnector sourceConn = ConnectorTestSupport.wrap("source", source);

    LeanPassthroughConnector passthrough = new LeanPassthroughConnector("source");
    LeanConnector passConn = ConnectorTestSupport.wrap("pass", passthrough);

    PresentationDataContext ctx = ConnectorTestSupport.dataContext(sourceConn, passConn);
    List<RowMetaAndData> rows = ConnectorTestSupport.retrieve(passConn, ctx);

    assertEquals(3, rows.size());
    assertEquals("one", rows.get(0).getString("v", null));
    assertEquals("three", rows.get(2).getString("v", null));
  }
}
