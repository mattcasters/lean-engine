package org.lean.presentation.component.types.crosstab;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.apache.hop.core.exception.HopException;
import org.lean.core.gui.plugin.LeanWidgetType;
import org.lean.core.gui.plugin.LeanWidgetElement;
import org.apache.hop.core.row.IRowMeta;
import org.apache.hop.core.row.IValueMeta;
import org.apache.hop.metadata.api.HopMetadataProperty;
import org.lean.core.AggregationMethod;
import org.lean.core.LeanColorRGB;
import org.lean.core.LeanDimension;
import org.lean.core.LeanFact;
import org.lean.core.LeanFont;
import org.lean.core.exception.LeanException;
import org.lean.core.gui.form.LeanGuiFormConstants;
import org.lean.presentation.component.type.ILeanComponent;
import org.lean.presentation.component.type.LeanBaseComponent;
import org.lean.presentation.theme.LeanTheme;
import org.lean.render.IRenderContext;

@Getter
@Setter
public abstract class LeanBaseAggregatingComponent extends LeanBaseComponent
    implements ILeanComponent {

  public static final String GRANT_TOTAL_STRING = "___!GrandTotal!___";

  @LeanWidgetElement(
      order = "09000-horizontalDimensions",
      parentId = LeanGuiFormConstants.PARENT_PLUGIN,
      type = LeanWidgetType.TEXT,
      label = "Horizontal dimensions")
  @HopMetadataProperty
  protected List<LeanDimension> horizontalDimensions;

  @LeanWidgetElement(
      order = "09100-verticalDimensions",
      parentId = LeanGuiFormConstants.PARENT_PLUGIN,
      type = LeanWidgetType.TEXT,
      label = "Vertical dimensions")
  @HopMetadataProperty
  protected List<LeanDimension> verticalDimensions;

  @LeanWidgetElement(
      order = "09200-facts",
      parentId = LeanGuiFormConstants.PARENT_PLUGIN,
      type = LeanWidgetType.TEXT,
      label = "Facts")
  @HopMetadataProperty
  protected List<LeanFact> facts;

  @LeanWidgetElement(
      order = "09300-showingHorizontalTotals",
      parentId = LeanGuiFormConstants.PARENT_PLUGIN,
      type = LeanWidgetType.CHECKBOX,
      label = "Show horizontal totals?")
  @HopMetadataProperty
  protected boolean showingHorizontalTotals;

  @LeanWidgetElement(
      order = "09400-showingVerticalTotals",
      parentId = LeanGuiFormConstants.PARENT_PLUGIN,
      type = LeanWidgetType.CHECKBOX,
      label = "Show vertical totals?")
  @HopMetadataProperty
  protected boolean showingVerticalTotals;

  @LeanWidgetElement(
      order = "09500-horizontalDimensionsFont",
      parentId = LeanGuiFormConstants.PARENT_PLUGIN,
      type = LeanWidgetType.TEXT,
      label = "Horizontal dimensions font")
  @HopMetadataProperty
  private LeanFont horizontalDimensionsFont;

  @LeanWidgetElement(
      order = "09510-horizontalDimensionsColor",
      parentId = LeanGuiFormConstants.PARENT_PLUGIN,
      type = LeanWidgetType.TEXT,
      label = "Horizontal dimensions color")
  @HopMetadataProperty
  private LeanColorRGB horizontalDimensionsColor;

  @LeanWidgetElement(
      order = "09600-verticalDimensionsFont",
      parentId = LeanGuiFormConstants.PARENT_PLUGIN,
      type = LeanWidgetType.TEXT,
      label = "Vertical dimensions font")
  @HopMetadataProperty
  private LeanFont verticalDimensionsFont;

  @LeanWidgetElement(
      order = "09610-verticalDimensionsColor",
      parentId = LeanGuiFormConstants.PARENT_PLUGIN,
      type = LeanWidgetType.TEXT,
      label = "Vertical dimensions color")
  @HopMetadataProperty
  private LeanColorRGB verticalDimensionsColor;

  // Fields below are used to calculate.
  // Always make copies if you need to calculate the same component more than once.
  //
  @LeanWidgetElement(
      order = "09700-factsFont",
      parentId = LeanGuiFormConstants.PARENT_PLUGIN,
      type = LeanWidgetType.TEXT,
      label = "Facts font")
  @HopMetadataProperty
  private LeanFont factsFont;

  @LeanWidgetElement(
      order = "09710-factsColor",
      parentId = LeanGuiFormConstants.PARENT_PLUGIN,
      type = LeanWidgetType.TEXT,
      label = "Facts color")
  @HopMetadataProperty
  private LeanColorRGB factsColor;

  @LeanWidgetElement(
      order = "09800-titleFont",
      parentId = LeanGuiFormConstants.PARENT_PLUGIN,
      type = LeanWidgetType.TEXT,
      label = "Title font")
  @HopMetadataProperty
  private LeanFont titleFont;

  @LeanWidgetElement(
      order = "09810-titleColor",
      parentId = LeanGuiFormConstants.PARENT_PLUGIN,
      type = LeanWidgetType.TEXT,
      label = "Title color")
  @HopMetadataProperty
  private LeanColorRGB titleColor;

  @LeanWidgetElement(
      order = "09900-gridColor",
      parentId = LeanGuiFormConstants.PARENT_PLUGIN,
      type = LeanWidgetType.TEXT,
      label = "Grid color")
  @HopMetadataProperty
  private LeanColorRGB gridColor;

  @LeanWidgetElement(
      order = "09910-axisColor",
      parentId = LeanGuiFormConstants.PARENT_PLUGIN,
      type = LeanWidgetType.TEXT,
      label = "Axis color")
  @HopMetadataProperty
  private LeanColorRGB axisColor;

  @JsonIgnore protected transient List<Integer> horizontalDimensionIndexes;
  @JsonIgnore protected transient List<Integer> verticalDimensionIndexes;
  @JsonIgnore protected transient List<Integer> factIndexes;
  @JsonIgnore protected transient List<Map<List<String>, Object>> pivotMapList;
  @JsonIgnore protected transient List<Map<List<String>, Long>> countMapList;
  @JsonIgnore protected transient IRowMeta inputRowMeta;

  public LeanBaseAggregatingComponent() {
    this.horizontalDimensions = new ArrayList<>();
    this.verticalDimensions = new ArrayList<>();
    this.facts = new ArrayList<>();
  }

  public LeanBaseAggregatingComponent(String pluginId) {
    super(pluginId);
    this.horizontalDimensions = new ArrayList<>();
    this.verticalDimensions = new ArrayList<>();
    this.facts = new ArrayList<>();
  }

  public LeanBaseAggregatingComponent(String pluginId, LeanBaseAggregatingComponent c) {
    super(pluginId, c);
    this.horizontalDimensions = new ArrayList<>();
    for (LeanDimension d : c.horizontalDimensions) {
      this.horizontalDimensions.add(new LeanDimension(d));
    }
    this.verticalDimensions = new ArrayList<>();
    for (LeanDimension d : c.verticalDimensions) {
      this.verticalDimensions.add(new LeanDimension(d));
    }
    this.facts = new ArrayList<>();
    for (LeanFact f : c.facts) {
      this.facts.add(new LeanFact(f));
    }
    this.showingHorizontalTotals = c.showingHorizontalTotals;
    this.showingVerticalTotals = c.showingVerticalTotals;

    // Fonts and colors
    //
    this.horizontalDimensionsFont =
        c.horizontalDimensionsFont == null ? null : new LeanFont(c.horizontalDimensionsFont);
    this.horizontalDimensionsColor =
        c.horizontalDimensionsColor == null ? null : new LeanColorRGB(c.horizontalDimensionsColor);
    this.verticalDimensionsFont =
        c.verticalDimensionsFont == null ? null : new LeanFont(c.verticalDimensionsFont);
    this.verticalDimensionsColor =
        c.verticalDimensionsColor == null ? null : new LeanColorRGB(c.verticalDimensionsColor);
    this.factsFont = c.factsFont == null ? null : new LeanFont(c.factsFont);
    this.factsColor = c.factsColor == null ? null : new LeanColorRGB(c.factsColor);
    this.axisColor = c.axisColor == null ? null : new LeanColorRGB(c.axisColor);
    this.gridColor = c.gridColor == null ? null : new LeanColorRGB(c.gridColor);
    this.titleFont = c.titleFont == null ? null : new LeanFont(c.titleFont);
    this.titleColor = c.titleColor == null ? null : new LeanColorRGB(c.titleColor);

    // Clear transient fields
    //
    this.horizontalDimensionIndexes = null;
    this.verticalDimensionIndexes = null;
    this.factIndexes = null;
    this.pivotMapList = null;
    this.countMapList = null;
    this.inputRowMeta = null;
  }

  protected void pivotRow(IRowMeta rowMeta, Object[] rowData) throws LeanException {
    try {
      if (factIndexes == null) {
        determineColumnIndexes(rowMeta);
      }

      // What are all the aggregations that need to be calculated?
      // One of every cell so all the vertical and horizontal combinations
      // Then one for every dimension subtotal and total
      //
      List<List<String>> keysList = new ArrayList<>();

      // Determine the keys for the horizontal & vertical axes
      // We'll be aggregating the data based on these keys...
      //
      List<String> verticalKeys = new ArrayList<>();
      for (int index : verticalDimensionIndexes) {
        verticalKeys.add(rowMeta.getString(rowData, index));
      }

      List<String> horizontalKeys = new ArrayList<>();
      for (int index : horizontalDimensionIndexes) {
        horizontalKeys.add(rowMeta.getString(rowData, index));
      }

      List<String> allKeys = new ArrayList<>();
      allKeys.addAll(verticalKeys);
      allKeys.addAll(horizontalKeys);

      // No dimensions: just add "-"
      //
      if (allKeys.size() == 0) {
        allKeys.add("-");
      }

      // Add the main keys list to aggregate on
      //
      if (allKeys.size() > 0) {
        keysList.add(allKeys);
      }

      if (showingVerticalTotals) {
        // Also on the vertical dimensions for the line totals
        //
        if (verticalKeys.size() > 0) {
          keysList.add(verticalKeys);
        }
      }

      if (showingHorizontalTotals) {
        // Add the horizontal dimensions for the column totals
        //
        if (horizontalKeys.size() > 0) {
          keysList.add(horizontalKeys);
        }
      }

      if (showingVerticalTotals && showingHorizontalTotals) {
        keysList.add(Arrays.asList(GRANT_TOTAL_STRING));
      }

      for (List<String> keys : keysList) {

        if (facts.size() == 0) {
          Map<List<String>, Object> pivotMap = pivotMapList.get(0);
          Map<List<String>, Long> countMap = countMapList.get(0);
          pivotMap.put(keys, Double.valueOf(0.0));
          countMap.put(keys, Long.valueOf(0));
        } else {
          for (int i = 0; i < facts.size(); i++) {
            // Every fact is basically generating a completely different crosstab
            // with the same dimensions
            //
            Map<List<String>, Object> pivotMap = pivotMapList.get(i);
            Map<List<String>, Long> countMap = countMapList.get(i);

            IValueMeta valueMeta = rowMeta.getValueMeta(factIndexes.get(i));
            Object valueData = rowData[factIndexes.get(i)];
            LeanFact fact = facts.get(i);

            if (!valueMeta.isNull(valueData)) {
              // Count the values regardless...
              //
              Long count = countMap.get(keys);
              if (count == null) {
                count = 1L;
              } else {
                count++;
              }
              countMap.put(keys, count);

              //
              switch (valueMeta.getType()) {
                case IValueMeta.TYPE_NUMBER:
                  // Do some aggregation
                  Double numberValue = valueMeta.getNumber(valueData);
                  switch (fact.getAggregationMethod()) {
                    case SUM:
                    case AVERAGE:
                      Double previous = (Double) pivotMap.get(keys);
                      if (previous == null) {
                        pivotMap.put(keys, numberValue);
                      } else {
                        pivotMap.put(keys, numberValue + previous);
                      }
                      break;
                    case COUNT:
                      // Already handled
                      break;
                    default:
                      throw new LeanException(
                          "Number aggregation not supported yet: " + fact.getAggregationMethod());
                  }
                  break;
                case IValueMeta.TYPE_INTEGER:
                  Long integerValue = valueMeta.getInteger(valueData);
                  switch (fact.getAggregationMethod()) {
                    case SUM:
                    case AVERAGE:
                      Long previous = (Long) pivotMap.get(keys);
                      if (previous == null) {
                        pivotMap.put(keys, integerValue);
                      } else {
                        pivotMap.put(keys, integerValue + previous);
                      }
                      break;
                    case COUNT:
                      // Handled above
                      break;
                    default:
                      throw new LeanException(
                          "Integer aggregation not supported yet: " + fact.getAggregationMethod());
                  }
                  break;
                case IValueMeta.TYPE_BIGNUMBER:
                  BigDecimal bigValue = valueMeta.getBigNumber(valueData);
                  switch (fact.getAggregationMethod()) {
                    case SUM:
                    case AVERAGE:
                      BigDecimal previous = (BigDecimal) pivotMap.get(keys);
                      if (previous == null) {
                        pivotMap.put(keys, bigValue);
                      } else {
                        BigDecimal sum = bigValue.add(bigValue);
                        pivotMap.put(keys, sum);
                      }
                      break;
                    case COUNT:
                      // Handled above
                      break;
                    default:
                      throw new LeanException(
                          "BigNumber aggregation not supported yet: "
                              + fact.getAggregationMethod());
                  }
                  break;
                default:
                  if (fact.getAggregationMethod() != AggregationMethod.COUNT) {
                    throw new LeanException(
                        "Unsupported data type for aggregation : " + valueMeta.getName());
                  }
              }
            }
          }
        }
      }
    } catch (Exception e) {
      try {
        throw new LeanException("Unable to pivot row of data : " + rowMeta.getString(rowData), e);
      } catch (HopException ex) {
        throw new LeanException("Unable to pivot row of data", ex);
      }
    }
  }

  protected void determineColumnIndexes(IRowMeta rowMeta) throws LeanException {
    factIndexes = new ArrayList<>();
    horizontalDimensionIndexes = new ArrayList<>();
    verticalDimensionIndexes = new ArrayList<>();

    // calculate vertical dimension indexes
    //
    for (LeanDimension dimension : verticalDimensions) {
      int index = rowMeta.indexOfValue(dimension.getColumnName());
      if (index < 0) {
        throw new LeanException(
            "Vertical dimension column '" + dimension.getColumnName() + "' couldn't be found");
      }
      verticalDimensionIndexes.add(index);
    }

    // calculate horizontal dimension indexes
    //
    for (LeanDimension dimension : horizontalDimensions) {
      int index = rowMeta.indexOfValue(dimension.getColumnName());
      if (index < 0) {
        throw new LeanException(
            "Horizontal dimension column '" + dimension.getColumnName() + "' couldn't be found");
      }
      horizontalDimensionIndexes.add(index);
    }

    // Calculate fact column indexes and allocate pivot map hash maps
    //
    pivotMapList = new ArrayList<>();
    countMapList = new ArrayList<>();
    for (LeanFact column : facts) {
      int index = rowMeta.indexOfValue(column.getColumnName());
      if (index < 0) {
        throw new LeanException("Fact column '" + column.getColumnName() + "' couldn't be found");
      }
      factIndexes.add(index);

      // Add an empty hash map for every metric
      //
      pivotMapList.add(new HashMap<>());
      countMapList.add(new HashMap<>());
    }

    // No facts, still keep mapping dimensions
    //
    if (facts.size() == 0) {
      pivotMapList.add(new HashMap<>());
      countMapList.add(new HashMap<>());
    }

    // Remember rowMeta
    inputRowMeta = rowMeta;
  }

  protected void getCombinations(
      List<Set<String>> setsList,
      int index,
      Set<List<String>> combinations,
      List<String> currentRow) {
    if (setsList.size() == 0) {
      return;
    }
    if (index >= setsList.size()) {
      // add the current row to the set of combinations
      // Make a copy!
      //
      combinations.add(new ArrayList(currentRow));
      return;
    }

    // Consider all values in the horizontal dimension in the given column.
    //
    Set<String> values = setsList.get(index);
    for (String value : values) {
      currentRow.add(value);
      getCombinations(setsList, index + 1, combinations, currentRow);
      // Remove the last row
      currentRow.remove(currentRow.size() - 1);
    }
  }

  protected List<List<String>> sortCombinations(Set<List<String>> horizontalCombinations) {
    List<List<String>> sortedHorizontalCombinations = new ArrayList<>(horizontalCombinations);
    sortListOfListOfStrings(sortedHorizontalCombinations);
    return sortedHorizontalCombinations;
  }

  protected void sortListOfListOfStrings(List<List<String>> listOfListOfStrings) {
    Collections.sort(
        listOfListOfStrings,
        (list1, list2) -> {
          for (int i = 0; i < list1.size(); i++) {
            String one = list1.get(i);
            String two = list2.get(i);
            if (!one.equals(two)) {
              return one.compareTo(two);
            }
          }
          return 0;
        });
  }

  protected void calculateDistinctValues(
      List<Set<String>> horizontalValues, List<Set<String>> verticalValues) {
    for (int i = 0; i < horizontalDimensions.size(); i++) {
      horizontalValues.add(new HashSet<>());
    }
    for (int i = 0; i < verticalDimensions.size(); i++) {
      verticalValues.add(new HashSet<>());
    }

    // So we calculate distinct values for all dimensions...
    //
    for (Map<List<String>, Object> pivotMap : pivotMapList) {
      // So if we take the keys in the pivotMap, the first values
      // are the vertical dimensions.
      // Then we'll find the horizontal dimensions.
      //
      // HOWEVER, we need to sort and draw all distinct values.
      //
      for (List<String> keys : pivotMap.keySet()) {
        // Avoid picking up the aggregates
        //
        if (keys.size() == verticalValues.size() + horizontalValues.size()) {
          for (int i = 0; i < verticalValues.size(); i++) {
            // Create a unique list of values for the horizontal dimensions...
            //
            verticalValues.get(i).add(keys.get(i));
          }
          for (int i = 0; i < horizontalValues.size(); i++) {
            // Also get a list of unique values over the vertical dimensions...
            //
            horizontalValues.get(i).add(keys.get(verticalDimensions.size() + i));
          }
        }
      }
    }
  }

  protected LeanColorRGB lookupVerticalDimensionsColor(IRenderContext renderContext)
      throws LeanException {
    if (verticalDimensionsColor != null) {
      return verticalDimensionsColor;
    }
    LeanTheme theme = renderContext.lookupTheme(themeName);
    if (theme != null) {
      return theme.lookupVerticalDimensionsColor();
    }
    if (getDefaultColor() != null) {
      return getDefaultColor();
    }
    throw new LeanException(
        "No vertical dimensions color nor default color defined (no theme used or found)");
  }

  protected LeanFont lookupVerticalDimensionsFont(IRenderContext renderContext)
      throws LeanException {
    if (verticalDimensionsFont != null) {
      return verticalDimensionsFont;
    }
    LeanTheme theme = renderContext.lookupTheme(themeName);
    if (theme != null) {
      return theme.lookupVerticalDimensionsFont();
    }
    if (getDefaultFont() != null) {
      return getDefaultFont();
    }
    throw new LeanException(
        "No vertical dimensions font nor default font defined (no theme used or found)");
  }

  protected LeanColorRGB lookupHorizontalDimensionsColor(IRenderContext renderContext)
      throws LeanException {
    if (horizontalDimensionsColor != null) {
      return horizontalDimensionsColor;
    }
    LeanTheme theme = renderContext.lookupTheme(themeName);
    if (theme != null) {
      return theme.lookupHorizontalDimensionsColor();
    }
    if (getDefaultColor() != null) {
      return getDefaultColor();
    }
    throw new LeanException(
        "No horizontal dimensions color nor default color defined (no theme used or found)");
  }

  protected LeanFont lookupHorizontalDimensionsFont(IRenderContext renderContext)
      throws LeanException {
    if (horizontalDimensionsFont != null) {
      return horizontalDimensionsFont;
    }
    LeanTheme theme = renderContext.lookupTheme(themeName);
    if (theme != null) {
      return theme.lookupHorizontalDimensionsFont();
    }
    if (getDefaultFont() != null) {
      return getDefaultFont();
    }
    throw new LeanException(
        "No horizontal dimensions font nor default font defined (no theme used or found)");
  }

  protected LeanColorRGB lookupFactsColor(IRenderContext renderContext) throws LeanException {
    if (factsColor != null) {
      return factsColor;
    }
    LeanTheme theme = renderContext.lookupTheme(themeName);
    if (theme != null) {
      return theme.lookupFactsColor();
    }
    if (getDefaultColor() != null) {
      return getDefaultColor();
    }
    throw new LeanException("No facts color nor default color defined (no theme used or found)");
  }

  protected LeanFont lookupFactsFont(IRenderContext renderContext) throws LeanException {
    if (factsFont != null) {
      return factsFont;
    }
    LeanTheme theme = renderContext.lookupTheme(themeName);
    if (theme != null) {
      return theme.lookupFactsFont();
    }
    if (getDefaultFont() != null) {
      return getDefaultFont();
    }
    throw new LeanException("No facts font nor default font defined (no theme used or found)");
  }

  protected LeanColorRGB lookupTitleColor(IRenderContext renderContext) throws LeanException {
    if (titleColor != null) {
      return titleColor;
    }
    LeanTheme theme = renderContext.lookupTheme(themeName);
    if (theme != null) {
      return theme.lookupTitleColor();
    }
    if (getDefaultColor() != null) {
      return getDefaultColor();
    }
    throw new LeanException("No title color nor default color defined (no theme used or found)");
  }

  protected LeanFont lookupTitleFont(IRenderContext renderContext) throws LeanException {
    if (titleFont != null) {
      return titleFont;
    }
    LeanTheme theme = renderContext.lookupTheme(themeName);
    if (theme != null) {
      return theme.lookupTitleFont();
    }
    if (getDefaultFont() != null) {
      return getDefaultFont();
    }
    throw new LeanException("No title font nor default font defined (no theme used or found)");
  }

  protected LeanColorRGB lookupAxisColor(IRenderContext renderContext) throws LeanException {
    if (axisColor != null) {
      return axisColor;
    }
    LeanTheme theme = renderContext.lookupTheme(themeName);
    if (theme != null) {
      return theme.lookupAxisColor();
    }
    if (getDefaultColor() != null) {
      return getDefaultColor();
    }
    throw new LeanException("No axis color nor default color defined (no theme used or found)");
  }

  protected LeanColorRGB lookupGridColor(IRenderContext renderContext) throws LeanException {
    if (gridColor != null) {
      return gridColor;
    }
    LeanColorRGB color = null;
    LeanTheme theme = renderContext.lookupTheme(themeName);
    if (theme != null) {
      return theme.lookupGridColor();
    }
    if (getDefaultColor() != null) {
      return getDefaultColor();
    }
    throw new LeanException("No grid color nor default color defined (no theme used or found)");
  }
}
