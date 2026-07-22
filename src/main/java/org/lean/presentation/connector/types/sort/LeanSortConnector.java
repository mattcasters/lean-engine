package org.lean.presentation.connector.types.sort;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.apache.hop.core.exception.HopValueException;
import org.apache.hop.core.row.IRowMeta;
import org.apache.hop.core.row.IValueMeta;
import org.apache.hop.metadata.api.HopMetadataProperty;
import org.lean.core.ILeanRowListener;
import org.lean.core.LeanColumn;
import org.lean.core.LeanSortMethod;
import org.lean.core.exception.LeanException;
import org.lean.core.gui.form.LeanGuiFormConstants;
import org.lean.core.gui.plugin.LeanWidgetElement;
import org.lean.core.gui.plugin.LeanWidgetType;
import org.lean.presentation.connector.LeanConnector;
import org.lean.presentation.connector.type.ILeanConnector;
import org.lean.presentation.connector.type.LeanBaseConnector;
import org.lean.presentation.connector.type.LeanConnectorPlugin;
import org.lean.presentation.datacontext.IDataContext;
import lombok.Getter;
import lombok.Setter;

/** Sort rows from a source connector using a selection of columns */
@JsonDeserialize(as = LeanSortConnector.class)
@LeanConnectorPlugin(id = "SortConnector", name = "Sort rows", description = "Sorts all rows")
@Getter
@Setter
public class LeanSortConnector extends LeanBaseConnector implements ILeanConnector {

  @JsonIgnore protected ArrayBlockingQueue<Object> finishedQueue;

  @LeanWidgetElement(
      order = "10000-columns",
      parentId = LeanGuiFormConstants.PARENT_PLUGIN,
      type = LeanWidgetType.TEXT,
      label = "Sort columns")
  @HopMetadataProperty
  private List<LeanColumn> columns;

  @LeanWidgetElement(
      order = "10100-sortMethods",
      parentId = LeanGuiFormConstants.PARENT_PLUGIN,
      type = LeanWidgetType.TEXT,
      label = "Sort methods",
      toolTip = "One method per sort column (same list size)")
  @HopMetadataProperty
  private List<LeanSortMethod> sortMethods;

  public LeanSortConnector() {
    super("SortConnector");
    finishedQueue = null;
    columns = new ArrayList<>();
    sortMethods = new ArrayList<>();
  }

  public LeanSortConnector(List<LeanColumn> columns, List<LeanSortMethod> sortMethods) {
    this();
    this.columns = columns;
    this.sortMethods = sortMethods;
  }

  public LeanSortConnector(LeanSortConnector c) {
    super(c);
    columns = new ArrayList<>();
    for (LeanColumn column : c.columns) {
      this.columns.add(new LeanColumn(column));
    }
    sortMethods = new ArrayList<>();
    for (LeanSortMethod method : c.sortMethods) {
      this.sortMethods.add(new LeanSortMethod(method));
    }
  }

  public LeanSortConnector clone() {
    return new LeanSortConnector(this);
  }

  @Override
  public IRowMeta describeOutput(IDataContext dataContext) throws LeanException {
    LeanConnector connector = dataContext.getConnector(getSourceConnectorName());
    if (connector == null) {
      throw new LeanException(
          "Unable to find source '" + getSourceConnectorName() + "' for sort connector");
    }
    return connector.getConnector().describeOutput(dataContext);
  }

  @Override
  public void startStreaming(IDataContext dataContext) throws LeanException {
    LeanConnector connector = dataContext.getConnector(getSourceConnectorName());
    if (connector == null) {
      throw new LeanException(
          "Unable to find source '" + getSourceConnectorName() + "' for sort connector");
    }

    if (finishedQueue != null) {
      throw new LeanException(
          "Please don't start streaming twice in your application, wait until the connector has finished sending rows");
    }
    finishedQueue = new ArrayBlockingQueue<>(10);

    final IRowMeta inputRowMeta = connector.describeOutput(dataContext);
    final IRowMeta outputRowMeta = inputRowMeta.clone();

    final List<Object[]> rows = new ArrayList<>();
    final int[] fieldIndexes = new int[columns.size()];

    for (int i = 0; i < fieldIndexes.length; i++) {
      LeanColumn column = columns.get(i);
      LeanSortMethod sortMethod = sortMethods.get(i);
      fieldIndexes[i] = inputRowMeta.indexOfValue(column.getColumnName());
      if (fieldIndexes[i] < 0) {
        throw new LeanException(
            "Sort column '" + column.getColumnName() + "' could not be found in the input");
      }

      IValueMeta valueMeta = outputRowMeta.getValueMeta(fieldIndexes[i]);
      valueMeta.setSortedDescending(!sortMethod.isAscending());
      switch (sortMethod.getType()) {
        case NATIVE_VALUE:
          break;
        case STRING_ALPHA_CASE_INSENSITIVE:
          valueMeta.setCaseInsensitive(true);
          break;
        case STRING_ALPHA:
          valueMeta.setCaseInsensitive(false);
          break;
        default:
          throw new LeanException(
              "Sort method " + sortMethod.getType().name() + " is not yet implemented");
      }
    }

    ILeanRowListener listener =
        (rowMeta, rowData) -> {
          if (rowData == null) {
            try {
              Collections.sort(
                  rows,
                  (o1, o2) -> {
                    try {
                      return outputRowMeta.compare(o1, o2, fieldIndexes);
                    } catch (HopValueException e) {
                      throw new RuntimeException("Error comparing rows", e);
                    }
                  });
            } catch (Exception e) {
              throw new LeanException("Error sorting rows", e);
            }

            for (Object[] row : rows) {
              passToRowListeners(outputRowMeta, row);
            }
            rows.clear();
            outputDone();
            finishedQueue.add(new Object());
            return;
          }

          rows.add(rowData);
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
