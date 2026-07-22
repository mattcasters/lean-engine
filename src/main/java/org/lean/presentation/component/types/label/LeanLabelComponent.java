package org.lean.presentation.component.types.label;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.awt.font.TextAttribute;
import java.text.AttributedString;
import lombok.Getter;
import lombok.Setter;
import org.apache.batik.svggen.SVGGraphics2D;
import org.apache.commons.lang3.StringUtils;
import org.lean.core.gui.plugin.LeanWidgetType;
import org.lean.core.gui.plugin.LeanWidgetElement;
import org.apache.hop.core.svg.HopSvgGraphics2D;
import org.apache.hop.metadata.api.HopMetadataProperty;
import org.lean.core.LeanGeometry;
import org.lean.core.LeanHorizontalAlignment;
import org.lean.core.LeanPosition;
import org.lean.core.LeanSize;
import org.lean.core.LeanTextGeometry;
import org.lean.core.LeanVerticalAlignment;
import org.lean.core.draw.DrawnContext;
import org.lean.core.draw.DrawnItem;
import org.lean.core.exception.LeanException;
import org.lean.core.gui.form.LeanGuiFormConstants;
import org.lean.presentation.LeanComponentLayoutResult;
import org.lean.presentation.LeanPresentation;
import org.lean.presentation.component.LeanComponent;
import org.lean.presentation.component.type.ILeanComponent;
import org.lean.presentation.component.type.LeanBaseComponent;
import org.lean.presentation.component.type.LeanComponentPlugin;
import org.lean.presentation.datacontext.IDataContext;
import org.lean.presentation.layout.LeanLayoutResults;
import org.lean.presentation.layout.LeanRenderPage;
import org.lean.presentation.page.LeanPage;
import org.lean.render.IRenderContext;

@JsonDeserialize(as = LeanLabelComponent.class)
@LeanComponentPlugin(
    id = "LeanLabelComponent",
    name = "Label",
    description = "A Label to decorate your presentations")
@Getter
@Setter
public class LeanLabelComponent extends LeanBaseComponent implements ILeanComponent {

  public static final String DATA_TEXT_GEOMETRY = "Text Geometry";
  public static final String DATA_TEXT_STRING = "Text String";

  @LeanWidgetElement(
      order = "10000-label",
      parentId = LeanGuiFormConstants.PARENT_PLUGIN,
      type = LeanWidgetType.TEXT,
      label = "Label text",
      toolTip = "The label text (variables are resolved at render time)")
  @HopMetadataProperty
  private String label;

  @LeanWidgetElement(
      order = "10100-underline",
      parentId = LeanGuiFormConstants.PARENT_PLUGIN,
      type = LeanWidgetType.CHECKBOX,
      label = "Underline?")
  @HopMetadataProperty
  private boolean underline;

  @LeanWidgetElement(
      order = "10200-horizontalAlignment",
      parentId = LeanGuiFormConstants.PARENT_PLUGIN,
      type = LeanWidgetType.COMBO,
      label = "Horizontal alignment")
  @HopMetadataProperty
  private LeanHorizontalAlignment horizontalAlignment;

  @LeanWidgetElement(
      order = "10300-verticalAlignment",
      parentId = LeanGuiFormConstants.PARENT_PLUGIN,
      type = LeanWidgetType.COMBO,
      label = "Vertical alignment")
  @HopMetadataProperty
  private LeanVerticalAlignment verticalAlignment;

  @LeanWidgetElement(
      order = "10400-customHtml",
      parentId = LeanGuiFormConstants.PARENT_PLUGIN,
      type = LeanWidgetType.TEXT,
      label = "Custom HTML",
      toolTip = "Optional custom HTML content")
  @HopMetadataProperty
  private String customHtml;

  public LeanLabelComponent() {
    super("LeanLabelComponent");
    horizontalAlignment = LeanHorizontalAlignment.LEFT;
    verticalAlignment = LeanVerticalAlignment.TOP;
    underline = false;
  }

  public LeanLabelComponent(LeanLabelComponent c) {
    super("LeanLabelComponent", c);
    this.label = c.label;
    this.horizontalAlignment = c.horizontalAlignment;
    this.verticalAlignment = c.verticalAlignment;
    this.customHtml = c.customHtml;
    this.underline = c.underline;
  }

  public LeanLabelComponent(String label) {
    this();
    this.label = label;
  }

  public LeanLabelComponent clone() {
    return new LeanLabelComponent(this);
  }

  @Override
  public void processSourceData(
      LeanPresentation presentation,
      LeanPage page,
      LeanComponent component,
      IDataContext dataContext,
      IRenderContext renderContext,
      LeanLayoutResults results)
      throws LeanException {
    // Nothing to read, it's just a label
    //
    // Calculate the width and height of the text in the given font
    //
    LeanRenderPage currentRenderPage = results.getCurrentRenderPage(page);
    HopSvgGraphics2D gc = currentRenderPage.getGc();

    // Set the font, so we can calculate the correct text imageSize
    //
    enableFont(gc, lookupDefaultFont(renderContext));

    // Calculate the string
    //
    String text = dataContext.getVariables().resolve(label);

    LeanTextGeometry textGeometry = calculateTextGeometry(gc, text);

    // Don't calculate this twice...
    //
    results.addDataSet(component, DATA_TEXT_STRING, text);
    results.addDataSet(component, DATA_TEXT_GEOMETRY, textGeometry);
  }

  public LeanSize getExpectedSize(
      LeanPresentation leanPresentation,
      LeanPage page,
      LeanComponent component,
      IDataContext dataContext,
      IRenderContext renderContext,
      LeanLayoutResults results)
      throws LeanException {

    LeanTextGeometry textGeometry =
        (LeanTextGeometry) results.getDataSet(component, DATA_TEXT_GEOMETRY);

    // Retain the location, adjust the width and Height
    //
    return new LeanSize(textGeometry.getWidth(), textGeometry.getHeight());
  }

  public void render(
      LeanComponentLayoutResult layoutResult,
      LeanLayoutResults results,
      IRenderContext renderContext,
      LeanPosition offSet)
      throws LeanException {

    LeanGeometry componentGeometry = layoutResult.getGeometry();
    LeanComponent component = layoutResult.getComponent();

    // Remember the proper text geometry
    //
    LeanTextGeometry textGeometry =
        (LeanTextGeometry) results.getDataSet(component, DATA_TEXT_GEOMETRY);
    String text = (String) results.getDataSet(component, DATA_TEXT_STRING);

    if (StringUtils.isEmpty(text)) {
      text = " ";
    }

    SVGGraphics2D gc = layoutResult.getRenderPage().getGc();

    // Draw background for the full imageSize of the component area
    //
    setBackgroundBorderFont(gc, componentGeometry, renderContext);

    float x;
    float y;

    if (horizontalAlignment == null) {
      throw new LeanException(
          "Don't know how to horizontally align label '"
              + layoutResult.getComponent().getName()
              + "'");
    }

    switch (horizontalAlignment) {
      case RIGHT:
        x = componentGeometry.getX() + componentGeometry.getWidth() - textGeometry.getWidth();
        break;
      case CENTER:
        x = componentGeometry.getX() + (componentGeometry.getWidth() - textGeometry.getWidth()) / 2;
        break;
      case LEFT:
      default:
        x = componentGeometry.getX();
        break;
    }

    if (verticalAlignment == null) {
      throw new LeanException(
          "Don't know how to vertically align label '"
              + layoutResult.getComponent().getName()
              + "'");
    }

    switch (verticalAlignment) {
      case BOTTOM:
        y = componentGeometry.getY() + componentGeometry.getHeight() - textGeometry.getHeight();
        break;
      case MIDDLE:
        y =
            componentGeometry.getY()
                + (componentGeometry.getHeight() - textGeometry.getHeight()) / 2;
        break;
      case TOP:
      default:
        y = componentGeometry.getY();
        break;
    }

    AttributedString attributedString = new AttributedString(text);
    if (underline) {
      attributedString.addAttribute( TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON );
    }
    gc.drawString(
        attributedString.getIterator(),
        x + textGeometry.getOffsetX(),
        y + textGeometry.getOffsetY());

    LeanGeometry labelGeometry =
        new LeanGeometry(
            Math.round(offSet.getX() + x + textGeometry.getOffsetX()),
            Math.round(offSet.getY() + y),
            textGeometry.getWidth(),
            textGeometry.getHeight());

    layoutResult
        .getRenderPage()
        .getDrawnItems()
        .add(
            new DrawnItem(
                component.getName(),
                component.getComponent().getPluginId(),
                layoutResult.getPartNumber(),
                DrawnItem.DrawnItemType.ComponentItem,
                DrawnItem.Category.Label.name(),
                0,
                0,
                labelGeometry,
                new DrawnContext(text)));
  }
}
