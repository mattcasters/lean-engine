package org.lean.presentation.connector.types.sampledata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.apache.hop.core.RowMetaAndData;
import org.apache.hop.core.row.IRowMeta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lean.presentation.connector.ConnectorTestSupport;
import org.lean.presentation.connector.LeanConnector;
import org.lean.presentation.datacontext.PresentationDataContext;

class LeanSampleDataConnectorTest {

  @BeforeEach
  void setUp() throws Exception {
    ConnectorTestSupport.initEnvironment();
  }

  @Test
  void streamsConfiguredRowCount() throws Exception {
    int rowCount = 25;
    LeanSampleDataConnector sample = new LeanSampleDataConnector(rowCount);
    LeanConnector connector = ConnectorTestSupport.wrap("sample", sample);
    PresentationDataContext ctx = ConnectorTestSupport.dataContext(connector);

    IRowMeta meta = sample.describeOutput(ctx);
    assertEquals(7, meta.size());
    assertNotNull(meta.searchValueMeta("id"));
    assertNotNull(meta.searchValueMeta("name"));

    List<RowMetaAndData> rows = ConnectorTestSupport.retrieve(connector, ctx);
    assertEquals(rowCount, rows.size());
    assertEquals(1L, rows.get(0).getInteger("id", 0));
    assertTrue(rows.get(0).getString("name", "").length() > 0);
  }

  @Test
  void cloneCopiesRowCount() {
    LeanSampleDataConnector original = new LeanSampleDataConnector(12);
    LeanSampleDataConnector copy = original.clone();
    assertEquals(12, copy.getRowCount());
    assertEquals("SampleDataConnector", copy.getPluginId());
  }
}
