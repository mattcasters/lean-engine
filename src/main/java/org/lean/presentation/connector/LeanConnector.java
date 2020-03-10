package org.lean.presentation.connector;

import org.lean.core.ILeanRowListener;
import org.lean.core.exception.LeanException;
import org.lean.presentation.connector.type.ILeanConnector;
import org.lean.presentation.datacontext.IDataContext;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.hop.core.RowMetaAndData;
import org.apache.hop.core.row.RowMetaInterface;
import org.apache.hop.metastore.persist.MetaStoreAttribute;
import org.apache.hop.metastore.persist.MetaStoreElementType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

@MetaStoreElementType( name = "Connector", description = "Connector between components and data sources" )
public class LeanConnector {

  /** The name of the connector*/
  @JsonProperty
  private String name;

  @MetaStoreAttribute
  @JsonProperty
  private ILeanConnector connector;

  @MetaStoreAttribute
  @JsonProperty
  private boolean shared;

  public LeanConnector() {
  }

  public LeanConnector( String name, ILeanConnector connector ) {
    this();
    this.name = name;
    this.connector = connector;
  }

  /**
   * Create a new connector by copying over the details of the given connector
   * @param c
   */
  public LeanConnector( LeanConnector c ) {
    this.name = c.name;
    this.connector = c.connector==null ? null : c.connector.clone();
    this.shared = c.shared;
  }

  /**
   * Uses addDataListener() to retrieve all the rows from the data stream...
   *
   * @return all the rows from the connector
   *
   * @throws LeanException
   */
  public List<RowMetaAndData> retrieveRows( IDataContext dataContext ) throws LeanException {
    try {
      final List<RowMetaAndData> rows = new ArrayList<>();

      // Add a listener to the connector data
      // Whenever we get a row, we add it to the list...
      //
      final ArrayBlockingQueue<Object> finishedQueue = new ArrayBlockingQueue<>( 10 );
      ILeanRowListener listener = ( rowMeta, rowData ) -> {
        if ( rowData == null ) {
          finishedQueue.add( new Object() );
        } else {
          rows.add( new RowMetaAndData( rowMeta, rowData ) );
        }
      };
      connector.addRowListener( listener );

      // Start streaming data
      //
      connector.startStreaming(dataContext);

      // Wait for it to end.
      //
      while ( finishedQueue.poll( 1L, TimeUnit.DAYS ) == null );

      connector.waitUntilFinished();

      connector.removeDataListener(listener);

      return rows;
    } catch(Exception e) {
      throw new LeanException( "Error getting all rows from connector", e);
    }
  }

  public RowMetaInterface describeOutput(IDataContext dataContext) throws LeanException {
    return connector.describeOutput( dataContext );
  }

  public String getName() {
    return name;
  }

  public void setName( String name ) {
    this.name = name;
  }

  public ILeanConnector getConnector() {
    return connector;
  }

  public void setConnector( ILeanConnector connector ) {
    this.connector = connector;
  }

  public boolean isShared() {
    return shared;
  }

  public void setShared( boolean shared ) {
    this.shared = shared;
  }
}