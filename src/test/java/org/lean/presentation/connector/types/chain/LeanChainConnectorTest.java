package org.lean.presentation.connector.types.chain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Collections;
import java.util.List;
import org.apache.hop.core.RowMetaAndData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lean.core.LeanColumn;
import org.lean.presentation.connector.ConnectorTestSupport;
import org.lean.presentation.connector.LeanConnector;
import org.lean.presentation.connector.types.sampledata.LeanSampleDataConnector;
import org.lean.presentation.connector.types.selection.LeanSelectionConnector;
import org.lean.presentation.datacontext.PresentationDataContext;

class LeanChainConnectorTest {

  @BeforeEach
  void setUp() throws Exception {
    ConnectorTestSupport.initEnvironment();
  }

  @Test
  void chainsSelectionOverSampleData() throws Exception {
    LeanSampleDataConnector sample = new LeanSampleDataConnector(8);
    LeanConnector sampleConn = ConnectorTestSupport.wrap("sample", sample);

    LeanSelectionConnector selection =
        new LeanSelectionConnector(Collections.singletonList(new LeanColumn("name")));
    LeanChainConnector chain =
        new LeanChainConnector("sample", Collections.singletonList(selection));
    LeanConnector chainConn = ConnectorTestSupport.wrap("chained", chain);

    PresentationDataContext ctx = ConnectorTestSupport.dataContext(sampleConn, chainConn);

    assertEquals(1, chain.describeOutput(ctx).size());
    assertEquals("name", chain.describeOutput(ctx).getValueMeta(0).getName());

    List<RowMetaAndData> rows = ConnectorTestSupport.retrieve(chainConn, ctx);
    assertEquals(8, rows.size());
    assertEquals(1, rows.get(0).getRowMeta().size());
    assertNull(rows.get(0).getRowMeta().searchValueMeta("id"));
  }
}
