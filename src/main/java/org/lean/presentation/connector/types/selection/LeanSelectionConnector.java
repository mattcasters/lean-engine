package org.lean.presentation.connector.types.selection;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.StringUtils;
import org.apache.hop.core.row.IRowMeta;
import org.apache.hop.core.row.IValueMeta;
import org.apache.hop.core.row.RowDataUtil;
import org.apache.hop.core.row.RowMeta;
import org.apache.hop.metadata.api.HopMetadataProperty;
import org.lean.core.ILeanRowListener;
import org.lean.core.LeanColumn;
import org.lean.core.exception.LeanException;
import org.lean.presentation.connector.LeanConnector;
import org.lean.presentation.connector.type.ILeanConnector;
import org.lean.presentation.connector.type.LeanBaseConnector;
import org.lean.presentation.connector.type.LeanConnectorPlugin;
import org.lean.presentation.datacontext.IDataContext;

/** Select a bunch of columns from a source connector */
@JsonDeserialize(as = LeanSelectionConnector.class)
@LeanConnectorPlugin(
    id = "SelectionConnector",
    name = "Select fields",
    description = "Makes a selection of fields from a source connector")
public class LeanSelectionConnector extends LeanBaseConnector implements ILeanConnector {

  @JsonIgnore protected ArrayBlockingQueue<Object> finishedQueue;
  @HopMetadataProperty private List<LeanColumn> columns;

  public LeanSelectionConnector() {
    super("SelectionConnector");
    finishedQueue = null;
    columns = new ArrayList<>();
  }

  public LeanSelectionConnector(LeanSelectionConnector c) {
    super(c);
    this.columns = new ArrayList<>();
    for (LeanColumn column : c.columns) {
      this.columns.add(new LeanColumn(column));
    }
  }

  public LeanSelectionConnector(List<LeanColumn> columns) {
    this();
    this.columns = columns;
  }

  public LeanSelectionConnector clone() {
    return new LeanSelectionConnector(this);
  }

  @Override
  public IRowMeta describeOutput(IDataContext dataContext) throws LeanException {
    LeanConnector connector = dataContext.getConnector(getSourceConnectorName());
    if (connector == null) {
      throw new LeanException(
          "Unable to find connector source '"
              + getSourceConnectorName()
              + "' for selection connector");
    }
    IRowMeta sourceRowMeta = connector.getConnector().describeOutput(dataContext);

    IRowMeta rowMeta = new RowMeta();
    for (LeanColumn column : columns) {
      IValueMeta sourceValueMeta = sourceRowMeta.searchValueMeta(column.getColumnName());
      if (sourceValueMeta == null) {
        throw new LeanException(
            "Unable to find column selection '"
                + column.getColumnName()
                + "' in source connector : "
                + getSourceConnectorName()
                + " : "
                + rowMeta.toString());
      }
      IValueMeta valueMeta = sourceValueMeta.clone();

      if (StringUtils.isNotEmpty(column.getFormatMask())) {
        valueMeta.setConversionMask(column.getFormatMask());
      }
      valueMeta.setOrigin(getSourceConnectorName());
      valueMeta.setComments(column.getHeaderValue());
      rowMeta.addValueMeta(valueMeta);
    }

    return rowMeta;
  }

  @Override
  public void startStreaming(IDataContext dataContext) throws LeanException {
    LeanConnector connector = dataContext.getConnector(getSourceConnectorName());
    if (connector == null) {
      throw new LeanException(
          "Unable to find connector source '"
              + getSourceConnectorName()
              + "' for selection connector");
    }

    if (finishedQueue != null) {
      throw new LeanException(
          "Please don't start streaming twice in your application, wait until the connector has finished sending rows");
    }
    finishedQueue = new ArrayBlockingQueue<>(10);

    final IRowMeta inputRowMeta = connector.describeOutput(dataContext);
    final IRowMeta outputRowMeta = describeOutput(dataContext);

    final int[] columnIndexes = new int[columns.size()];
    for (int i = 0; i < columnIndexes.length; i++) {
      columnIndexes[i] = inputRowMeta.indexOfValue(columns.get(i).getColumnName());
    }

    ILeanRowListener listener =
        (rowMeta, rowData) -> {
          if (rowData == null) {
            outputDone();
            finishedQueue.add(new Object());
            return;
          }

          Object[] outputRowData = RowDataUtil.allocateRowData(outputRowMeta.size());
          for (int i = 0; i < outputRowMeta.size(); i++) {
            outputRowData[i] = rowData[columnIndexes[i]];
          }

          passToRowListeners(outputRowMeta, outputRowData);
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

  public List<LeanColumn> getColumns() {
    return columns;
  }

  public void setColumns(List<LeanColumn> columns) {
    this.columns = columns;
  }
}
