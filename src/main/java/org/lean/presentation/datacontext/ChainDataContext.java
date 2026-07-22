package org.lean.presentation.datacontext;

import org.apache.hop.core.variables.IVariables;
import org.apache.hop.metadata.api.IHopMetadataProvider;
import org.lean.core.exception.LeanException;
import org.lean.presentation.connector.LeanConnector;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChainDataContext implements IDataContext {

  private IDataContext parentDataContext;

  private Map<String, LeanConnector> connectorsMap;

  private LeanConnector lastConnector;

  public ChainDataContext() {
    connectorsMap = new HashMap<>();
  }

  public ChainDataContext(IDataContext parentDataContext) {
    this.parentDataContext = parentDataContext;
    connectorsMap = new HashMap<>();
  }

  @Override
  public LeanConnector getConnector(String name) throws LeanException {
    LeanConnector connector = parentDataContext.getConnector(name);
    if (connector == null) {
      connector = connectorsMap.get(name);
    }
    if (connector != null) {
      connector = new LeanConnector(connector);
    }
    return connector;
  }

  @Override
  public IVariables getVariables() {
    return parentDataContext.getVariables();
  }

  public void addConnector(LeanConnector leanConnector) {
    this.lastConnector = leanConnector;
    connectorsMap.put(leanConnector.getName(), leanConnector);
  }


  @Override
  public IHopMetadataProvider getMetadataProvider() {
    return parentDataContext.getMetadataProvider();
  }
}
