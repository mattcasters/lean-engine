package org.lean.presentation;

import org.lean.core.LeanDataSet;
import org.lean.core.LeanGeometry;
import org.lean.presentation.component.LeanComponent;
import org.lean.presentation.layout.LeanRenderPage;
import org.lean.presentation.page.LeanPage;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

/** The result of doing a layout on a component with all the data read */
@Getter
@Setter
public class LeanComponentLayoutResult {
  /** The component for which we did the layout */
  private LeanComponent component;

  /** The part number for components split into multiple parts over multiple pages */
  private int partNumber;

  /** The page from which this result originally came */
  private LeanPage sourcePage;

  /** The page on which we render */
  private LeanRenderPage renderPage;

  /** All the data read by the component, cached in memory */
  private LeanDataSet dataSet;

  /** The resulting location and imageSize after the layout */
  private LeanGeometry geometry;

  /**
   * All extra data a component might want to store between doing a layout and the actual rendering
   * of the component
   */
  private Map<String, Object> dataMap;

  public LeanComponentLayoutResult() {
    dataMap = new HashMap<>();
  }

  public LeanComponentLayoutResult(LeanComponentLayoutResult layoutResult) {
    this.component = layoutResult.component;
    this.partNumber = layoutResult.partNumber;
    this.sourcePage = layoutResult.sourcePage;
    this.renderPage = layoutResult.renderPage;
    this.dataSet = layoutResult.dataSet;
    this.geometry = layoutResult.geometry;
    this.dataMap = layoutResult.dataMap;
  }
}
