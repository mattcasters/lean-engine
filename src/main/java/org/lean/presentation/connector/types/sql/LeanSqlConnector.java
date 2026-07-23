package org.lean.presentation.connector.types.sql;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.sql.ResultSet;
import lombok.Getter;
import lombok.Setter;
import org.apache.hop.core.database.Database;
import org.apache.hop.core.database.DatabaseMeta;
import org.apache.hop.core.logging.LoggingObject;
import org.apache.hop.core.row.IRowMeta;
import org.apache.hop.metadata.api.HopMetadataProperty;
import org.apache.hop.metadata.api.IHopMetadataSerializer;
import org.lean.core.LeanDatabaseConnection;
import org.lean.core.exception.LeanException;
import org.lean.core.gui.form.LeanGuiFormConstants;
import org.lean.core.gui.plugin.LeanComboSource;
import org.lean.core.gui.plugin.LeanWidgetElement;
import org.lean.core.gui.plugin.LeanWidgetType;
import org.lean.presentation.connector.type.ILeanConnector;
import org.lean.presentation.connector.type.LeanBaseConnector;
import org.lean.presentation.connector.type.LeanConnectorPlugin;
import org.lean.presentation.datacontext.IDataContext;

@JsonDeserialize(as = LeanSqlConnector.class)
@LeanConnectorPlugin(
    id = "SqlConnector",
    name = "Execute a SQL query",
    description = "Reads data from a relational database using a SQL query",
    image = "ui/images/connectors/sql.svg")
@Getter
@Setter
public class LeanSqlConnector extends LeanBaseConnector implements ILeanConnector {

  @LeanWidgetElement(
      order = "10000-databaseConnectionName",
      parentId = LeanGuiFormConstants.PARENT_PLUGIN,
      type = LeanWidgetType.COMBO,
      comboSource = LeanComboSource.METADATA,
      metadataKey = "lean-database-connection",
      label = "Database connection",
      toolTip = "Name of a lean-database-connection metadata element")
  @HopMetadataProperty
  private String databaseConnectionName;

  @LeanWidgetElement(
      order = "10100-sql",
      parentId = LeanGuiFormConstants.PARENT_PLUGIN,
      type = LeanWidgetType.MULTI_LINE_TEXT,
      multiLineTextHeight = 10,
      label = "SQL",
      toolTip = "SQL query executed against the selected database connection")
  @HopMetadataProperty
  private String sql;

  @JsonIgnore private transient ResultSet resultSet;

  public LeanSqlConnector() {
    super("SqlConnector");
  }

  public LeanSqlConnector(String databaseConnectionName, String sql) {
    this();
    this.databaseConnectionName = databaseConnectionName;
    this.sql = sql;
  }

  public LeanSqlConnector(LeanSqlConnector c) {
    super(c);
    this.databaseConnectionName = c.databaseConnectionName;
    this.sql = c.sql;
  }

  public LeanSqlConnector clone() {
    return new LeanSqlConnector(this);
  }

  @Override
  public IRowMeta describeOutput(IDataContext dataContext) throws LeanException {
    Database database = null;

    try {
      IHopMetadataSerializer<LeanDatabaseConnection> serializer =
          dataContext.getMetadataProvider().getSerializer(LeanDatabaseConnection.class);
      LeanDatabaseConnection databaseConnection = serializer.load(databaseConnectionName);

      DatabaseMeta databaseMeta = databaseConnection.createDatabaseMeta();
      database =
          new Database(
              new LoggingObject("Database connection '" + databaseConnectionName + "'"),
              dataContext.getVariables(),
              databaseMeta);
      database.connect();

      IRowMeta rowMeta = database.getQueryFields(sql, false);

      return rowMeta;
    } catch (Exception e) {
      throw new LeanException("Unable to describe output of SQL query", e);
    } finally {
      if (database != null) {
        database.disconnect();
      }
    }
  }

  /**
   * For the sampledata usecase we pass 100 rows with a few interesting data types...
   *
   * @param dataContext the data context to optionally reference (not used here)
   * @throws LeanException
   */
  @Override
  public void startStreaming(IDataContext dataContext) throws LeanException {

    Database database = null;
    try {
      IHopMetadataSerializer<LeanDatabaseConnection> serializer =
          dataContext.getMetadataProvider().getSerializer(LeanDatabaseConnection.class);
      LeanDatabaseConnection databaseConnection = serializer.load(databaseConnectionName);

      DatabaseMeta databaseMeta = databaseConnection.createDatabaseMeta();

      database =
          new Database(
              new LoggingObject("Database connection '" + databaseConnectionName + "'"),
              dataContext.getVariables(),
              databaseMeta);
      database.connect();

      resultSet = database.openQuery(sql);
      Object[] row = database.getRow(resultSet);
      while (row != null) {
        passToRowListeners(database.getReturnRowMeta(), row);
        row = database.getRow(resultSet);
      }
      database.closeQuery(resultSet);

      // Signal to all row listeners (and subsequent connectors) that no more rows are forthcoming .
      //
      outputDone();

    } catch (LeanException e) {
      // Row listeners (e.g. crosstab aggregation) throw LeanException mid-stream — keep the
      // original message so editors show the real cause, not only "Couldn't stream data…".
      throw e;
    } catch (Exception e) {
      throw new LeanException(
          "Couldn't stream data from database connection " + databaseConnectionName, e);
    } finally {
      if (database != null) {
        database.disconnect();
      }
    }
  }

  @Override
  public void waitUntilFinished() throws LeanException {
    // StartStreaming works synchronized, no need to get complicated about it
  }
}
