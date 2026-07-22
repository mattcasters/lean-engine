package org.lean.presentation.connector.types.selection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Collections;
import java.util.List;
import org.apache.hop.core.RowMetaAndData;
import org.apache.hop.core.row.IRowMeta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lean.core.LeanColumn;
import org.lean.presentation.connector.ConnectorTestSupport;
import org.lean.presentation.connector.LeanConnector;
import org.lean.presentation.connector.types.sampledata.LeanSampleDataConnector;
import org.lean.presentation.datacontext.PresentationDataContext;

class LeanSelectionConnectorTest {

  @BeforeEach
  void setUp() throws Exception {
    ConnectorTestSupport.initEnvironment();
  }

  @Test
  void selectsSubsetOfFields() throws Exception {
    LeanSampleDataConnector source = new LeanSampleDataConnector(5);
    LeanConnector sourceConn = ConnectorTestSupport.wrap("source", source);

    LeanSelectionConnector selection =
        new LeanSelectionConnector(Collections.singletonList(new LeanColumn("name")));
    selection.setSourceConnectorName("source");
    LeanConnector selectionConn = ConnectorTestSupport.wrap("selected", selection);

    PresentationDataContext ctx = ConnectorTestSupport.dataContext(sourceConn, selectionConn);

    IRowMeta outMeta = selection.describeOutput(ctx);
    assertEquals(1, outMeta.size());
    assertEquals("name", outMeta.getValueMeta(0).getName());

    List<RowMetaAndData> rows = ConnectorTestSupport.retrieve(selectionConn, ctx);
    assertEquals(5, rows.size());
    assertNull(rows.get(0).getRowMeta().searchValueMeta("id"));
    assertEquals(1, rows.get(0).getRowMeta().size());
  }
}
