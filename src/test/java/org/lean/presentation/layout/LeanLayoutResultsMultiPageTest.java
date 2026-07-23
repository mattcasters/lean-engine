package org.lean.presentation.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import org.apache.hop.core.logging.LogChannel;
import org.apache.hop.core.logging.LoggingObject;
import org.apache.hop.metadata.serializer.memory.MemoryMetadataProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lean.core.LeanAttachment;
import org.lean.core.LeanColumn;
import org.lean.core.LeanEnvironment;
import org.lean.core.LeanGeometry;
import org.lean.presentation.LeanPresentation;
import org.lean.presentation.component.LeanComponent;
import org.lean.presentation.component.types.label.LeanLabelComponent;
import org.lean.presentation.component.types.table.LeanTableComponent;
import org.lean.presentation.connector.LeanConnector;
import org.lean.presentation.connector.types.sampledata.LeanSampleDataConnector;
import org.lean.presentation.layout.LeanLayout;
import org.lean.presentation.page.LeanPage;
import org.lean.presentation.theme.LeanTheme;
import org.lean.render.context.PresentationRenderContext;
import org.lean.util.BasePresentationUtil;

/**
 * Multi-page tables leave {@code getCurrentRenderPage} on the last overflow page. Relatively
 * positioned siblings must use first-part geometry and land on the first body page.
 */
class LeanLayoutResultsMultiPageTest {

  @BeforeEach
  void setUp() throws Exception {
    LeanEnvironment.init();
    BasePresentationUtil.registerTestPlugins();
  }

  @Test
  void firstGeometryIsStableAcrossParts() {
    LeanLayoutResults results = new LeanLayoutResults(new LogChannel("test"));
    results.addComponentGeometry("T", new LeanGeometry(10, 20, 100, 200));
    results.addComponentGeometry("T", new LeanGeometry(10, 20, 100, 50));
    assertEquals(200, results.findFirstGeometry("T").getHeight());
    assertEquals(50, results.findGeometry("T").getHeight());
  }

  @Test
  void firstRenderPageIsNotLastAfterOverflow() {
    LeanLayoutResults results = new LeanLayoutResults(new LogChannel("test"));
    LeanPage page = LeanPage.getA4(true);
    LeanRenderPage p1 = results.addNewPage(page, null);
    LeanRenderPage p2 = results.addNewPage(page, p1);
    LeanRenderPage p3 = results.addNewPage(page, p2);
    assertSame(p1, results.getFirstRenderPage(page));
    assertSame(p3, results.getCurrentRenderPage(page));
  }

  @Test
  void relativeSiblingLandsOnFirstPageBesideMultiPageTable() throws Exception {
    MemoryMetadataProvider metadata = new MemoryMetadataProvider();
    LeanTheme theme = LeanTheme.getDefault();
    metadata.getSerializer(LeanTheme.class).save(theme);
    // Enough rows that a short page forces table pagination
    LeanSampleDataConnector sample = new LeanSampleDataConnector(80);
    LeanConnector connector = new LeanConnector("sample", sample);
    metadata.getSerializer(LeanConnector.class).save(connector);

    LeanPresentation presentation = new LeanPresentation();
    presentation.setName("relative-multipage");
    presentation.setDefaultThemeName(theme.getName());
    presentation.getThemes().add(theme);

    // Small usable height so table overflows quickly (width, height, L, R, T, B margins)
    LeanPage page = new LeanPage(400, 300, 10, 10, 10, 10);
    presentation.getPages().add(page);

    LeanTableComponent tableType = new LeanTableComponent();
    tableType.setSourceConnectorName("sample");
    tableType.setColumnSelection(
        new ArrayList<>(Collections.singletonList(new LeanColumn("id"))));
    LeanComponent table = new LeanComponent("ProductsTable", tableType);
    table.setLayout(LeanLayout.topLeftPage());
    page.getComponents().add(table);

    // Side label: left = table RIGHT (same pattern as products Bar Chart)
    LeanLabelComponent labelType = new LeanLabelComponent("SIDE");
    LeanComponent side = new LeanComponent("SideChart", labelType);
    LeanLayout sideLayout = new LeanLayout();
    sideLayout.setLeft(
        new LeanAttachment("ProductsTable", 0, 5, LeanAttachment.Alignment.RIGHT));
    sideLayout.setTop(new LeanAttachment(null, 0, 10, LeanAttachment.Alignment.TOP));
    side.setLayout(sideLayout);
    page.getComponents().add(side);

    PresentationRenderContext renderContext =
        new PresentationRenderContext(presentation, metadata);
    LeanLayoutResults results =
        presentation.doLayout(
            new LoggingObject("test"),
            renderContext,
            metadata,
            Collections.emptyList());

    assertTrue(results.getRenderPages().size() > 1, "table should paginate");

    LeanGeometry tableFirst = results.findFirstGeometry("ProductsTable");
    assertNotNull(tableFirst);
    LeanGeometry sideGeo = results.findFirstGeometry("SideChart");
    assertNotNull(sideGeo, "side component must receive geometry");

    // Left of side = right of first table part + offset 5
    assertEquals(tableFirst.getX() + tableFirst.getWidth() + 5, sideGeo.getX());

    // Side chart must be on the first render page, not only the last
    LeanRenderPage firstPage = results.getFirstRenderPage(page);
    boolean foundOnFirst = false;
    for (var lr : firstPage.getLayoutResults()) {
      if (lr.getComponent() != null && "SideChart".equals(lr.getComponent().getName())) {
        foundOnFirst = true;
        break;
      }
    }
    assertTrue(foundOnFirst, "SideChart must be laid out on first body page");
  }
}
