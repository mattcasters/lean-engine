package org.lean.presentation.component.types.chart;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.awt.BasicStroke;
import java.awt.Stroke;
import java.awt.geom.Arc2D;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.apache.batik.svggen.SVGGraphics2D;
import org.apache.commons.lang3.StringUtils;
import org.apache.hop.core.Const;
import org.apache.hop.core.exception.HopException;
import org.apache.hop.core.exception.HopValueException;
import org.apache.hop.core.row.IValueMeta;
import org.apache.hop.metadata.api.HopMetadataProperty;
import org.lean.core.LeanColorRGB;
import org.lean.core.LeanDimension;
import org.lean.core.LeanFact;
import org.lean.core.LeanGeometry;
import org.lean.core.LeanPosition;
import org.lean.core.LeanSize;
import org.lean.core.LeanTextGeometry;
import org.lean.core.draw.DrawnContext;
import org.lean.core.draw.DrawnItem;
import org.lean.core.exception.LeanException;
import org.lean.core.gui.form.LeanGuiFormConstants;
import org.lean.core.gui.plugin.LeanWidgetElement;
import org.lean.core.gui.plugin.LeanWidgetType;
import org.lean.presentation.LeanComponentLayoutResult;
import org.lean.presentation.LeanPresentation;
import org.lean.presentation.component.LeanComponent;
import org.lean.presentation.component.type.ILeanComponent;
import org.lean.presentation.component.type.LeanComponentPlugin;
import org.lean.presentation.component.types.chart.PieChartDetails.PieSlice;
import org.lean.presentation.component.types.crosstab.LeanBaseAggregatingComponent;
import org.lean.presentation.connector.LeanConnector;
import org.lean.presentation.datacontext.IDataContext;
import org.lean.presentation.layout.LeanLayoutResults;
import org.lean.presentation.page.LeanPage;
import org.lean.presentation.theme.LeanTheme;
import org.lean.render.IRenderContext;

/**
 * Pie / donut chart component. Slices come from horizontal dimension combinations; values from a
 * single fact aggregation. Vertical dimensions are ignored in v1.
 */
@JsonDeserialize(as = LeanPieChartComponent.class)
@LeanComponentPlugin(
    id = "LeanPieChartComponent",
    name = "Pie Chart",
    description = "A pie or donut chart component",
    image = "ui/images/components/pie-chart.svg")
@Getter
@Setter
public class LeanPieChartComponent extends LeanBaseAggregatingComponent implements ILeanComponent {

  /** Minimum slice share (0–100) required to draw an on-slice label when labels are enabled. */
  private static final double MIN_LABEL_PERCENT = 5.0;

  @LeanWidgetElement(
      order = "10000-title",
      parentId = LeanGuiFormConstants.PARENT_PLUGIN,
      type = LeanWidgetType.TEXT,
      label = "Title")
  @HopMetadataProperty
  protected String title;

  @LeanWidgetElement(
      order = "10100-horizontalMargin",
      parentId = LeanGuiFormConstants.PARENT_PLUGIN,
      type = LeanWidgetType.TEXT,
      label = "Horizontal margin")
  @HopMetadataProperty
  protected int horizontalMargin;

  @LeanWidgetElement(
      order = "10200-verticalMargin",
      parentId = LeanGuiFormConstants.PARENT_PLUGIN,
      type = LeanWidgetType.TEXT,
      label = "Vertical margin")
  @HopMetadataProperty
  protected int verticalMargin;

  @LeanWidgetElement(
      order = "10300-showingLegend",
      parentId = LeanGuiFormConstants.PARENT_PLUGIN,
      type = LeanWidgetType.CHECKBOX,
      label = "Show legend?")
  @HopMetadataProperty
  protected boolean showingLegend;

  /**
   * Legend placement relative to the pie: {@code RIGHT} (default) or {@code BOTTOM}.
   */
  @LeanWidgetElement(
      order = "10400-legendPosition",
      parentId = LeanGuiFormConstants.PARENT_PLUGIN,
      type = LeanWidgetType.TEXT,
      label = "Legend position (RIGHT/BOTTOM)")
  @HopMetadataProperty
  protected String legendPosition;

  @LeanWidgetElement(
      order = "10500-showingSliceLabels",
      parentId = LeanGuiFormConstants.PARENT_PLUGIN,
      type = LeanWidgetType.CHECKBOX,
      label = "Show slice labels?")
  @HopMetadataProperty
  protected boolean showingSliceLabels;

  @LeanWidgetElement(
      order = "10600-showingPercentages",
      parentId = LeanGuiFormConstants.PARENT_PLUGIN,
      type = LeanWidgetType.CHECKBOX,
      label = "Show percentages?")
  @HopMetadataProperty
  protected boolean showingPercentages;

  @LeanWidgetElement(
      order = "10700-showingFactValues",
      parentId = LeanGuiFormConstants.PARENT_PLUGIN,
      type = LeanWidgetType.CHECKBOX,
      label = "Show fact values?")
  @HopMetadataProperty
  protected boolean showingFactValues;

  /**
   * Inner hole as percent of outer radius (0 = full pie, 50 = typical donut). Clamped to [0, 90].
   */
  @LeanWidgetElement(
      order = "10800-innerRadiusPercent",
      parentId = LeanGuiFormConstants.PARENT_PLUGIN,
      type = LeanWidgetType.TEXT,
      label = "Inner radius % (0=pie)")
  @HopMetadataProperty
  protected String innerRadiusPercent;

  /**
   * Arc2D start angle in degrees for the first slice (0 = east, −90 = top).
   */
  @LeanWidgetElement(
      order = "10900-startAngleDegrees",
      parentId = LeanGuiFormConstants.PARENT_PLUGIN,
      type = LeanWidgetType.TEXT,
      label = "Start angle (degrees)")
  @HopMetadataProperty
  protected String startAngleDegrees;

  @LeanWidgetElement(
      order = "11000-clockwise",
      parentId = LeanGuiFormConstants.PARENT_PLUGIN,
      type = LeanWidgetType.CHECKBOX,
      label = "Clockwise?")
  @HopMetadataProperty
  protected boolean clockwise;

  @JsonIgnore protected transient String titleText;
  @JsonIgnore protected transient double actualInnerRadiusPercent;
  @JsonIgnore protected transient double actualStartAngleDegrees;

  public LeanPieChartComponent() {
    this((String) null);
  }

  public LeanPieChartComponent(String connectorName) {
    super("LeanPieChartComponent");
    this.sourceConnectorName = connectorName;
    this.horizontalDimensions = new ArrayList<>();
    this.verticalDimensions = new ArrayList<>();
    this.facts = new ArrayList<>();
    this.horizontalMargin = 10;
    this.verticalMargin = 10;
    this.showingLegend = true;
    this.legendPosition = "RIGHT";
    this.showingSliceLabels = true;
    this.showingPercentages = true;
    this.showingFactValues = false;
    this.innerRadiusPercent = "0";
    this.startAngleDegrees = "-90";
    this.clockwise = true;
  }

  public LeanPieChartComponent(LeanPieChartComponent c) {
    super("LeanPieChartComponent", c);
    this.title = c.title;
    this.horizontalMargin = c.horizontalMargin;
    this.verticalMargin = c.verticalMargin;
    this.showingLegend = c.showingLegend;
    this.legendPosition = c.legendPosition;
    this.showingSliceLabels = c.showingSliceLabels;
    this.showingPercentages = c.showingPercentages;
    this.showingFactValues = c.showingFactValues;
    this.innerRadiusPercent = c.innerRadiusPercent;
    this.startAngleDegrees = c.startAngleDegrees;
    this.clockwise = c.clockwise;
    this.themeName = c.themeName;
  }

  @Override
  public LeanPieChartComponent clone() {
    return new LeanPieChartComponent(this);
  }

  @Override
  public void processSourceData(
      LeanPresentation presentation,
      LeanPage page,
      LeanComponent component,
      IDataContext dataContext,
      IRenderContext renderContext,
      LeanLayoutResults results)
      throws LeanException {

    titleText =
        dataContext != null && dataContext.getVariables() != null
            ? dataContext.getVariables().resolve(title)
            : title;

    String innerPctStr =
        dataContext != null && dataContext.getVariables() != null
            ? dataContext.getVariables().resolve(innerRadiusPercent)
            : innerRadiusPercent;
    actualInnerRadiusPercent = Math.max(0, Math.min(90, Const.toDouble(innerPctStr, 0)));

    String startStr =
        dataContext != null && dataContext.getVariables() != null
            ? dataContext.getVariables().resolve(startAngleDegrees)
            : startAngleDegrees;
    actualStartAngleDegrees = Const.toDouble(startStr, -90);

    if (StringUtils.isBlank(sourceConnectorName)) {
      return;
    }

    LeanConnector connector = dataContext.getConnector(sourceConnectorName);
    if (connector == null) {
      return;
    }

    try {
      validateSettings();
    } catch (LeanException e) {
      return;
    }

    connector
        .getConnector()
        .addRowListener(
            (rowMeta, rowData) -> {
              if (rowData != null) {
                pivotRow(rowMeta, rowData);
              }
            });

    connector.getConnector().startStreaming(dataContext);
    connector.getConnector().waitUntilFinished();
  }

  protected void validateSettings() throws LeanException {
    if (horizontalDimensions == null || horizontalDimensions.isEmpty()) {
      throw new LeanException("A pie chart needs at least one horizontal (category) dimension");
    }
    for (LeanDimension dimension : horizontalDimensions) {
      if (StringUtils.isEmpty(dimension.getColumnName())) {
        throw new LeanException("No column name given for a horizontal dimension");
      }
    }
    if (facts == null || facts.isEmpty()) {
      throw new LeanException("A pie chart needs exactly one fact");
    }
    if (facts.size() > 1) {
      throw new LeanException("Only 1 fact is supported for pie charts at this time");
    }
    LeanFact fact = facts.get(0);
    if (StringUtils.isEmpty(fact.getColumnName())) {
      throw new LeanException("No column name given for a fact");
    }
    if (fact.getAggregationMethod() == null) {
      throw new LeanException(
          "No aggregation method specified for fact column '" + fact.getColumnName() + "'");
    }
  }

  protected boolean isIncompleteChartConfig() {
    return StringUtils.isBlank(sourceConnectorName)
        || facts == null
        || facts.isEmpty()
        || horizontalDimensions == null
        || horizontalDimensions.isEmpty()
        || pivotMapList == null;
  }

  @Override
  public LeanSize getExpectedSize(
      LeanPresentation presentation,
      LeanPage page,
      LeanComponent component,
      IDataContext dataContext,
      IRenderContext renderContext,
      LeanLayoutResults results) {

    if (StringUtils.isBlank(sourceConnectorName)
        || facts == null
        || facts.isEmpty()
        || horizontalDimensions == null
        || horizontalDimensions.isEmpty()) {
      return new LeanSize(400, 260);
    }
    return null;
  }

  protected void renderIncompletePlaceholder(
      SVGGraphics2D gc, LeanGeometry componentGeometry, IRenderContext renderContext)
      throws LeanException {
    drawBackGround(gc, componentGeometry, renderContext);
    boolean oldBorder = border;
    border = true;
    try {
      drawBorder(gc, componentGeometry, renderContext);
    } finally {
      border = oldBorder;
    }
    enableColor(gc, lookupDefaultColor(renderContext));
    enableFont(gc, lookupDefaultFont(renderContext));
    String msg =
        StringUtils.isNotBlank(titleText)
            ? titleText
            : (StringUtils.isNotBlank(title) ? title : "Pie Chart");
    String hint =
        StringUtils.isBlank(sourceConnectorName)
            ? msg + " (set connector)"
            : msg + " (configure dimensions/facts)";
    gc.drawString(hint, componentGeometry.getX() + 8, componentGeometry.getY() + 20);
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

    if (isIncompleteChartConfig()) {
      renderIncompletePlaceholder(gc, componentGeometry, renderContext);
      return;
    }

    PieChartDetails details = calculateDetails(gc, x, y, width, height, renderContext);

    drawBackGround(gc, componentGeometry, renderContext);
    drawBorder(gc, componentGeometry, renderContext);

    // Title
    if (StringUtils.isNotEmpty(titleText) && details.titleGeometry != null) {
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
                  (int) (offSet.getX() + titleX),
                  (int) (offSet.getY() + titleY - details.titleGeometry.getHeight()),
                  details.titleGeometry.getWidth(),
                  details.titleGeometry.getHeight()),
              new DrawnContext(titleText)));
    }

    // Empty data: outline only
    if (details.total <= 0 || details.slices.isEmpty()) {
      enableColor(gc, lookupDefaultColor(renderContext));
      Stroke oldStroke = gc.getStroke();
      gc.setStroke(new BasicStroke(1.5f));
      gc.draw(
          new Arc2D.Double(
              details.cx - details.outerRadius,
              details.cy - details.outerRadius,
              details.outerRadius * 2,
              details.outerRadius * 2,
              0,
              360,
              Arc2D.OPEN));
      if (details.innerRadius > 0) {
        gc.draw(
            new Arc2D.Double(
                details.cx - details.innerRadius,
                details.cy - details.innerRadius,
                details.innerRadius * 2,
                details.innerRadius * 2,
                0,
                360,
                Arc2D.OPEN));
      }
      gc.setStroke(oldStroke);
      drawLegend(gc, details, theme, renderContext, component, layoutResult, offSet, drawnItems);
      return;
    }

    String themeNameKey = theme != null ? theme.getName() : null;
    Stroke oldStroke = gc.getStroke();
    gc.setStroke(new BasicStroke(1.0f));

    for (int i = 0; i < details.slices.size(); i++) {
      PieSlice slice = details.slices.get(i);

      LeanColorRGB color = null;
      try {
        color = renderContext.getStableColor(themeNameKey, slice.label);
      } catch (LeanException e) {
        // fall through
      }
      if (color == null) {
        color = lookupDefaultColor(renderContext);
      }
      if (color == null) {
        color = LeanColorRGB.BLACK;
      }
      enableColor(gc, color);

      GeneralPath path = createSlicePath(details, slice);
      gc.fill(path);
      enableColor(gc, lookupDefaultColor(renderContext));
      gc.draw(path);

      // Slice hit region (bounding box of the pie area is coarse but consistent with other charts)
      double midDeg = slice.startAngleDegrees + slice.extentDegrees / 2.0;
      double midRad = Math.toRadians(midDeg);
      double hitR = (details.outerRadius + details.innerRadius) / 2.0;
      if (hitR <= 0) {
        hitR = details.outerRadius * 0.65;
      }
      int hx = (int) Math.round(details.cx + Math.cos(midRad) * hitR);
      int hy = (int) Math.round(details.cy - Math.sin(midRad) * hitR);
      int box = Math.max(12, (int) (details.outerRadius * 0.25));

      drawnItems.add(
          new DrawnItem(
              component.getName(),
              component.getComponent().getPluginId(),
              layoutResult.getPartNumber(),
              DrawnItem.DrawnItemType.ComponentItem,
              DrawnItem.Category.ChartLabel.name(),
              i,
              0,
              new LeanGeometry(
                  (int) (offSet.getX() + hx - box / 2),
                  (int) (offSet.getY() + hy - box / 2),
                  box,
                  box),
              new DrawnContext(slice.label)));
    }

    // On-slice labels
    if (showingSliceLabels) {
      enableFont(gc, lookupFactsFont(renderContext));
      enableColor(gc, lookupFactsColor(renderContext));
      for (PieSlice slice : details.slices) {
        if (slice.percentage < MIN_LABEL_PERCENT) {
          continue;
        }
        if (StringUtils.isEmpty(slice.displayLabel) || slice.labelGeometry == null) {
          continue;
        }
        double midDeg = slice.startAngleDegrees + slice.extentDegrees / 2.0;
        double midRad = Math.toRadians(midDeg);
        double labelR =
            details.innerRadius > 0
                ? (details.innerRadius + details.outerRadius) / 2.0
                : details.outerRadius * 0.62;
        int lx =
            (int)
                Math.round(
                    details.cx
                        + Math.cos(midRad) * labelR
                        - slice.labelGeometry.getWidth() / 2.0);
        int ly =
            (int)
                Math.round(
                    details.cy
                        - Math.sin(midRad) * labelR
                        + slice.labelGeometry.getHeight() / 3.0);
        gc.drawString(slice.displayLabel, lx, ly);
      }
    }

    gc.setStroke(oldStroke);
    drawLegend(gc, details, theme, renderContext, component, layoutResult, offSet, drawnItems);
  }

  private GeneralPath createSlicePath(PieChartDetails details, PieSlice slice) {
    double ox = details.cx - details.outerRadius;
    double oy = details.cy - details.outerRadius;
    double od = details.outerRadius * 2;

    GeneralPath path = new GeneralPath();
    if (details.innerRadius <= 0.5) {
      // Full pie wedge
      Arc2D pie =
          new Arc2D.Double(
              ox, oy, od, od, slice.startAngleDegrees, slice.extentDegrees, Arc2D.PIE);
      path.append(pie, false);
    } else {
      double ix = details.cx - details.innerRadius;
      double iy = details.cy - details.innerRadius;
      double id = details.innerRadius * 2;
      Arc2D outer =
          new Arc2D.Double(
              ox, oy, od, od, slice.startAngleDegrees, slice.extentDegrees, Arc2D.OPEN);
      Arc2D inner =
          new Arc2D.Double(
              ix,
              iy,
              id,
              id,
              slice.startAngleDegrees + slice.extentDegrees,
              -slice.extentDegrees,
              Arc2D.OPEN);
      path.append(outer, false);
      path.append(inner, true);
      path.closePath();
    }
    return path;
  }

  private void drawLegend(
      SVGGraphics2D gc,
      PieChartDetails details,
      LeanTheme theme,
      IRenderContext renderContext,
      LeanComponent component,
      LeanComponentLayoutResult layoutResult,
      LeanPosition offSet,
      List<DrawnItem> drawnItems)
      throws LeanException {

    if (!showingLegend || details.legendLabels.isEmpty()) {
      return;
    }

    double legendX = details.legendAreaX;
    double legendY = details.legendAreaY;
    double legendEntryWidth =
        details.maxLegendLabelWidth + 2 * horizontalMargin + details.legendMarkerSize;
    // Center legend horizontally when it has fewer columns than available width allows
    if (details.maxNrLegendColumns > details.nrLegendColumns && details.nrLegendColumns > 0) {
      double emptySpace =
          (details.maxNrLegendColumns - details.nrLegendColumns) * legendEntryWidth;
      legendX += emptySpace / 2;
    }

    String themeNameKey = theme != null ? theme.getName() : null;
    enableFont(gc, lookupDefaultFont(renderContext));

    int colNr = 0;
    int rowNr = 0;
    for (int i = 0; i < details.legendLabels.size(); i++) {
      String seriesLabel = details.legendLabels.get(i);

      double labelX = legendX + colNr * legendEntryWidth;
      double labelY = legendY + rowNr * (details.maxLegendLabelHeight + verticalMargin);

      LeanColorRGB color = null;
      try {
        color = renderContext.getStableColor(themeNameKey, seriesLabel);
      } catch (LeanException e) {
        // ignore
      }
      if (color == null) {
        color = lookupDefaultColor(renderContext);
      }
      if (color == null) {
        color = LeanColorRGB.BLACK;
      }
      enableColor(gc, color);

      gc.fillOval(
          (int) labelX,
          (int) (labelY + (details.maxLegendLabelHeight - details.legendMarkerSize) / 2.0),
          details.legendMarkerSize,
          details.legendMarkerSize);

      enableColor(gc, lookupDefaultColor(renderContext));
      gc.drawString(
          seriesLabel,
          (int) (labelX + details.legendMarkerSize + horizontalMargin / 2.0),
          (int) (labelY + details.maxLegendLabelHeight));

      drawnItems.add(
          new DrawnItem(
              component.getName(),
              component.getComponent().getPluginId(),
              layoutResult.getPartNumber(),
              DrawnItem.DrawnItemType.ComponentItem,
              DrawnItem.Category.LegendEntry.name(),
              i,
              0,
              new LeanGeometry(
                  (int) (offSet.getX() + labelX),
                  (int) (offSet.getY() + labelY),
                  (int) legendEntryWidth,
                  details.maxLegendLabelHeight + verticalMargin),
              new DrawnContext(seriesLabel)));

      colNr++;
      if (colNr >= details.nrLegendColumns) {
        colNr = 0;
        rowNr++;
      }
    }
  }

  protected PieChartDetails calculateDetails(
      SVGGraphics2D gc, int x, int y, int width, int height, IRenderContext renderContext)
      throws LeanException {

    PieChartDetails details = new PieChartDetails(x, y, width, height);

    List<Set<String>> horizontalValues = new ArrayList<>();
    List<Set<String>> verticalValues = new ArrayList<>();
    calculateDistinctValues(horizontalValues, verticalValues);

    Set<List<String>> horizontalCombinations = new HashSet<>();
    getCombinations(horizontalValues, 0, horizontalCombinations, new ArrayList<>());
    List<List<String>> sortedHorizontal = sortCombinations(horizontalCombinations);

    enableFont(gc, lookupDefaultFont(renderContext));

    if (StringUtils.isNotEmpty(titleText)) {
      details.titleGeometry = calculateTextGeometry(gc, titleText);
      details.titleHeight = details.titleGeometry.getHeight() + verticalMargin;
    } else {
      details.titleHeight = 0;
    }

    if (facts == null || facts.isEmpty()) {
      throw new LeanException("We need at least 1 fact to work with");
    }
    LeanFact fact = facts.get(0);
    int factIndex = 0;

    IValueMeta valueMeta = inputRowMeta.getValueMeta(factIndexes.get(factIndex)).clone();
    if (fact.getFormatMask() != null) {
      valueMeta.setConversionMask(fact.getFormatMask());
    }

    details.total = 0;
    List<PieSlice> rawSlices = new ArrayList<>();

    for (List<String> combination : sortedHorizontal) {
      String label = getCombinationString(combination);
      // Pivot keys are vertical dimensions then horizontal; vertical is empty in v1.
      List<String> factLookupKey = new ArrayList<>(combination);
      Object valueData = pivotMapList.get(factIndex).get(factLookupKey);

      double factValue = 0;
      try {
        if (valueData != null) {
          Double number = valueMeta.getNumber(valueData);
          if (number != null) {
            factValue = number;
          }
        }
      } catch (HopException e) {
        throw new LeanException("Data conversion error for pie slice '" + label + "'", e);
      }

      // Skip non-positive values (nulls already 0; negatives ignored)
      if (factValue <= 0) {
        continue;
      }

      String formatted;
      try {
        formatted = Const.NVL(valueMeta.getString(valueData), "-");
      } catch (HopValueException e) {
        formatted = String.valueOf(factValue);
      }

      PieSlice slice = new PieSlice();
      slice.label = label;
      slice.value = factValue;
      slice.formattedValue = formatted;
      rawSlices.add(slice);
      details.total += factValue;
    }

    // Angles
    double angle = actualStartAngleDegrees;
    for (PieSlice slice : rawSlices) {
      slice.percentage = details.total > 0 ? (slice.value / details.total) * 100.0 : 0;
      double extent = details.total > 0 ? 360.0 * (slice.value / details.total) : 0;
      if (clockwise) {
        extent = -extent;
      }
      slice.startAngleDegrees = angle;
      slice.extentDegrees = extent;
      angle += extent;

      // Build display label
      StringBuilder display = new StringBuilder();
      if (showingSliceLabels) {
        display.append(slice.label);
      }
      if (showingFactValues) {
        if (display.length() > 0) {
          display.append(": ");
        }
        display.append(slice.formattedValue);
      }
      if (showingPercentages) {
        if (display.length() > 0) {
          display.append(" ");
        }
        display.append(String.format("%.0f%%", slice.percentage));
      }
      slice.displayLabel = display.toString();
      if (StringUtils.isNotEmpty(slice.displayLabel)) {
        slice.labelGeometry = calculateTextGeometry(gc, slice.displayLabel);
      }
      details.slices.add(slice);
    }

    // Legend metrics from slice labels
    if (showingLegend) {
      for (PieSlice slice : details.slices) {
        details.legendLabels.add(slice.label);
        LeanTextGeometry geo = calculateTextGeometry(gc, slice.label);
        details.legendLabelGeos.add(geo);
        if (geo.getWidth() > details.maxLegendLabelWidth) {
          details.maxLegendLabelWidth = geo.getWidth();
        }
        if (geo.getHeight() > details.maxLegendLabelHeight) {
          details.maxLegendLabelHeight = geo.getHeight();
        }
      }
    }
    details.legendMarkerSize =
        details.maxLegendLabelHeight > 0 ? details.maxLegendLabelHeight * 2 / 3 : 8;

    boolean legendRight = isLegendRight();

    int contentTop = y + verticalMargin + details.titleHeight;
    int contentBottom = y + height - verticalMargin;
    int contentLeft = x + horizontalMargin;
    int contentRight = x + width - horizontalMargin;
    int contentWidth = contentRight - contentLeft;
    int contentHeight = contentBottom - contentTop;

    if (showingLegend && !details.legendLabels.isEmpty()) {
      if (legendRight) {
        // Reserve right column for legend
        int legendColWidth =
            details.maxLegendLabelWidth + 2 * horizontalMargin + details.legendMarkerSize;
        details.legendWidth = legendColWidth;
        details.nrLegendColumns = 1;
        details.maxNrLegendColumns = 1;
        details.nrLegendRows = details.legendLabels.size();
        details.legendHeight =
            details.nrLegendRows * (details.maxLegendLabelHeight + verticalMargin);
        details.legendAreaX = contentRight - legendColWidth;
        details.legendAreaY = contentTop + Math.max(0, (contentHeight - details.legendHeight) / 2.0);

        contentRight = (int) details.legendAreaX - horizontalMargin;
        contentWidth = Math.max(0, contentRight - contentLeft);
      } else {
        // BOTTOM legend
        details.legendWidth = contentWidth;
        details.maxNrLegendColumns =
            Math.max(
                1,
                (int)
                    Math.floor(
                        (double) details.legendWidth
                            / (details.maxLegendLabelWidth
                                + 2 * horizontalMargin
                                + details.legendMarkerSize)));
        details.nrLegendColumns =
            Math.min(details.legendLabels.size(), details.maxNrLegendColumns);
        if (details.nrLegendColumns > 0) {
          details.nrLegendRows =
              1 + (details.legendLabels.size() - 1) / details.nrLegendColumns;
        } else {
          details.nrLegendRows = 0;
        }
        details.legendHeight =
            details.nrLegendRows * (details.maxLegendLabelHeight + verticalMargin);
        details.legendAreaX = contentLeft;
        details.legendAreaY = contentBottom - details.legendHeight;
        contentBottom = (int) details.legendAreaY - verticalMargin;
        contentHeight = Math.max(0, contentBottom - contentTop);
      }
    }

    // Fit pie in remaining content box
    double diameter = Math.min(contentWidth, contentHeight);
    details.outerRadius = Math.max(1, diameter / 2.0 - 2);
    details.innerRadius = details.outerRadius * (actualInnerRadiusPercent / 100.0);
    details.cx = contentLeft + contentWidth / 2.0;
    details.cy = contentTop + contentHeight / 2.0;

    return details;
  }

  private boolean isLegendRight() {
    return legendPosition == null
        || legendPosition.isBlank()
        || "RIGHT".equalsIgnoreCase(legendPosition.trim());
  }

  protected String getCombinationString(List<String> combinationList) {
    StringBuilder combo = new StringBuilder();
    for (String combination : combinationList) {
      if (combo.length() > 0) {
        combo.append("-");
      }
      combo.append(combination);
    }
    return combo.toString();
  }
}
