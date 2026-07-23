package org.lean.presentation.connector.types.chain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.hop.core.RowMetaAndData;
import org.apache.hop.core.row.IRowMeta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lean.core.LeanColumn;
import org.lean.presentation.connector.ConnectorTestSupport;
import org.lean.presentation.connector.LeanConnector;
import org.lean.presentation.connector.type.ILeanConnector;
import org.lean.presentation.connector.types.distinct.LeanDistinctConnector;
import org.lean.presentation.connector.types.sampledata.LeanSampleDataConnector;
import org.lean.presentation.connector.types.selection.LeanSelectionConnector;
import org.lean.presentation.connector.types.sort.LeanSortConnector;
import org.lean.core.LeanSortMethod;
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

  /**
   * Multi-step chains wire intermediate sources as {@code __ChainConnector_N}. describeOutput must
   * use {@link org.lean.presentation.datacontext.ChainDataContext} so those names resolve (streaming
   * already did).
   */
  @Test
  void multiStepDescribeUsesChainContext() throws Exception {
    LeanSampleDataConnector sample = new LeanSampleDataConnector(12);
    LeanConnector sampleConn = ConnectorTestSupport.wrap("sample", sample);

    LeanSelectionConnector selection =
        new LeanSelectionConnector(
            Arrays.asList(new LeanColumn("name"), new LeanColumn("color")));
    LeanSortConnector sort =
        new LeanSortConnector(
            Collections.singletonList(new LeanColumn("color")),
            Collections.singletonList(new LeanSortMethod()));
    LeanDistinctConnector distinct = new LeanDistinctConnector();

    List<ILeanConnector> steps = Arrays.asList(selection, sort, distinct);
    LeanChainConnector chain = new LeanChainConnector("sample", steps);
    LeanConnector chainConn = ConnectorTestSupport.wrap("product-style-chain", chain);

    PresentationDataContext ctx = ConnectorTestSupport.dataContext(sampleConn, chainConn);

    IRowMeta meta =
        assertDoesNotThrow(
            () -> chain.describeOutput(ctx),
            "describeOutput must resolve intermediate chain step sources");
    assertEquals(2, meta.size());
    assertEquals("name", meta.getValueMeta(0).getName());
    assertEquals("color", meta.getValueMeta(1).getName());

    List<RowMetaAndData> rows = ConnectorTestSupport.retrieve(chainConn, ctx);
    // 12 sample rows with repeating colors → fewer after adjacent distinct on sorted color
    assertEquals(2, rows.get(0).getRowMeta().size());
  }
}
