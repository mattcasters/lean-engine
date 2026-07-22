package org.lean.presentation.datacontext;

import org.apache.hop.core.variables.IVariables;
import org.apache.hop.core.variables.Variables;
import org.apache.hop.metadata.api.IHopMetadataProvider;
import org.lean.core.Constants;
import org.lean.core.exception.LeanException;
import org.lean.presentation.connector.LeanConnector;
import org.lean.presentation.layout.LeanRenderPage;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RenderPageDataContext implements IDataContext {

  private IDataContext parentDataContext;
  private LeanRenderPage renderPage;

  private IVariables variableSpace;

  public RenderPageDataContext(IDataContext parentDataContext, LeanRenderPage renderPage) {
    this.parentDataContext = parentDataContext;
    this.renderPage = renderPage;

    variableSpace = new Variables();
    variableSpace.copyFrom(parentDataContext.getVariables());

    // Inject page specific variables
    //
    variableSpace.setVariable(
        Constants.VARIABLE_PAGE_NUMBER, Integer.toString(renderPage.getPageNumber()));
  }

  @Override
  public LeanConnector getConnector(String name) throws LeanException {
    LeanConnector connector = parentDataContext.getConnector(name);

    // Create a copy every time someone asks for a connector.
    // This ensures that querying is safe
    //
    if (connector != null) {
      connector = new LeanConnector(connector);
    }
    return connector;
  }


  /**
   * Gets variableSpace
   *
   * @return value of variableSpace
   */
  @Override
  public IVariables getVariables() {
    return variableSpace;
  }


  @Override
  public IHopMetadataProvider getMetadataProvider() {
    return parentDataContext.getMetadataProvider();
  }
}
