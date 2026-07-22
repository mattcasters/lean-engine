package org.lean.presentation.connector.types.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.hop.core.RowMetaAndData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lean.presentation.connector.ConnectorTestSupport;
import org.lean.presentation.connector.LeanConnector;
import org.lean.presentation.connector.types.list.LeanListConnector;
import org.lean.presentation.datacontext.PresentationDataContext;

class LeanSimpleFilterConnectorTest {

  @BeforeEach
  void setUp() throws Exception {
    ConnectorTestSupport.initEnvironment();
  }

  @Test
  void keepsOnlyMatchingValues() throws Exception {
    LeanListConnector source =
        new LeanListConnector("color", Arrays.asList("Red", "Green", "Blue", "Red", "Yellow"));
    LeanConnector sourceConn = ConnectorTestSupport.wrap("source", source);

    LeanSimpleFilterConnector filter =
        new LeanSimpleFilterConnector(
            Collections.singletonList(new SimpleFilterValue("color", "Red")));
    filter.setSourceConnectorName("source");
    LeanConnector filterConn = ConnectorTestSupport.wrap("filtered", filter);

    PresentationDataContext ctx = ConnectorTestSupport.dataContext(sourceConn, filterConn);
    List<RowMetaAndData> rows = ConnectorTestSupport.retrieve(filterConn, ctx);

    assertEquals(2, rows.size());
    assertEquals("Red", rows.get(0).getString("color", null));
    assertEquals("Red", rows.get(1).getString("color", null));
  }
}
