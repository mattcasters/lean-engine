package org.lean.presentation.connector.types.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.hop.core.variables.IVariables;
import org.apache.hop.core.variables.Variables;
import org.apache.hop.metadata.api.IHopMetadataProvider;
import org.apache.hop.metadata.api.IHopMetadataSerializer;
import org.apache.hop.metadata.serializer.memory.MemoryMetadataProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lean.core.LeanDatabaseConnection;
import org.lean.core.LeanEnvironment;
import org.lean.core.exception.LeanException;
import org.lean.presentation.connector.LeanConnector;
import org.lean.presentation.datacontext.IDataContext;
import org.lean.util.BasePresentationUtil;
import org.lean.util.TablePresentationUtil;

class LeanSqlConnectorTest {

  private static final int ROW_COUNT = 50;
  private static final String TABLE_NAME = "SQL_TEST_TABLE";

  private IHopMetadataProvider metadataProvider;
  private IVariables variables;
  private LeanDatabaseConnection connection;

  @BeforeEach
  void setUp() throws Exception {
    metadataProvider = new MemoryMetadataProvider();
    variables = Variables.getADefaultVariableSpace();
    LeanEnvironment.init();
    BasePresentationUtil.registerTestPlugins();

    IHopMetadataSerializer<LeanDatabaseConnection> dbSerializer =
        metadataProvider.getSerializer(LeanDatabaseConnection.class);
    connection = TablePresentationUtil.populateTestTable(variables, TABLE_NAME, ROW_COUNT);
    dbSerializer.save(connection);
  }

  @Test
  void streamsAllRowsFromSqlQuery() throws Exception {
    String sql = "SELECT * FROM " + TABLE_NAME;
    final LeanSqlConnector leanSqlConnector = new LeanSqlConnector(connection.getName(), sql);

    AtomicInteger rowCounter = new AtomicInteger(0);
    AtomicBoolean endReceived = new AtomicBoolean(false);

    leanSqlConnector.addRowListener(
        (rowMeta, rowData) -> {
          if (rowMeta != null && rowData != null) {
            rowCounter.incrementAndGet();
          }
          if (rowMeta == null && rowData == null) {
            endReceived.set(true);
          }
        });

    IDataContext dataContext =
        new IDataContext() {
          @Override
          public LeanConnector getConnector(String name) throws LeanException {
            return new LeanConnector(name, leanSqlConnector);
          }

          @Override
          public IVariables getVariables() {
            return Variables.getADefaultVariableSpace();
          }

          @Override
          public IHopMetadataProvider getMetadataProvider() {
            return metadataProvider;
          }
        };

    leanSqlConnector.startStreaming(dataContext);
    leanSqlConnector.waitUntilFinished();

    assertTrue(endReceived.get());
    assertEquals(ROW_COUNT, rowCounter.get());
  }
}
