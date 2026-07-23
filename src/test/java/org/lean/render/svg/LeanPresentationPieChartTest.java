package org.lean.render.svg;

import org.junit.jupiter.api.Test;
import org.lean.presentation.LeanPresentation;
import org.lean.util.PieChartPresentationUtil;

public class LeanPresentationPieChartTest extends LeanPresentationTestBase {

  @Test
  public void testPieChartRender() throws Exception {
    LeanPresentation presentation =
        new PieChartPresentationUtil(metadataProvider, variables).createPieChartPresentation(4300);
    testRendering(presentation, "pie_chart_test");
  }

  @Test
  public void testDonutChartRender() throws Exception {
    LeanPresentation presentation =
        new PieChartPresentationUtil(metadataProvider, variables).createDonutChartPresentation(4301);
    testRendering(presentation, "donut_chart_test");
  }
}
