package org.lean.presentation.datacontext;

import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.hop.core.exception.HopException;
import org.apache.hop.core.variables.IVariables;
import org.apache.hop.core.variables.Variables;
import org.apache.hop.metadata.api.IHopMetadataProvider;
import org.lean.core.Constants;
import org.lean.core.exception.LeanException;
import org.lean.presentation.LeanPresentation;
import org.lean.presentation.connector.LeanConnector;
import lombok.Getter;
import lombok.Setter;

/** A data context with variables */
@Getter
@Setter
public class PresentationDataContext implements IDataContext {

  private LeanPresentation presentation;

  private IVariables variables;

  private IHopMetadataProvider metadataProvider;

  public PresentationDataContext(
      LeanPresentation presentation, IHopMetadataProvider metadataProvider) {
    this.presentation = presentation;
    this.metadataProvider = metadataProvider;
    variables = new Variables();

    variables.setVariable(Constants.VARIABLE_PRESENTATION_NAME, presentation.getName());
    variables.setVariable(
        Constants.VARIABLE_PRESENTATION_DESCRIPTION, presentation.getDescription());
    variables.setVariable(
        Constants.VARIABLE_SYSTEM_DATE, new SimpleDateFormat("yyyy/MM/dd").format(new Date()));
  }

  @Override
  public LeanConnector getConnector(String name) throws LeanException {
    LeanConnector connector = presentation.getConnector(name);
    if (connector == null) {
      // Try to load it from the metadata provider.
      //
      try {
        connector = metadataProvider.getSerializer(LeanConnector.class).load(name);
      } catch (HopException e) {
        throw new LeanException("Error loading Lean connector '" + name + "' from metadata", e);
      }
    }

    // Create a copy every time someone asks for a connector.
    // This ensures that querying is safe
    //
    if (connector != null) {
      connector = new LeanConnector(connector);
    }
    return connector;
  }
}
