package org.lean.presentation.connector.types.filter;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.apache.hop.core.exception.HopException;
import org.apache.hop.core.row.IRowMeta;
import org.apache.hop.core.row.IValueMeta;
import org.apache.hop.core.variables.IVariables;
import org.lean.core.exception.LeanException;
import org.lean.presentation.connector.LeanConnector;
import org.lean.presentation.connector.type.ILeanConnector;
import org.lean.presentation.connector.type.LeanBaseConnector;
import org.lean.presentation.datacontext.IDataContext;

/** Simple filter for rows with specific field values */
@JsonDeserialize(as = LeanSimpleFilterConnector.class)
public class LeanSimpleFilterConnector extends LeanBaseConnector implements ILeanConnector {

  @JsonIgnore protected ArrayBlockingQueue<Object> finishedQueue;
  private List<SimpleFilterValue> filterValues;

  public LeanSimpleFilterConnector() {
    super("SimpleFilterConnector");
    filterValues = new ArrayList<>();
    finishedQueue = null;
  }

  public LeanSimpleFilterConnector(List<SimpleFilterValue> filterValues) {
    this();
    this.filterValues = filterValues;
  }

  public LeanSimpleFilterConnector(LeanSimpleFilterConnector c) {
    super(c);
    this.filterValues = new ArrayList<>();
    for (SimpleFilterValue value : c.filterValues) {
      this.filterValues.add(new SimpleFilterValue(value.getFieldName(), value.getFilterValue()));
    }
  }

  public LeanSimpleFilterConnector clone() {
    return new LeanSimpleFilterConnector(this);
  }

  @Override
  public IRowMeta describeOutput(IDataContext dataContext) throws LeanException {
    LeanConnector connector = dataContext.getConnector(getSourceConnectorName());
    if (connector == null) {
      throw new LeanException(
          "Unable to find connector source '"
              + getSourceConnectorName()
              + "' for simple filter connector");
    }
    IRowMeta sourceRowMeta = connector.getConnector().describeOutput(dataContext);
    return sourceRowMeta;
  }

  @Override
  public void startStreaming(final IDataContext dataContext) throws LeanException {
    // which connector do we read from?
    //
    LeanConnector connector = dataContext.getConnector(getSourceConnectorName());
    if (connector == null) {
      throw new LeanException(
          "Unable to find connector source '"
              + getSourceConnectorName()
              + "' for simple filter connector");
    }

    if (finishedQueue != null) {
      throw new LeanException(
          "Please don't start streaming twice in your application, wait until the connector has finished sending rows");
    }
    finishedQueue = new ArrayBlockingQueue<>(10);

    // What does the input look like?
    //
    final IRowMeta inputRowMeta = connector.describeOutput(dataContext);
    final IVariables variables = dataContext.getVariables();

    // What are the simple filter row indexes?
    //
    Map<String, Set<String>> fieldFiltersMap = new HashMap<>();

    int[] valueIndexes = new int[filterValues.size()];
    for (int i = 0; i < valueIndexes.length; i++) {
      SimpleFilterValue filterValue = filterValues.get(i);
      valueIndexes[i] = inputRowMeta.indexOfValue(filterValue.getFieldName());
      if (valueIndexes[i] < 0) {
        throw new LeanException(
            "Unable to find filter field '"
                + filterValue.getFieldName()
                + "' in input of connector '"
                + getSourceConnectorName());
      }
      IValueMeta valueMeta = inputRowMeta.getValueMeta(valueIndexes[i]);
      String valueName = valueMeta.getName();

      Set<String> values = fieldFiltersMap.computeIfAbsent(valueName, e -> new HashSet<>());
      values.add(variables.resolve(filterValue.getFilterValue()));
    }

    // Add a row listener to the parent connector
    //
    connector
        .getConnector()
        .addRowListener(
            (rowMeta, rowData) -> {
              if (rowData == null) {
                outputDone();
                finishedQueue.add(new Object());
                return;
              }

              boolean pass = true;
              for (int i = 0; i < valueIndexes.length; i++) {
                SimpleFilterValue simpleFilterValue = filterValues.get(i);
                int valueIndex = valueIndexes[i];

                IValueMeta valueMeta = inputRowMeta.getValueMeta(valueIndex);

                // What are the filter values for this field?
                //
                Set<String> filterValues =
                    fieldFiltersMap.computeIfAbsent(valueMeta.getName(), e -> new HashSet<>());

                try {
                  String rowValue = valueMeta.getString(rowData[valueIndex]);

                  if (!filterValues.contains(rowValue)) {
                    pass = false;

                    // stop looking
                    break;
                  }
                } catch (HopException e) {
                  throw new LeanException(
                      "Unable to convert simple filter input row value '" + valueMeta.toString(),
                      e);
                }
              }

              if (pass) {
                passToRowListeners(rowMeta, rowData);
              }
            });

    // Now signal start streaming...
    //
    connector.getConnector().startStreaming(dataContext);
  }

  @Override
  public void waitUntilFinished() throws LeanException {
    try {
      while (finishedQueue.poll(1, TimeUnit.DAYS) == null) {
        // This loop will stop once the last row is read in the row listener and an object is added
        // to the queue.
      }
    } catch (InterruptedException e) {
      throw new LeanException("Interrupted while waiting for more rows in connector", e);
    }
    finishedQueue = null;
  }

  /**
   * Gets filterValues
   *
   * @return value of filterValues
   */
  public List<SimpleFilterValue> getFilterValues() {
    return filterValues;
  }

  /**
   * @param filterValues The filterValues to set
   */
  public void setFilterValues(List<SimpleFilterValue> filterValues) {
    this.filterValues = filterValues;
  }
}
