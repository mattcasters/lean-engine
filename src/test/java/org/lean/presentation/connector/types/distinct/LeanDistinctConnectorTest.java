package org.lean.presentation.connector.types.distinct;

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

class LeanDistinctConnectorTest {

  @BeforeEach
  void setUp() throws Exception {
    ConnectorTestSupport.initEnvironment();
  }

  @Test
  void removesConsecutiveDuplicatesOnlyWhenAdjacentAfterSourceOrder() throws Exception {
    // Distinct compares to previous row: non-adjacent duplicates pass through.
    LeanListConnector source =
        new LeanListConnector("v", Arrays.asList("a", "a", "b", "b", "a"));
    LeanConnector sourceConn = ConnectorTestSupport.wrap("source", source);

    LeanDistinctConnector distinct = new LeanDistinctConnector();
    distinct.setSourceConnectorName("source");
    LeanConnector distinctConn = ConnectorTestSupport.wrap("distinct", distinct);

    PresentationDataContext ctx = ConnectorTestSupport.dataContext(sourceConn, distinctConn);
    List<RowMetaAndData> rows = ConnectorTestSupport.retrieve(distinctConn, ctx);

    assertEquals(3, rows.size());
    assertEquals("a", rows.get(0).getString("v", null));
    assertEquals("b", rows.get(1).getString("v", null));
    assertEquals("a", rows.get(2).getString("v", null));
  }
}
