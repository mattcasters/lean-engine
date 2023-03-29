package org.lean.render.context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.hop.metadata.api.IHopMetadataProvider;
import org.lean.core.LeanColorRGB;
import org.lean.core.LeanSize;
import org.lean.core.exception.LeanException;
import org.lean.presentation.theme.LeanTheme;
import org.lean.render.IRenderContext;

public class SimpleRenderContext implements IRenderContext {
  private final IHopMetadataProvider metadataProvider;

  private LeanSize canvasSize;
  private List<LeanTheme> themes;

  private Map<String, Map<String, Integer>> themeValueColorMap;
  private Map<String, Integer> themeColorIndexMap;

  public SimpleRenderContext(IHopMetadataProvider metadataProvider) {
    this.metadataProvider = metadataProvider;
    themes = new ArrayList<>();
    themeValueColorMap = new HashMap<>();
    themeColorIndexMap = new HashMap<>();
  }

  public SimpleRenderContext(
      int width, int height, List<LeanTheme> themes, IHopMetadataProvider metadataProvider) {
    this(new LeanSize(width, height), themes, metadataProvider);
  }

  public SimpleRenderContext(
      LeanSize canvasSize, List<LeanTheme> themes, IHopMetadataProvider metadataProvider) {
    this(metadataProvider);
    this.canvasSize = canvasSize;
    this.themes = themes;
  }

  /**
   * Look for the theme scheme with the given name
   *
   * @param themeName the scheme name to look for
   * @return The theme scheme or null if nothing could be found
   */
  @Override
  public LeanTheme lookupTheme(String themeName) throws LeanException {
    for (LeanTheme theme : themes) {
      if (theme.getName().equalsIgnoreCase(themeName)) {
        return theme;
      }
    }
    // Try again in the metadata
    //
    try {
      return metadataProvider.getSerializer(LeanTheme.class).load(themeName);
    } catch (Exception e) {
      throw new LeanException("Error loading theme '" + themeName + "' from the metadata", e);
    }
  }

  @Override
  public LeanColorRGB getStableColor(String themeName, String value) throws LeanException {

    LeanTheme theme = lookupTheme(themeName);
    if (theme == null) {
      return null;
    }

    // Is the value found?
    //
    int colorIndex;
    Map<String, Integer> valueColorMap = themeValueColorMap.get(themeName);
    if (valueColorMap == null) {

      // First usage of this theme in the map.
      //
      valueColorMap = new HashMap<>();
      themeValueColorMap.put(themeName, valueColorMap);
      colorIndex = 0;
    } else {
      // Get the index of the last color used.
      //
      Integer index = valueColorMap.get(value);
      if (index == null) {
        // First time we see this value...
        // Get the last index used...
        //
        index = themeColorIndexMap.get(themeName);
        if (index == null) {
          throw new RuntimeException(
              "Index out of sync for theme '" + themeName + "' and value: '" + value + "'");
        }
        colorIndex = index + 1;
      } else {
        // We recognized the color, return the same index...
        //
        colorIndex = index;
      }
    }

    if (colorIndex >= theme.getColors().size()) {
      colorIndex = 0;
    }
    LeanColorRGB color = theme.getColors().get(colorIndex);

    // Save the last one used...
    //
    themeColorIndexMap.put(themeName, colorIndex);

    // Save the index for the value...
    //
    valueColorMap.put(value, colorIndex);

    return color;
  }

  /**
   * Gets canvasSize
   *
   * @return value of canvasSize
   */
  public LeanSize getCanvasSize() {
    return canvasSize;
  }

  /**
   * @param canvasSize The canvasSize to set
   */
  public void setCanvasSize(LeanSize canvasSize) {
    this.canvasSize = canvasSize;
  }

  /**
   * Gets themes
   *
   * @return value of themes
   */
  public List<LeanTheme> getThemes() {
    return themes;
  }

  /**
   * @param themes The themes to set
   */
  public void setThemes(List<LeanTheme> themes) {
    this.themes = themes;
  }

  /**
   * Gets metadataProvider
   *
   * @return value of metadataProvider
   */
  public IHopMetadataProvider getMetadataProvider() {
    return metadataProvider;
  }
}
