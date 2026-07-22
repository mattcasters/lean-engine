package org.lean.presentation.connector.types.sort;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.hop.core.RowMetaAndData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lean.core.LeanColumn;
import org.lean.core.LeanSortMethod;
import org.lean.presentation.connector.ConnectorTestSupport;
import org.lean.presentation.connector.LeanConnector;
import org.lean.presentation.connector.types.list.LeanListConnector;
import org.lean.presentation.datacontext.PresentationDataContext;

class LeanSortConnectorTest {

  @BeforeEach
  void setUp() throws Exception {
    ConnectorTestSupport.initEnvironment();
  }

  @Test
  void sortsAscendingByColumn() throws Exception {
    LeanListConnector source =
        new LeanListConnector("name", Arrays.asList("Charlie", "Alice", "Bob"));
    LeanConnector sourceConn = ConnectorTestSupport.wrap("source", source);

    LeanSortConnector sort =
        new LeanSortConnector(
            Collections.singletonList(new LeanColumn("name")),
            Collections.singletonList(
                new LeanSortMethod(LeanSortMethod.Type.NATIVE_VALUE, true)));
    sort.setSourceConnectorName("source");
    LeanConnector sortConn = ConnectorTestSupport.wrap("sorted", sort);

    PresentationDataContext ctx = ConnectorTestSupport.dataContext(sourceConn, sortConn);
    List<RowMetaAndData> rows = ConnectorTestSupport.retrieve(sortConn, ctx);

    assertEquals(3, rows.size());
    assertEquals("Alice", rows.get(0).getString("name", null));
    assertEquals("Bob", rows.get(1).getString("name", null));
    assertEquals("Charlie", rows.get(2).getString("name", null));
  }

  @Test
  void sortsDescendingByColumn() throws Exception {
    LeanListConnector source =
        new LeanListConnector("name", Arrays.asList("Charlie", "Alice", "Bob"));
    LeanConnector sourceConn = ConnectorTestSupport.wrap("source", source);

    LeanSortConnector sort =
        new LeanSortConnector(
            Collections.singletonList(new LeanColumn("name")),
            Collections.singletonList(
                new LeanSortMethod(LeanSortMethod.Type.NATIVE_VALUE, false)));
    sort.setSourceConnectorName("source");
    LeanConnector sortConn = ConnectorTestSupport.wrap("sorted", sort);

    PresentationDataContext ctx = ConnectorTestSupport.dataContext(sourceConn, sortConn);
    List<RowMetaAndData> rows = ConnectorTestSupport.retrieve(sortConn, ctx);

    assertEquals("Charlie", rows.get(0).getString("name", null));
    assertEquals("Bob", rows.get(1).getString("name", null));
    assertEquals("Alice", rows.get(2).getString("name", null));
  }
}
