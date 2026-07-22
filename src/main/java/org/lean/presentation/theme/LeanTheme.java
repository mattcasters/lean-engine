package org.lean.presentation.theme;

import org.apache.hop.metadata.api.HopMetadata;
import org.apache.hop.metadata.api.HopMetadataBase;
import org.apache.hop.metadata.api.HopMetadataProperty;
import org.apache.hop.metadata.api.IHopMetadata;
import org.lean.core.Constants;
import org.lean.core.LeanColorRGB;
import org.lean.core.LeanFont;
import org.lean.core.exception.LeanException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@HopMetadata(
    key = "theme",
    name = "Lean Theme",
    description = "A theme with colors and fonts to use as default in the components")
@Getter
@Setter
public class LeanTheme extends HopMetadataBase implements IHopMetadata {

  @HopMetadataProperty protected String description;

  @HopMetadataProperty protected List<LeanColorRGB> colors;

  @HopMetadataProperty protected LeanColorRGB backgroundColor;

  @HopMetadataProperty protected LeanColorRGB defaultColor;

  @HopMetadataProperty protected LeanFont defaultFont;

  @HopMetadataProperty protected LeanColorRGB borderColor;

  @HopMetadataProperty protected LeanFont horizontalDimensionsFont;

  @HopMetadataProperty protected LeanColorRGB horizontalDimensionsColor;

  @HopMetadataProperty protected LeanFont verticalDimensionsFont;

  @HopMetadataProperty protected LeanColorRGB verticalDimensionsColor;

  @HopMetadataProperty protected LeanFont factsFont;

  @HopMetadataProperty protected LeanColorRGB factsColor;

  @HopMetadataProperty protected LeanFont titleFont;

  @HopMetadataProperty protected LeanColorRGB titleColor;

  @HopMetadataProperty protected LeanColorRGB axisColor;

  @HopMetadataProperty protected LeanColorRGB gridColor;

  @HopMetadataProperty private boolean shared;

  public LeanTheme() {
    colors = new ArrayList<>();
  }

  public LeanTheme(String name, String description, List<LeanColorRGB> colors) {
    this.name = name;
    this.description = description;
    this.colors = colors;
    this.backgroundColor = null;
    this.defaultColor = null;
    this.defaultFont = null;
    this.borderColor = null;
  }

  public LeanTheme(LeanTheme s) {
    this();
    this.name = s.name;
    this.description = s.description;
    for (LeanColorRGB color : s.getColors()) {
      colors.add(new LeanColorRGB(color));
    }
    this.backgroundColor = s.backgroundColor == null ? null : new LeanColorRGB(s.backgroundColor);
    this.defaultColor = s.defaultColor == null ? null : new LeanColorRGB(s.defaultColor);
    this.defaultFont = s.defaultFont == null ? null : new LeanFont(s.defaultFont);
    this.borderColor = s.borderColor == null ? null : new LeanColorRGB(s.borderColor);
    this.horizontalDimensionsFont =
        s.horizontalDimensionsFont == null ? null : new LeanFont(s.horizontalDimensionsFont);
    this.horizontalDimensionsColor =
        s.horizontalDimensionsColor == null ? null : new LeanColorRGB(s.horizontalDimensionsColor);
    this.verticalDimensionsFont =
        s.verticalDimensionsFont == null ? null : new LeanFont(s.verticalDimensionsFont);
    this.verticalDimensionsColor =
        s.verticalDimensionsColor == null ? null : new LeanColorRGB(s.verticalDimensionsColor);
    this.factsFont = s.factsFont == null ? null : new LeanFont(s.factsFont);
    this.factsColor = s.factsColor == null ? null : new LeanColorRGB(s.factsColor);
    this.titleFont = s.titleFont == null ? null : new LeanFont(s.titleFont);
    this.titleColor = s.titleColor == null ? null : new LeanColorRGB(s.titleColor);
    this.axisColor = s.axisColor == null ? null : new LeanColorRGB(s.axisColor);
    this.gridColor = s.gridColor == null ? null : new LeanColorRGB(s.gridColor);
  }

  public static final LeanTheme getDefault() {
    LeanTheme theme = new LeanTheme();

    theme.setName(Constants.DEFAULT_THEME_NAME);
    theme.setDescription(Constants.DEFAULT_THEME_DESCRIPTION);

    theme.getColors().clear();
    theme
        .getColors()
        .addAll(
            Arrays.asList(
                new LeanColorRGB("#003f5c"),
                new LeanColorRGB("#2f4b7c"),
                new LeanColorRGB("#665191"),
                new LeanColorRGB("#a05195"),
                new LeanColorRGB("#d45087"),
                new LeanColorRGB("#f95d6a"),
                new LeanColorRGB("#ff7c43"),
                new LeanColorRGB("#ffa600")));

    theme.setBackgroundColor(new LeanColorRGB("#ffffff")); // Simply white
    theme.setDefaultColor(new LeanColorRGB("#000000")); // Simply black
    theme.setDefaultFont(new LeanFont("Arial", "12", false, false));
    theme.setBorderColor(new LeanColorRGB("#f0f0f0")); // very light gray

    theme.setHorizontalDimensionsFont(new LeanFont("Arial", "12", true, false));
    theme.setHorizontalDimensionsColor(new LeanColorRGB("#000000"));
    theme.setVerticalDimensionsFont(new LeanFont("Arial", "12", true, false));
    theme.setVerticalDimensionsColor(new LeanColorRGB("#000000"));
    theme.setFactsFont(new LeanFont("Hack", "12", false, false));
    theme.setFactsColor(new LeanColorRGB("#000000"));
    theme.setTitleFont(new LeanFont("Arial", "10", true, true));
    theme.setTitleColor(new LeanColorRGB("#c8c8c8"));
    theme.setAxisColor(new LeanColorRGB("#000000"));
    theme.setGridColor(new LeanColorRGB("#c8c8c8"));

    return theme;
  }

  public LeanColorRGB lookupDefaultColor() throws LeanException {
    if (defaultColor == null) {
      throw new LeanException("No default color defined in theme '" + name + "'");
    }
    return defaultColor;
  }

  public LeanFont lookupDefaultFont() throws LeanException {
    if (defaultFont == null) {
      throw new LeanException("No default font defined in theme '" + name + "'");
    }
    return defaultFont;
  }

  public LeanColorRGB lookupBackgroundColor() throws LeanException {
    if (backgroundColor == null && defaultColor == null) {
      throw new LeanException(
          "No background color nor default color defined in theme '" + name + "'");
    }
    if (backgroundColor != null) {
      return backgroundColor;
    }
    return LeanColorRGB.WHITE;
  }

  public LeanColorRGB lookupBorderColor() throws LeanException {
    if (borderColor == null && defaultColor == null) {
      throw new LeanException("No border color nor default color defined in theme '" + name + "'");
    }
    if (borderColor != null) {
      return borderColor;
    }
    return defaultColor;
  }

  public LeanColorRGB lookupHorizontalDimensionsColor() throws LeanException {
    if (horizontalDimensionsColor == null && defaultColor == null) {
      throw new LeanException(
          "No horizontal dimensions color nor default color defined in theme '" + name + "'");
    }
    if (horizontalDimensionsColor != null) {
      return horizontalDimensionsColor;
    }
    return defaultColor;
  }

  public LeanColorRGB lookupVerticalDimensionsColor() throws LeanException {
    if (verticalDimensionsColor == null && defaultColor == null) {
      throw new LeanException(
          "No vertical dimensions color nor default color defined in theme '" + name + "'");
    }
    if (verticalDimensionsColor != null) {
      return verticalDimensionsColor;
    }
    return defaultColor;
  }

  public LeanColorRGB lookupFactsColor() throws LeanException {
    if (factsColor == null && defaultColor == null) {
      throw new LeanException("No facts color nor default color defined in theme '" + name + "'");
    }
    if (factsColor != null) {
      return factsColor;
    }
    return defaultColor;
  }

  public LeanColorRGB lookupTitleColor() throws LeanException {
    if (titleColor == null && defaultColor == null) {
      throw new LeanException("No title color nor default color defined in theme '" + name + "'");
    }
    if (titleColor != null) {
      return titleColor;
    }
    return defaultColor;
  }

  public LeanColorRGB lookupAxisColor() throws LeanException {
    if (axisColor == null && defaultColor == null) {
      throw new LeanException("No axis color nor default color defined in theme '" + name + "'");
    }
    if (axisColor != null) {
      return axisColor;
    }
    return defaultColor;
  }

  public LeanColorRGB lookupGridColor() throws LeanException {
    if (gridColor == null && defaultColor == null) {
      throw new LeanException("No grid color nor default color defined in theme '" + name + "'");
    }
    if (gridColor != null) {
      return gridColor;
    }
    return defaultColor;
  }

  public LeanFont lookupHorizontalDimensionsFont() throws LeanException {
    if (horizontalDimensionsFont == null && defaultFont == null) {
      throw new LeanException(
          "No horizontal dimensions font nor default font defined in theme '" + name + "'");
    }
    if (horizontalDimensionsFont != null) {
      return horizontalDimensionsFont;
    }
    return defaultFont;
  }

  public LeanFont lookupVerticalDimensionsFont() throws LeanException {
    if (verticalDimensionsFont == null && defaultFont == null) {
      throw new LeanException(
          "No vertical dimensions font nor default font defined in theme '" + name + "'");
    }
    if (verticalDimensionsFont != null) {
      return verticalDimensionsFont;
    }
    return defaultFont;
  }

  public LeanFont lookupFactsFont() throws LeanException {
    if (factsFont == null && defaultFont == null) {
      throw new LeanException("No facts font nor default font defined in theme '" + name + "'");
    }
    if (factsFont != null) {
      return factsFont;
    }
    return defaultFont;
  }

  public LeanFont lookupTitleFont() throws LeanException {
    if (titleFont == null && defaultFont == null) {
      throw new LeanException("No title font nor default font defined in theme '" + name + "'");
    }
    if (titleFont != null) {
      return titleFont;
    }
    return defaultFont;
  }
}
