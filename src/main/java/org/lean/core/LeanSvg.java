package org.lean.core;

import lombok.Getter;
import lombok.Setter;

/** Represents a piece of SVG on a certain location in another document */
@Getter
@Setter
public class LeanSvg {

  private String svgXml;

  private LeanGeometry geometry;

  public LeanSvg() {}

  public LeanSvg(String svgXml, LeanGeometry geometry) {
    this.svgXml = svgXml;
    this.geometry = geometry;
  }
}
