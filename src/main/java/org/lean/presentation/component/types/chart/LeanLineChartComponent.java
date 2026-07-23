package org.lean.presentation.component.types.chart;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.awt.BasicStroke;
import java.awt.Stroke;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.apache.batik.svggen.SVGGraphics2D;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.apache.hop.core.Const;
import org.apache.hop.core.exception.HopValueException;
import org.lean.core.gui.plugin.LeanWidgetType;
import org.lean.core.gui.plugin.LeanWidgetElement;
import org.apache.hop.core.row.IValueMeta;
import org.apache.hop.metadata.api.HopMetadataProperty;
import org.lean.core.LeanColorRGB;
import org.lean.core.LeanGeometry;
import org.lean.core.LeanPosition;
import org.lean.core.LeanTextGeometry;
import org.lean.core.draw.DrawnContext;
import org.lean.core.draw.DrawnItem;
import org.lean.core.exception.LeanException;
import org.lean.core.gui.form.LeanGuiFormConstants;
import org.lean.presentation.LeanComponentLayoutResult;
import org.lean.presentation.component.LeanComponent;
import org.lean.presentation.component.type.ILeanComponent;
import org.lean.presentation.component.type.LeanComponentPlugin;
import org.lean.presentation.layout.LeanLayoutResults;
import org.lean.presentation.theme.LeanTheme;
import org.lean.render.IRenderContext;

@JsonDeserialize(as = LeanLineChartComponent.class)
@LeanComponentPlugin(
    id = "LeanLineChartComponent",
    name = "Line chart",
    description = "A line chart component",
    image = "ui/images/components/line-chart.svg")
@Getter
@Setter
public class LeanLineChartComponent extends LeanBaseChartComponent implements ILeanComponent {

  @LeanWidgetElement(
      order = "12000-drawingCurvedTrendLine",
      parentId = LeanGuiFormConstants.PARENT_PLUGIN,
      type = LeanWidgetType.CHECKBOX,
      label = "Draw curved trend line?")
  @HopMetadataProperty
  protected boolean drawingCurvedTrendLine;

  public LeanLineChartComponent() {
    this((String) null);
  }

  public LeanLineChartComponent(String connectorName) {
    super("LeanLineChartComponent", connectorName);
  }

  public LeanLineChartComponent(LeanLineChartComponent c) {
    super("LeanLineChartComponent", c);
    drawingCurvedTrendLine = c.drawingCurvedTrendLine;
  }

  public LeanLineChartComponent clone() {
    return new LeanLineChartComponent(this);
  }

  @Override
  public void render(
      LeanComponentLayoutResult layoutResult,
      LeanLayoutResults results,
      IRenderContext renderContext,
      LeanPosition offSet)
      throws LeanException {
    LeanGeometry componentGeometry = layoutResult.getGeometry();
    LeanComponent component = layoutResult.getComponent();
    SVGGraphics2D gc = layoutResult.getRenderPage().getGc();
    List<DrawnItem> drawnItems = layoutResult.getRenderPage().getDrawnItems();

    LeanTheme theme = renderContext.lookupTheme(themeName);

    int x = componentGeometry.getX();
    int y = componentGeometry.getY();
    int width = componentGeometry.getWidth();
    int height = componentGeometry.getHeight();
    int tickSize = 4;

    if (isIncompleteChartConfig()) {
      renderIncompletePlaceholder(gc, componentGeometry, renderContext);
      return;
    }

    // Now get the horizontal dimension combinations
    //
    // Get all dimension combinations horizontally
    // Then sort this list of lists...
    //
    ChartDetails details = calculateDetails(gc, x, y, width, height);

    // Render the background and border to get started
    //
    drawBackGround(gc, componentGeometry, renderContext);
    drawBorder(gc, componentGeometry, renderContext);

    // Draw the title
    //
    if (StringUtils.isNotEmpty(titleText)) {
      int titleX = x + (width - details.titleGeometry.getWidth()) / 2;
      int titleY = y + verticalMargin + details.titleGeometry.getHeight();
      enableColor(gc, lookupTitleColor(renderContext));
      enableFont(gc, lookupTitleFont(renderContext));
      gc.drawString(titleText, titleX, titleY);

      drawnItems.add(
          new DrawnItem(
              component.getName(),
              component.getComponent().getPluginId(),
              layoutResult.getPartNumber(),
              DrawnItem.DrawnItemType.ComponentItem,
              DrawnItem.Category.Title.name(),
              0,
              0,
              new LeanGeometry(
                  offSet.getX() + titleX,
                  offSet.getY() + titleY - details.titleGeometry.getHeight(),
                  details.titleGeometry.getWidth(),
                  details.titleGeometry.getHeight()),
              new DrawnContext(titleText)));
    }

    // Draw the X and Y axis
    //
    enableColor(gc, lookupAxisColor(renderContext));

    // top left X and Y
    //
    double topLeftX =
        x
            + horizontalMargin
            + (showingVerticalLabels ? details.maxFactWidth + horizontalMargin : 0);
    double topLeftY = y + verticalMargin + details.titleHeight;

    // bottom left X and Y
    //
    double bottomLeftX = topLeftX;
    double bottomLeftY =
        y
            + height
            - verticalMargin
            - (showingHorizontalLabels ? details.maxLabelHeight + verticalMargin : 0);

    // bottom right X and Y
    //
    double bottomRightX = x + width - horizontalMargin;
    double bottomRightY = bottomLeftY;

    // X axis
    //
    gc.drawLine((int) bottomLeftX, (int) bottomLeftY, (int) bottomRightX, (int) bottomRightY);

    // Y axis
    //
    gc.drawLine((int) topLeftX, (int) topLeftY, (int) bottomLeftX, (int) bottomLeftY);

    // Draw the min value
    //
    double minX = topLeftX;
    double minY = bottomLeftY;
    if (!usingZeroBaseline) {
      minY -= details.overshoot;
    }
    LeanTextGeometry minGeo = details.minLabelGeometry;
    if (showingAxisTicks) {
      gc.drawLine(
          (int) (minX - tickSize / 2), (int) (minY), (int) (minX + tickSize / 2), (int) minY);
    }
    if (showingVerticalLabels && StringUtils.isNotEmpty(details.minLabel)) {
      enableFont(gc, lookupVerticalDimensionsFont(renderContext));
      int labelX = x + horizontalMargin;
      int labelY = (int) (minY + minGeo.getHeight() / 2);
      gc.drawString(details.minLabel, labelX, labelY);

      drawnItems.add(
          new DrawnItem(
              component.getName(),
              component.getComponent().getPluginId(),
              layoutResult.getPartNumber(),
              DrawnItem.DrawnItemType.ComponentItem,
              DrawnItem.Category.YAxisLabel.name(),
              0,
              0,
              new LeanGeometry(
                  offSet.getX() + labelX,
                  offSet.getY() + labelY - details.minLabelGeometry.getHeight(),
                  details.minLabelGeometry.getWidth(),
                  details.minLabelGeometry.getHeight()),
              new DrawnContext(details.minLabel)));
    }

    // Draw the max value
    //
    double maxX = topLeftX;
    double maxY = topLeftY + details.overshoot;
    LeanTextGeometry maxGeo = details.maxLabelGeometry;
    if (showingAxisTicks) {
      gc.drawLine(
          (int) (maxX - tickSize / 2), (int) (maxY), (int) (maxX + tickSize / 2), (int) maxY);
    }
    if (showingVerticalLabels && StringUtils.isNotEmpty(details.maxLabel)) {
      enableFont(gc, lookupVerticalDimensionsFont(renderContext));
      int labelX = x + horizontalMargin;
      int labelY = (int) (maxY + maxGeo.getHeight() / 2);
      gc.drawString(details.maxLabel, labelX, labelY);

      drawnItems.add(
          new DrawnItem(
              component.getName(),
              component.getComponent().getPluginId(),
              layoutResult.getPartNumber(),
              DrawnItem.DrawnItemType.ComponentItem,
              DrawnItem.Category.YAxisLabel.name(),
              0,
              0,
              new LeanGeometry(
                  offSet.getX() + labelX,
                  offSet.getY() + labelY - details.maxLabelGeometry.getHeight(),
                  details.maxLabelGeometry.getWidth(),
                  details.maxLabelGeometry.getHeight()),
              new DrawnContext(details.maxLabel)));
    }

    // Loop over the vertical dimension value combinations
    // This is the chart series...
    //
    List<List<String>> verticalCombinations = new ArrayList<>();
    verticalCombinations.addAll(details.verticalCombinations);
    if (verticalCombinations.isEmpty()) {
      // At least perform once without vertical dimensions...
      //
      verticalCombinations.add(new ArrayList<>());
    }

    // Keep the location for the label...
    //
    Point2D.Double labelPoint = null;
    int horizontalLabelsDrawn = 1;

    for (int series = 0; series < verticalCombinations.size(); series++) {
      List<String> verticalCombination = verticalCombinations.get(series);

      List<Double> xCoordinates = new ArrayList<>();
      List<Double> yCoordinates = new ArrayList<>();

      // Draw the parts: one for every bottom horizontal label
      //
      double lastX = -1;
      double lastY = -1;
      for (int part = 0; part < details.labels.size(); part++) {
        // Only draw this label once
        if (series == 0) {
          String label = details.labels.get(part);
          LeanTextGeometry geometry = details.labelGeometries.get(part);

          // The texts will be centered in the part.
          //
          double labelX =
              bottomLeftX
                  + part * details.partWidth
                  + (details.partWidth - geometry.getWidth()) / 2
                  + geometry.getOffsetX();
          double labelY = bottomLeftY + verticalMargin + geometry.getOffsetY();

          if (showingHorizontalLabels) {
            if (actualHorizontalLabelInterval <= 0
                || (horizontalLabelsDrawn % actualHorizontalLabelInterval == 0)) {
              enableColor(gc, lookupDefaultColor(renderContext));
              enableFont(gc, lookupHorizontalDimensionsFont(renderContext));
              gc.drawString(label, (int) labelX, (int) labelY);

              drawnItems.add(
                  new DrawnItem(
                      component.getName(),
                      component.getComponent().getPluginId(),
                      layoutResult.getPartNumber(),
                      DrawnItem.DrawnItemType.ComponentItem,
                      DrawnItem.Category.XAxisLabel.name(),
                      0,
                      0,
                      new LeanGeometry(
                          (int) (offSet.getX() + labelX),
                          (int) (offSet.getY() + labelY - geometry.getHeight()),
                          geometry.getWidth(),
                          geometry.getHeight()),
                      new DrawnContext(label)));
            }
            horizontalLabelsDrawn++;
          }

          // Draw a small tick at the end of the part
          //
          if (showingAxisTicks) {
            double tickX = bottomLeftX + part * details.partWidth + details.partWidth;
            double tickY = bottomLeftY - tickSize / 2;
            enableColor(gc, lookupAxisColor(renderContext));
            gc.drawLine((int) tickX, (int) tickY, (int) tickX, (int) (tickY + tickSize));
          }
        }

        // Draw the fact series...
        //
        // List<String> factLabels = details.factLabels.get(series);
        List<Object> factValues = details.factValues.get(series);
        List<IValueMeta> factValueMetas = details.factValueMetas.get(series);

        Object valueData = factValues.get(part);
        IValueMeta valueMeta = factValueMetas.get(part);
        double factValue = 0;
        try {
          Double factValueDouble = valueMeta.getNumber(valueData);
          if (factValueDouble == null) {
            factValue = 0.0d;
          } else {
            factValue = factValueDouble.doubleValue();
          }
        } catch (HopValueException e) {
          throw new LeanException("Fact data conversion error", e);
        }
        double factX = bottomLeftX + part * details.partWidth + details.partWidth / 2;
        double factY =
            topLeftY + details.overshoot + ((details.maxValue - factValue) * details.valueFactor);

        // See if we need to set a specific theme scheme for the line...
        //
        if (theme == null) {
          enableColor(gc, lookupDefaultColor(renderContext));
        } else {
          // Color is depending on the series we're drawing...
          //
          String labelValue = getCombinationString(verticalCombination);
          LeanColorRGB color = renderContext.getStableColor(theme.getName(), labelValue);
          enableColor(gc, color);
        }

        if (dotSize > 0) {
          gc.drawRect(
              (int) (factX - dotSize / 2),
              (int) (factY - dotSize / 2),
              (int) dotSize,
              (int) dotSize);
        }

        xCoordinates.add(factX);
        yCoordinates.add(factY);

        // Keep the location of the last dot in a series to put the label on it...
        //
        if (!verticalCombination.isEmpty() && part == details.labels.size() - 1) {

          labelPoint = new Point2D.Double(factX, factY);
        }
      }

      Stroke stroke = gc.getStroke();
      float lw = (float) Const.toDouble(Const.NVL(lineWidth, "1.0"), 1.0d);

      if (isDrawingCurvedTrendLine() && xCoordinates.size() > 2) {
        // Quarter of the line width, it's a trend...
        //
        gc.setStroke(new BasicStroke(lw, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));

        // SplineInterpolator requires strictly increasing X. Preview/layout can produce
        // out-of-order or duplicate abscissae (e.g. after re-layout at different size).
        double[][] sorted = sortStrictlyIncreasingX(xCoordinates, yCoordinates);
        double[] xs = sorted[0];
        double[] ys = sorted[1];

        if (xs.length < 3) {
          // Not enough distinct X values for a cubic spline — fall back to polylines
          drawStraightSegments(gc, xCoordinates, yCoordinates);
        } else {
          try {
            SplineInterpolator splineInterpolator = new SplineInterpolator();
            PolynomialSplineFunction function = splineInterpolator.interpolate(xs, ys);

            // TODO: make the number of curve points configurable...
            //
            int nrValues = xs.length * 30;
            double[] xi = new double[nrValues];
            xi[0] = xs[0];
            xi[xi.length - 1] = xs[xs.length - 1];
            double diff = xs[xs.length - 1] - xi[0];
            double delta = diff / nrValues;
            for (int i = 1; i < xi.length - 1; i++) {
              xi[i] = xs[0] + delta * i;
            }

            double lx = xs[0];
            double ly = ys[0];

            Path2D path = new Path2D.Double();
            path.moveTo(lx, ly);

            for (int i = 1; i < xi.length; i++) {
              // Clamp into the spline domain [xs[0], xs[last]]
              double xq = Math.min(xs[xs.length - 1], Math.max(xs[0], xi[i]));
              double yi = function.value(xq);
              path.lineTo(xq, yi);
              lx = xq;
              ly = yi;
            }
            gc.draw(path);
          } catch (Exception e) {
            // Any interpolation failure: still show the series as straight segments
            drawStraightSegments(gc, xCoordinates, yCoordinates);
          }
        }
        gc.setStroke(stroke);

      } else {

        // Draw the actual line
        //
        gc.setStroke(new BasicStroke(lw, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));
        drawStraightSegments(gc, xCoordinates, yCoordinates);
        gc.setStroke(stroke);
      }

      if (labelPoint != null) {
        // At the end, draw the series name...
        //
        double factX = labelPoint.x - dotSize - 2;
        double factY = labelPoint.y - dotSize - 2;

        String seriesLabel = getCombinationString(verticalCombination);

        gc.drawString(seriesLabel, (int) factX, (int) factY);

        LeanTextGeometry seriesGeometry = calculateTextGeometry(gc, seriesLabel);

        drawnItems.add(
            new DrawnItem(
                component.getName(),
                component.getComponent().getPluginId(),
                layoutResult.getPartNumber(),
                DrawnItem.DrawnItemType.ComponentItem,
                DrawnItem.Category.ChartSeriesLabel.name(),
                0,
                0,
                new LeanGeometry(
                    (int) (offSet.getX() + factX),
                    (int) (offSet.getY() + factY - seriesGeometry.getHeight()),
                    seriesGeometry.getWidth(),
                    seriesGeometry.getHeight()),
                new DrawnContext(seriesLabel)));
      }
    }
  }

  /**
   * Sort series points by ascending X and collapse duplicate X values (average Y) so that {@link
   * SplineInterpolator} does not throw {@code NonMonotonicSequenceException}.
   *
   * @return {@code [xs, ys]} with length &gt;= 0 and strictly increasing {@code xs}
   */
  private static double[][] sortStrictlyIncreasingX(
      List<Double> xCoordinates, List<Double> yCoordinates) {
    int n = xCoordinates.size();
    Integer[] order = new Integer[n];
    for (int i = 0; i < n; i++) {
      order[i] = i;
    }
    java.util.Arrays.sort(
        order, (a, b) -> Double.compare(xCoordinates.get(a), xCoordinates.get(b)));

    List<Double> xs = new ArrayList<>();
    List<Double> ys = new ArrayList<>();
    double lastX = Double.NaN;
    for (int i = 0; i < order.length; i++) {
      int idx = order[i];
      double x = xCoordinates.get(idx);
      double y = yCoordinates.get(idx);
      if (xs.isEmpty()) {
        xs.add(x);
        ys.add(y);
        lastX = x;
      } else if (x > lastX) {
        xs.add(x);
        ys.add(y);
        lastX = x;
      } else if (x == lastX) {
        // Keep a single knot for this X (average Y)
        int last = ys.size() - 1;
        ys.set(last, (ys.get(last) + y) / 2.0);
      }
    }
    double[] xa = new double[xs.size()];
    double[] ya = new double[ys.size()];
    for (int i = 0; i < xs.size(); i++) {
      xa[i] = xs.get(i);
      ya[i] = ys.get(i);
    }
    return new double[][] {xa, ya};
  }

  private static void drawStraightSegments(
      SVGGraphics2D gc, List<Double> xCoordinates, List<Double> yCoordinates) {
    if (xCoordinates == null || xCoordinates.size() < 2) {
      return;
    }
    int[] xs = new int[xCoordinates.size()];
    int[] ys = new int[yCoordinates.size()];
    for (int i = 0; i < xs.length; i++) {
      xs[i] = xCoordinates.get(i).intValue();
      ys[i] = yCoordinates.get(i).intValue();
    }
    gc.drawPolyline(xs, ys, xs.length);
  }
}
