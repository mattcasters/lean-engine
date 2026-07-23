package org.lean.util;

import java.util.Arrays;
import org.apache.hop.core.variables.IVariables;
import org.apache.hop.metadata.api.IHopMetadataProvider;
import org.lean.core.AggregationMethod;
import org.lean.core.LeanDimension;
import org.lean.core.LeanFact;
import org.lean.core.LeanHorizontalAlignment;
import org.lean.core.LeanVerticalAlignment;
import org.lean.presentation.LeanPresentation;
import org.lean.presentation.component.LeanComponent;
import org.lean.presentation.component.types.chart.LeanPieChartComponent;
import org.lean.presentation.component.types.label.LeanLabelComponent;
import org.lean.presentation.layout.LeanLayoutBuilder;

public class PieChartPresentationUtil extends BasePresentationUtil {

  public static final String PIE_CHART_NAME = "PieChart";

  public PieChartPresentationUtil(IHopMetadataProvider metadataProvider, IVariables variables) {
    super(metadataProvider, variables);
  }

  public LeanPresentation createPieChartPresentation(int nr) throws Exception {
    LeanPresentation presentation =
        createBasePresentation(
            "PieChart (" + nr + ")",
            "PieChart " + nr + " description",
            1000,
            "Pie chart filling whole page");

    LeanPieChartComponent pieChart = createColorRandomPieChart();

    LeanComponent pieChartComponent = new LeanComponent(PIE_CHART_NAME, pieChart);
    pieChartComponent.setLayout(new LeanLayoutBuilder().all(5).build());

    presentation.getPages().get(0).getComponents().add(pieChartComponent);

    return presentation;
  }

  public LeanPresentation createDonutChartPresentation(int nr) throws Exception {
    LeanPresentation presentation = createPieChartPresentation(nr);

    LeanPieChartComponent chart =
        (LeanPieChartComponent)
            presentation.getPages().get(0).findComponent(PIE_CHART_NAME).getComponent();

    chart.setInnerRadiusPercent("50");
    chart.setLegendPosition("BOTTOM");
    chart.setTitle("Random value by Color (donut)");
    chart.setShowingFactValues(true);

    LeanLabelComponent label =
        (LeanLabelComponent)
            presentation.getHeader().findComponent(HEADER_MESSAGE_LABEL).getComponent();
    label.setLabel("Donut chart filling the whole page");
    return presentation;
  }

  public LeanPieChartComponent createColorRandomPieChart() {
    LeanPieChartComponent chart = new LeanPieChartComponent(CONNECTOR_SAMPLE_ROWS);
    chart.setHorizontalDimensions(
        Arrays.asList(
            new LeanDimension(
                "color", "Color", LeanHorizontalAlignment.CENTER, LeanVerticalAlignment.MIDDLE)));
    LeanFact sumFact =
        new LeanFact(
            "random",
            "Sum",
            LeanHorizontalAlignment.RIGHT,
            LeanVerticalAlignment.MIDDLE,
            AggregationMethod.SUM,
            "0.000");
    sumFact.setFormatMask("0.00");
    chart.setFacts(Arrays.asList(sumFact));
    chart.setHorizontalMargin(10);
    chart.setVerticalMargin(10);
    chart.setBorder(true);
    chart.setBackground(false);
    chart.setTitle("Random by Color");
    chart.setShowingLegend(true);
    chart.setLegendPosition("RIGHT");
    chart.setShowingSliceLabels(true);
    chart.setShowingPercentages(true);
    chart.setShowingFactValues(false);
    chart.setInnerRadiusPercent("0");
    chart.setStartAngleDegrees("-90");
    chart.setClockwise(true);

    return chart;
  }
}
