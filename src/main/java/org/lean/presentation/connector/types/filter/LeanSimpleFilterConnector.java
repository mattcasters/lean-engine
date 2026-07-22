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
import org.lean.core.ILeanRowListener;
import org.lean.core.exception.LeanException;
import org.lean.presentation.connector.LeanConnector;
import org.lean.presentation.connector.type.ILeanConnector;
import org.lean.presentation.connector.type.LeanBaseConnector;
import org.lean.presentation.datacontext.IDataContext;
import lombok.Getter;
import lombok.Setter;

/**
 * Filters rows by exact string equality on field values.
 *
 * <p>When multiple {@link SimpleFilterValue}s are configured, a row must match <strong>all</strong>
 * of them (logical AND). For a single field, multiple allowed values act as OR within that field
 * (the row value must be in the set of filter values for that field).
 */
@JsonDeserialize(as = LeanSimpleFilterConnector.class)
@Getter
@Setter
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
    return connector.getConnector().describeOutput(dataContext);
  }

  @Override
  public void startStreaming(final IDataContext dataContext) throws LeanException {
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

    final IRowMeta inputRowMeta = connector.describeOutput(dataContext);
    final IVariables variables = dataContext.getVariables();

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

    ILeanRowListener listener =
        (rowMeta, rowData) -> {
          if (rowData == null) {
            outputDone();
            finishedQueue.add(new Object());
            return;
          }

          boolean pass = true;
          for (int i = 0; i < valueIndexes.length; i++) {
            int valueIndex = valueIndexes[i];
            IValueMeta valueMeta = inputRowMeta.getValueMeta(valueIndex);
            Set<String> allowed =
                fieldFiltersMap.computeIfAbsent(valueMeta.getName(), e -> new HashSet<>());

            try {
              String rowValue = valueMeta.getString(rowData[valueIndex]);
              if (!allowed.contains(rowValue)) {
                pass = false;
                break;
              }
            } catch (HopException e) {
              throw new LeanException(
                  "Unable to convert simple filter input row value '" + valueMeta, e);
            }
          }

          if (pass) {
            passToRowListeners(rowMeta, rowData);
          }
        };

    ILeanConnector source = connector.getConnector();
    attachToSource(source, listener);
    source.startStreaming(dataContext);
  }

  @Override
  public void waitUntilFinished() throws LeanException {
    try {
      while (finishedQueue != null && finishedQueue.poll(1, TimeUnit.DAYS) == null) {
        // wait for end-of-stream signal
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new LeanException("Interrupted while waiting for more rows in connector", e);
    } finally {
      detachFromSource();
      finishedQueue = null;
    }
  }
}
