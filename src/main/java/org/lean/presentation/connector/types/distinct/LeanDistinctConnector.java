package org.lean.presentation.connector.types.distinct;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.hop.core.exception.HopValueException;
import org.apache.hop.core.row.IRowMeta;
import org.lean.core.ILeanRowListener;
import org.lean.core.exception.LeanException;
import org.lean.presentation.connector.LeanConnector;
import org.lean.presentation.connector.type.ILeanConnector;
import org.lean.presentation.connector.type.LeanBaseConnector;
import org.lean.presentation.connector.type.LeanConnectorPlugin;
import org.lean.presentation.datacontext.IDataContext;

/**
 * Suppresses rows that are equal to the immediately previous row (adjacent-duplicate removal).
 *
 * <p>This is not a full-set DISTINCT: non-adjacent duplicates still pass. For global uniqueness,
 * sort first (e.g. via {@code SortConnector}) so equal rows are adjacent.
 */
@JsonDeserialize(as = LeanDistinctConnector.class)
@LeanConnectorPlugin(
    id = "DistinctConnector",
    name = "Select distinct rows",
    description =
        "Drops rows equal to the previous row (adjacent distinct); sort first for full uniqueness")
public class LeanDistinctConnector extends LeanBaseConnector implements ILeanConnector {

  @JsonIgnore protected ArrayBlockingQueue<Object> finishedQueue;

  public LeanDistinctConnector() {
    super("DistinctConnector");
    finishedQueue = null;
  }

  public LeanDistinctConnector(LeanDistinctConnector c) {
    super(c);
  }

  @Override
  public IRowMeta describeOutput(IDataContext dataContext) throws LeanException {
    LeanConnector connector = dataContext.getConnector(getSourceConnectorName());
    if (connector == null) {
      throw new LeanException(
          "Unable to find connector source '"
              + getSourceConnectorName()
              + "' for distinct connector");
    }
    return connector.getConnector().describeOutput(dataContext);
  }

  public LeanDistinctConnector clone() {
    return new LeanDistinctConnector(this);
  }

  @Override
  public void startStreaming(IDataContext dataContext) throws LeanException {
    LeanConnector connector = dataContext.getConnector(getSourceConnectorName());
    if (connector == null) {
      throw new LeanException(
          "Unable to find connector source '"
              + getSourceConnectorName()
              + "' for distinct connector");
    }

    if (finishedQueue != null) {
      throw new LeanException(
          "Please don't start streaming twice in your application, wait until the connector has finished sending rows");
    }
    finishedQueue = new ArrayBlockingQueue<>(10);

    AtomicBoolean firstRow = new AtomicBoolean(true);
    ILeanRowListener listener =
        new ILeanRowListener() {
          private Object[] previousRow = null;

          @Override
          public void rowReceived(IRowMeta rowMeta, Object[] rowData) throws LeanException {
            if (rowData == null) {
              outputDone();
              finishedQueue.add(new Object());
              return;
            }

            if (firstRow.get()) {
              passToRowListeners(rowMeta, rowData);
              firstRow.set(false);
            } else {
              int result;
              try {
                result = rowMeta.compare(rowData, previousRow);
              } catch (HopValueException e) {
                throw new LeanException("Error comparing rows of data", e);
              }
              if (result != 0) {
                passToRowListeners(rowMeta, rowData);
              }
            }

            previousRow = rowData;
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
