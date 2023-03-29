package org.lean.render.context;

import org.apache.hop.metadata.api.IHopMetadataProvider;
import org.lean.core.exception.LeanException;
import org.lean.presentation.LeanPresentation;
import org.lean.presentation.theme.LeanTheme;
import org.lean.render.IRenderContext;

public class PresentationRenderContext extends SimpleRenderContext implements IRenderContext {

  private LeanPresentation presentation;

  public PresentationRenderContext(IHopMetadataProvider metadataProvider) {
    super(metadataProvider);
  }

  public PresentationRenderContext(
      LeanPresentation presentation, IHopMetadataProvider metadataProvider) {
    this(metadataProvider);
    this.presentation = presentation;
  }

  /**
   * @param themeName The name of the theme to look for or null if you want to use the default
   * @return The theme or null if none is found.
   */
  @Override
  public LeanTheme lookupTheme(String themeName) throws LeanException {
    // If no theme name is given, them we'll use the default of the presentation
    //
    LeanTheme theme;
    if (themeName == null) {
      theme = presentation.getDefaultTheme();
    } else {
      theme = presentation.lookupTheme(themeName);
    }
    if (theme != null) {
      return theme;
    }
    return super.lookupTheme(themeName);
  }

  /**
   * Gets presentation
   *
   * @return value of presentation
   */
  public LeanPresentation getPresentation() {
    return presentation;
  }

  /**
   * @param presentation The presentation to set
   */
  public void setPresentation(LeanPresentation presentation) {
    this.presentation = presentation;
  }
}
