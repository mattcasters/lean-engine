package org.lean.render.context;

import org.apache.commons.lang3.StringUtils;
import org.apache.hop.metadata.api.IHopMetadataProvider;
import org.lean.core.exception.LeanException;
import org.lean.presentation.LeanPresentation;
import org.lean.presentation.theme.LeanTheme;
import org.lean.render.IRenderContext;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
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
   * @param themeName The name of the theme to look for, or null/blank to use the presentation
   *     default (form editors often save "(none)" as {@code ""})
   * @return The theme or null if none is found.
   */
  @Override
  public LeanTheme lookupTheme(String themeName) throws LeanException {
    // null or blank → presentation default (empty string is common after Apply with theme "(none)")
    LeanTheme theme;
    if (StringUtils.isBlank(themeName)) {
      theme = presentation != null ? presentation.getDefaultTheme() : null;
    } else {
      theme = presentation != null ? presentation.lookupTheme(themeName) : null;
    }
    if (theme != null) {
      return theme;
    }
    // Named theme not embedded on the presentation: try metadata (only when a real name was given)
    if (StringUtils.isNotBlank(themeName)) {
      return super.lookupTheme(themeName);
    }
    // Still nothing: fall back to first theme on the presentation or built-in default
    if (presentation != null && presentation.getThemes() != null && !presentation.getThemes().isEmpty()) {
      return presentation.getThemes().get(0);
    }
    return LeanTheme.getDefault();
  }
}
