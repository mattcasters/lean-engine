package org.lean.presentation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.batik.svggen.SVGGraphics2D;
import org.apache.commons.lang3.StringUtils;
import org.apache.hop.core.Const;
import org.apache.hop.core.RowMetaAndData;
import org.apache.hop.core.exception.HopException;
import org.apache.hop.core.logging.ILogChannel;
import org.apache.hop.core.logging.ILoggingObject;
import org.apache.hop.core.logging.LogChannel;
import org.apache.hop.core.logging.Metrics;
import org.apache.hop.core.metrics.MetricsSnapshotType;
import org.apache.hop.core.svg.HopSvgGraphics2D;
import org.apache.hop.core.variables.IVariables;
import org.apache.hop.metadata.api.HopMetadata;
import org.apache.hop.metadata.api.HopMetadataBase;
import org.apache.hop.metadata.api.HopMetadataProperty;
import org.apache.hop.metadata.api.IHopMetadata;
import org.apache.hop.metadata.api.IHopMetadataProvider;
import org.lean.core.LeanColorRGB;
import org.lean.core.LeanGeometry;
import org.lean.core.LeanJson;
import org.lean.core.LeanPosition;
import org.lean.core.draw.DrawnItem;
import org.lean.core.exception.LeanException;
import org.lean.core.log.LeanMetricsUtil;
import org.lean.core.metastore.IHasIdentity;
import org.lean.core.LeanAttachment;
import org.lean.presentation.component.LeanComponent;
import org.lean.presentation.component.type.ILeanComponent;
import org.lean.presentation.connector.LeanConnector;
import org.lean.presentation.datacontext.IDataContext;
import org.lean.presentation.datacontext.PresentationDataContext;
import org.lean.presentation.datacontext.RenderPageDataContext;
import org.lean.presentation.interaction.LeanInteraction;
import org.lean.presentation.interaction.LeanInteractionMethod;
import org.lean.presentation.layout.LeanLayout;
import org.lean.presentation.layout.LeanLayoutResults;
import org.lean.presentation.layout.LeanRenderPage;
import org.lean.presentation.page.LeanPage;
import org.lean.presentation.theme.LeanTheme;
import org.lean.presentation.variable.LeanParameter;
import org.lean.presentation.variable.LeanParameterMapping;
import org.lean.render.IRenderContext;
import org.lean.render.context.PresentationRenderContext;
import lombok.Getter;
import lombok.Setter;

@HopMetadata(
    key = "presentation",
    name = "Presentation",
    description = "Top level document of the presentation metadata")
@Getter
@Setter
public class LeanPresentation extends HopMetadataBase implements IHasIdentity, IHopMetadata {

  @HopMetadataProperty private String description;

  @HopMetadataProperty private List<LeanPage> pages;

  @HopMetadataProperty private LeanPage header;

  @HopMetadataProperty private LeanPage footer;

  @HopMetadataProperty private List<LeanTheme> themes;

  @HopMetadataProperty private String defaultThemeName;

  @HopMetadataProperty(storeWithName = true)
  private List<LeanConnector> connectors;

  @HopMetadataProperty private List<LeanInteraction> interactions;
  @HopMetadataProperty private List<LeanParameterMapping> parameterMappings;

  public LeanPresentation() {
    pages = new ArrayList<>();
    connectors = new ArrayList<>();
    themes = new ArrayList<>();
    interactions = new ArrayList<>();
    parameterMappings = new ArrayList<>();
  }

  /**
   * Create a copy of every page, component and connector
   *
   * @param p
   */
  public LeanPresentation(LeanPresentation p) {
    this();
    this.name = p.name;
    this.description = p.description;
    this.header = p.header == null ? null : new LeanPage(p.header);
    this.footer = p.footer == null ? null : new LeanPage(p.footer);
    p.pages.forEach(page -> this.pages.add(new LeanPage(page)));
    p.connectors.forEach(c -> this.connectors.add(new LeanConnector(c)));
    p.themes.forEach(t -> this.themes.add(new LeanTheme(t)));
    p.interactions.forEach(i -> this.interactions.add(new LeanInteraction(i)));
    p.parameterMappings.forEach(m -> this.parameterMappings.add(new LeanParameterMapping(m)));
  }

  public static LeanPresentation fromJsonString(String jsonString) throws IOException {
    return LeanJson.createMapper().readValue(jsonString, LeanPresentation.class);
  }

  @Override
  public String toString() {
    return name != null ? name : super.toString();
  }

  public String toJsonString() throws JsonProcessingException {
    return toJsonString(false);
  }

  public String toJsonString(boolean indent) throws JsonProcessingException {
    ObjectMapper objectMapper = LeanJson.createMapper();
    if (indent) {
      return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(this);
    } else {
      return objectMapper.writeValueAsString(this);
    }
  }

  /**
   * Perform the layout of this presentation.
   *
   * @param parent the parent logging object
   * @param renderContext The rendering context
   * @param metadataProvider The metadata provider to reference external metadata with
   * @param parameters Parameter values that you might want to set in the presentation data context
   * @return The layout results
   * @throws LeanException
   */
  public LeanLayoutResults doLayout(
      ILoggingObject parent,
      IRenderContext renderContext,
      IHopMetadataProvider metadataProvider,
      List<LeanParameter> parameters)
      throws LeanException {

    ILogChannel log = new LogChannel(getName(), parent, true);

    log.logBasic("====> setting parameters: " + parameters.size());
    for (LeanParameter parameter : parameters) {
      log.logBasic("  ===> Setting parameter: " + parameter);
    }

    PresentationDataContext presentationDataContext =
        new PresentationDataContext(this, metadataProvider);

    // See if we need to load the default theme from the metadata...
    //
    if (StringUtils.isNotEmpty(defaultThemeName) && lookupTheme(defaultThemeName) == null) {
      try {
        LeanTheme defaultTheme =
            metadataProvider.getSerializer(LeanTheme.class).load(defaultThemeName);
        if (defaultTheme == null) {
          throw new LeanException(
              "Specified default theme with name '"
                  + defaultThemeName
                  + "' is not present in the metadata");
        }
        // Simply keep it locally
        //
        themes.add(defaultTheme);
      } catch (HopException e) {
        throw new LeanException(
            "Error loading default theme " + defaultThemeName + " from the metadata", e);
      }
    }

    // Apply the given variable values to the data context...
    //
    applyParametersToContext(parameters, presentationDataContext);

    // See if more parameters need to be set using one or more connectors
    //
    applyParameterMappings(presentationDataContext);

    LeanLayoutResults results = new LeanLayoutResults(log);
    results.setDataContext(presentationDataContext);

    log.logBasic("Started layout of presentation");
    log.snap(
        new Metrics(
            MetricsSnapshotType.START,
            LeanMetricsUtil.PRESENTATION_START_LAYOUT,
            "Presentation starts layout"));

    try {
      List<LeanPage> pagesCopy = new ArrayList<>(pages);

      // Loop over the components on every page, generate layout results...
      //
      for (LeanPage page : pagesCopy) {

        // At the very least, add an empty render page in case we have no components...
        //
        results.addNewPage(page, null);

        List<LeanComponent> sortedComponents = page.getSortedComponents();
        for (LeanComponent leanComponent : sortedComponents) {
          layoutComponentSafely(
              log, page, leanComponent, presentationDataContext, renderContext, results);
        }
      }

      return results;
    } finally {
      log.snap(
          new Metrics(
              MetricsSnapshotType.STOP,
              LeanMetricsUtil.PRESENTATION_FINISH_LAYOUT,
              "Presentation finished layout"));
      log.logBasic("Finished layout of presentation");
    }
  }

  private void applyParameterMappings(PresentationDataContext presentationDataContext)
      throws LeanException {
    IVariables variables = presentationDataContext.getVariables();

    for (LeanParameterMapping parameterMapping : parameterMappings) {
      // Read rows from the connector specified.  The data context has the metadata provider.
      //
      String connectorName = variables.resolve(parameterMapping.getConnectorName());
      if (StringUtils.isEmpty(connectorName)) {
        throw new LeanException(
            "Please specify a connector name to read rows of data from.  "
                + "These rows can be used to set parameters in the presentation.");
      }
      String separator = variables.resolve(parameterMapping.getSeparator());

      LeanConnector connector = presentationDataContext.getConnector(connectorName);
      List<RowMetaAndData> rows = connector.retrieveRows(presentationDataContext);

      Map<String, String> parametersMap = new HashMap<>();

      for (LeanParameterMapping.FieldToParameterMapping mapping : parameterMapping.getMappings()) {
        String fieldName = variables.resolve(mapping.getFieldName());
        if (StringUtils.isEmpty(fieldName)) {
          throw new LeanException(
              "Please specify a field name to map when reading from connector " + connectorName);
        }
        String parameterName = variables.resolve(mapping.getParameterName());
        if (StringUtils.isEmpty(parameterName)) {
          throw new LeanException(
              "Please specify a name for a parameter to set for field name " + fieldName);
        }

        // Concatenate all input rows to flatten to a single value per field.
        //
        for (RowMetaAndData row : rows) {
          try {
            String value = row.getString(fieldName, "");
            String totalValue = parametersMap.get(parameterName);
            if (totalValue == null) {
              totalValue = value;
            } else {
              totalValue = Const.NVL(separator, "") + value;
            }
            parametersMap.put(parameterName, totalValue);

          } catch (Exception e) {
            throw new LeanException(
                "Error converting an input row value to a string when mapping field "
                    + fieldName
                    + " to parameter "
                    + parameterName,
                e);
          }
        }
      }

      // Now that we have all the parameter values, set these in the data context.
      //
      parametersMap
          .keySet()
          .forEach(
              parameterName -> {
                String parameterValue = parametersMap.get(parameterName);
                presentationDataContext.getVariables().setVariable(parameterName, parameterValue);
              });
    }
  }

  private void applyParametersToContext(
      List<LeanParameter> parameters, PresentationDataContext presentationDataContext) {
    for (LeanParameter variable : parameters) {
      if (StringUtils.isNotEmpty(variable.getParameterName())) {
        String name = variable.getParameterName();
        String value = variable.getParameterValue();
        presentationDataContext.getVariables().setVariable(name, Const.NVL(value, ""));
      }
    }
  }

  /**
   * Render this presentation by rendering all the render pages in the layout results... At the end,
   * we'll have some stuff drawn on the Graphics Context of each render page...
   *
   * @param results Where to store rendering results
   * @param metadataProvider The metadata provider to reference external metadata with
   * @return The presentation rendering log channel
   * @throws LeanException in case something goes wrong
   */
  public ILogChannel render(LeanLayoutResults results, IHopMetadataProvider metadataProvider)
      throws LeanException {
    return render(results, metadataProvider, null);
  }

  /**
   * Render this presentation. When {@code sharedRenderContext} is non-null it is reused (including
   * its stable series-color maps) so a follow-up single-component preview can match full-page
   * colors.
   */
  public ILogChannel render(
      LeanLayoutResults results,
      IHopMetadataProvider metadataProvider,
      PresentationRenderContext sharedRenderContext)
      throws LeanException {

    ILogChannel log = results.getLog();
    PresentationDataContext presentationDataContext =
        new PresentationDataContext(this, metadataProvider);
    PresentationRenderContext presentationRenderContext =
        sharedRenderContext != null
            ? sharedRenderContext
            : new PresentationRenderContext(this, metadataProvider);
    if (sharedRenderContext != null) {
      // Keep theme lookup bound to this presentation instance
      presentationRenderContext.setPresentation(this);
    }

    log.logBasic("Started rendering presentation");
    log.snap(
        new Metrics(
            MetricsSnapshotType.START,
            LeanMetricsUtil.PRESENTATION_START_RENDER,
            "Presentation starts rendering"));

    try {
      // Now that we know the layout, we know the page numbers.
      //
      results.setRenderPageNumbers();

      // Loop over all the pages that were allocated
      //
      for (LeanRenderPage renderPage : results.getRenderPages()) {
        LeanPage page = renderPage.getPage();
        SVGGraphics2D gc = renderPage.getGc();

        // Fill the background with the default background color...
        //
        LeanTheme defaultTheme = getDefaultTheme();
        if (defaultTheme == null && themes != null && !themes.isEmpty()) {
          defaultTheme = themes.get(0);
        }
        if (defaultTheme == null) {
          defaultTheme = LeanTheme.getDefault();
        }
        LeanColorRGB bg = defaultTheme.lookupBackgroundColor();
        gc.setColor(new Color(bg.getR(), bg.getG(), bg.getB()));
        gc.fillRect(0, 0, page.getWidth(), page.getHeight());

        AffineTransform parentTransform = gc.getTransform();

        // First render header and footer if present
        //
        renderHeaderFooter(
            log, renderPage, parentTransform, presentationDataContext, presentationRenderContext);

        // Draw at top left of page
        //
        LeanPosition offSet =
            new LeanPosition(page.getLeftMargin(), page.getTopMargin() + getHeaderHeight());
        gc.translate(offSet.getX(), offSet.getY());

        // Loop over all the component layout results on the page...
        //
        List<LeanComponentLayoutResult> componentLayoutResults = renderPage.getLayoutResults();
        for (LeanComponentLayoutResult componentLayoutResult : componentLayoutResults) {
          LeanComponent leanComponent = componentLayoutResult.getComponent();

          // Render the component...
          //
          AffineTransform beforeRotation = gc.getTransform();

          // Do we rotate?
          // If so, rotate around the center of the object
          //
          if (StringUtils.isNotEmpty(leanComponent.getRotation())) {
            LeanGeometry geometry = componentLayoutResult.getGeometry();
            double angle = Math.toRadians(Const.toDouble(leanComponent.getRotation(), 0));
            int originX = geometry.getX() + geometry.getWidth() / 2;
            int originY = geometry.getY() + geometry.getHeight() / 2;
            gc.rotate(angle, originX, originY);
          }

          // Transparency?
          //
          Composite beforeComposite = gc.getComposite();
          if (StringUtils.isNotEmpty(leanComponent.getTransparency())) {
            double alpha = Const.toDouble(leanComponent.getTransparency(), 0) / 100;
            if (alpha > 1.0f) {
              alpha = 1.0f;
            }
            if (alpha < 0.0f) {
              alpha = 0.0f;
            }
            gc.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) alpha));
          }

          // Clipping of string drawing...
          boolean clip =
              leanComponent.getClipSize() != null && leanComponent.getClipSize().isDefined();
          Shape oldClip = gc.getClip();
          if (clip) {
            LeanGeometry lg = componentLayoutResult.getGeometry();
            gc.setClip(lg.getX(), lg.getY(), lg.getWidth(), lg.getHeight());
          }

          LeanComponent component = componentLayoutResult.getComponent();
          LeanRenderPage bodyPage = componentLayoutResult.getRenderPage();
          renderComponentSafely(
              log,
              gc,
              componentLayoutResult,
              results,
              presentationRenderContext,
              offSet,
              bodyPage);

          if (clip) {
            gc.setClip(oldClip);
          }

          // Remember where we've drawn this component on THIS render page.
          // Use the layout result geometry (per part / per page), not results.findGeometry(name)
          // which is a single map entry overwritten by multi-page components (table/crosstab).
          //
          LeanGeometry componentGeometry = componentLayoutResult.getGeometry();
          if (componentGeometry != null) {
            bodyPage.addComponentDrawnItem(component, componentGeometry, offSet);
          }

          gc.setComposite(beforeComposite);
          gc.setTransform(beforeRotation);
        }
      }

    } finally {
      log.snap(
          new Metrics(
              MetricsSnapshotType.STOP,
              LeanMetricsUtil.PRESENTATION_FINISH_RENDER,
              "Presentation finished rendering"));
      log.logBasic("Finished rendering presentation");
    }

    return log;
  }

  /** Render the header and footers on top of the render page... */
  private void renderHeaderFooter(
      ILogChannel log,
      LeanRenderPage renderPage,
      AffineTransform parentTransform,
      IDataContext presentationDataContext,
      IRenderContext renderContext)
      throws LeanException {
    LeanPage page = renderPage.getPage();
    HopSvgGraphics2D gc = renderPage.getGc();

    // What is the render context for header and footer?
    //
    RenderPageDataContext pageDataContext =
        new RenderPageDataContext(presentationDataContext, renderPage);

    if (header != null) {
      // Just making sure
      header.setHeader(true);

      // Do the layout of the header on every page again...
      //
      List<LeanComponent> sortedComponents = header.getSortedComponents();

      // Create a new results object which maps onto the existing render page
      //
      LeanLayoutResults headerResults = new LeanLayoutResults(log);

      for (LeanComponent component : sortedComponents) {
        layoutComponentSafely(
            log, header, component, pageDataContext, renderContext, headerResults);
      }

      // We did the layout and generated a new page for the header
      // It's contained in headerResults
      // We don't want to render on these RenderPages though, we want to render on the given
      // renderPage.
      //
      headerResults.replaceGCForHeaderFooter(gc);
      headerResults.replaceDrawnItemsForHeaderFooter(renderPage.getDrawnItems());

      // Empty header (just enabled, no components yet) has no layout results — skip draw
      if (!headerResults.getRenderPages().isEmpty()) {
        // Before rendering, position rendering at the top of the page, after the margin...
        //
        LeanPosition offSet = new LeanPosition(page.getLeftMargin(), page.getTopMargin());
        gc.translate(offSet.getX(), offSet.getY());

        // Now render the header onto the given render page GC
        // Only one header "page" is supported
        //
        List<LeanComponentLayoutResult> componentLayoutResults =
            headerResults.getRenderPages().get(0).getLayoutResults();
        for (LeanComponentLayoutResult componentLayoutResult : componentLayoutResults) {
          LeanComponent component = componentLayoutResult.getComponent();
          renderComponentSafely(
              log,
              gc,
              componentLayoutResult,
              headerResults,
              renderContext,
              offSet,
              renderPage);
          if (componentLayoutResult.getGeometry() != null) {
            renderPage.addComponentDrawnItem(
                component, componentLayoutResult.getGeometry(), offSet);
          }
        }

        // Reset the gc translation...
        //
        gc.setTransform(parentTransform);
      }
    }

    if (footer != null) {
      // Just making sure
      footer.setFooter(true);

      // Do the layout of the footer on every page again...
      //
      List<LeanComponent> sortedComponents = footer.getSortedComponents();

      // Create a new results object which maps onto the existing render page
      //
      LeanLayoutResults footerResults = new LeanLayoutResults(log);

      for (LeanComponent leanComponent : sortedComponents) {
        layoutComponentSafely(
            log, footer, leanComponent, pageDataContext, renderContext, footerResults);
      }

      // We did the layout and generated a new page for the footer
      // It's contained in footerResults
      // We don't want to render on these RenderPages though, we want to render on the given
      // renderPage.
      //
      footerResults.replaceGCForHeaderFooter(gc);
      footerResults.replaceDrawnItemsForHeaderFooter(renderPage.getDrawnItems());

      // Empty footer (just enabled, no components yet) has no layout results — skip draw
      if (!footerResults.getRenderPages().isEmpty()) {
        // Before rendering, position rendering at the bottom of the page.
        // The position is the page height minus bottom margin and footer height
        //
        LeanPosition offSet =
            new LeanPosition(
                page.getLeftMargin(),
                page.getHeight() - page.getBottomMargin() - getFooterHeight());
        gc.translate(offSet.getX(), offSet.getY());

        // Now render the footer onto the given render page GC
        // Only one footer "page" is supported
        //
        List<LeanComponentLayoutResult> componentLayoutResults =
            footerResults.getRenderPages().get(0).getLayoutResults();
        for (LeanComponentLayoutResult componentLayoutResult : componentLayoutResults) {
          LeanComponent component = componentLayoutResult.getComponent();
          renderComponentSafely(
              log,
              gc,
              componentLayoutResult,
              footerResults,
              renderContext,
              offSet,
              renderPage);
          if (componentLayoutResult.getGeometry() != null) {
            renderPage.addComponentDrawnItem(
                component, componentLayoutResult.getGeometry(), offSet);
          }
        }

        // Reset the gc translation...
        //
        gc.setTransform(parentTransform);
      }
    }
  }

  /** Data-map key when layout failed for a component (placeholder still drawn). */
  public static final String DATA_LAYOUT_ERROR = "layoutError";

  /** Full exception chain / stack for property-panel diagnostics. */
  public static final String DATA_LAYOUT_ERROR_DETAIL = "layoutErrorDetail";

  /**
   * Process source data + layout for one component. On failure (e.g. SQL table missing), log and
   * place a placeholder so the presentation editor can still open.
   */
  private void layoutComponentSafely(
      ILogChannel log,
      LeanPage page,
      LeanComponent leanComponent,
      IDataContext dataContext,
      IRenderContext renderContext,
      LeanLayoutResults results) {
    if (leanComponent == null || leanComponent.getComponent() == null) {
      return;
    }
    ILeanComponent component = leanComponent.getComponent();
    try {
      // Treat blank like unset (forms often write "" for "use default theme")
      if (StringUtils.isEmpty(component.getThemeName())) {
        component.setThemeName(defaultThemeName);
      }
      component.setLogChannel(log);
      component.processSourceData(this, page, leanComponent, dataContext, renderContext, results);
      component.doLayout(this, page, leanComponent, dataContext, renderContext, results);
    } catch (Exception e) {
      String summary = summarizeException(e);
      String detail = formatExceptionDetail(e);
      log.logError(
          "Error laying out component '"
              + leanComponent.getName()
              + "' (continuing with placeholder): "
              + summary,
          e);
      addFailedComponentPlaceholder(results, page, leanComponent, summary, detail);
    }
  }

  /**
   * Render one component layout result; draw an error box if layout failed or render throws.
   *
   * @param targetPage the page receiving drawn items (body page; may differ from layout result page
   *     for header/footer). When non-null, layout errors are recorded for the editor.
   */
  private void renderComponentSafely(
      ILogChannel log,
      SVGGraphics2D gc,
      LeanComponentLayoutResult componentLayoutResult,
      LeanLayoutResults results,
      IRenderContext renderContext,
      LeanPosition offSet,
      LeanRenderPage targetPage) {
    LeanComponent component = componentLayoutResult.getComponent();
    try {
      if (componentLayoutResult.getDataMap() != null
          && componentLayoutResult.getDataMap().containsKey(DATA_LAYOUT_ERROR)) {
        String summary =
            String.valueOf(componentLayoutResult.getDataMap().get(DATA_LAYOUT_ERROR));
        Object detailObj = componentLayoutResult.getDataMap().get(DATA_LAYOUT_ERROR_DETAIL);
        String detail = detailObj != null ? String.valueOf(detailObj) : summary;
        drawFailedComponentPlaceholder(gc, componentLayoutResult.getGeometry(), summary);
        recordComponentErrorOnPage(targetPage, component, summary, detail);
        return;
      }
      component
          .getComponent()
          .render(componentLayoutResult, results, renderContext, offSet);
    } catch (Exception renderEx) {
      String summary = summarizeException(renderEx);
      String detail = formatExceptionDetail(renderEx);
      log.logError(
          "Error rendering component '" + component.getName() + "': " + summary, renderEx);
      drawFailedComponentPlaceholder(gc, componentLayoutResult.getGeometry(), summary);
      if (componentLayoutResult.getDataMap() != null) {
        componentLayoutResult.getDataMap().put(DATA_LAYOUT_ERROR, summary);
        componentLayoutResult.getDataMap().put(DATA_LAYOUT_ERROR_DETAIL, detail);
      }
      recordComponentErrorOnPage(targetPage, component, summary, detail);
    }
  }

  private static void recordComponentErrorOnPage(
      LeanRenderPage targetPage, LeanComponent component, String summary, String detail) {
    if (targetPage == null || component == null) {
      return;
    }
    targetPage.recordComponentError(component.getName(), summary, detail);
  }

  /** Prefer the root cause message for short UI labels (canvas / list). */
  public static String summarizeException(Throwable e) {
    if (e == null) {
      return "Unknown error";
    }
    Throwable root = e;
    while (root.getCause() != null && root.getCause() != root) {
      root = root.getCause();
    }
    if (root.getMessage() != null && !root.getMessage().isBlank()) {
      return root.getMessage().trim();
    }
    if (e.getMessage() != null && !e.getMessage().isBlank()) {
      return e.getMessage().trim();
    }
    return e.getClass().getSimpleName();
  }

  /** Full cause chain + simple stack for the property-panel diagnostics. */
  public static String formatExceptionDetail(Throwable e) {
    if (e == null) {
      return "";
    }
    StringBuilder sb = new StringBuilder();
    Throwable t = e;
    int depth = 0;
    while (t != null && depth < 20) {
      if (depth > 0) {
        sb.append("\nCaused by: ");
      }
      String msg = t.getMessage();
      if (msg != null && !msg.isBlank()) {
        sb.append(msg.trim());
      } else {
        sb.append(t.getClass().getName());
      }
      t = t.getCause();
      depth++;
    }
    try {
      String stack = Const.getSimpleStackTrace(e);
      if (stack != null && !stack.isBlank()) {
        sb.append("\n\n").append(stack.trim());
      }
    } catch (Exception ignored) {
      // Const may not be available in all environments
    }
    return sb.toString();
  }

  private void addFailedComponentPlaceholder(
      LeanLayoutResults results,
      LeanPage page,
      LeanComponent leanComponent,
      String errorMessage,
      String errorDetail) {
    try {
      LeanRenderPage renderPage = results.getCurrentRenderPage(page);
      if (renderPage == null) {
        renderPage = results.addNewPage(page, null);
      }
      LeanGeometry geometry = geometryFromLayoutOrDefault(leanComponent, page);
      LeanComponentLayoutResult result = new LeanComponentLayoutResult();
      result.setRenderPage(renderPage);
      result.setSourcePage(page);
      result.setComponent(leanComponent);
      result.setGeometry(geometry);
      result.setPartNumber(1);
      result.getDataMap().put(DATA_LAYOUT_ERROR, errorMessage);
      if (errorDetail != null) {
        result.getDataMap().put(DATA_LAYOUT_ERROR_DETAIL, errorDetail);
      }
      renderPage.recordComponentError(leanComponent.getName(), errorMessage, errorDetail);
      results.addComponentGeometry(leanComponent.getName(), geometry);
      renderPage.getLayoutResults().add(result);
    } catch (Exception e) {
      // Last resort: ignore placeholder failure so layout can finish
      if (results != null && results.getLog() != null) {
        results
            .getLog()
            .logError(
                "Could not create placeholder for component '"
                    + leanComponent.getName()
                    + "': "
                    + e.getMessage());
      }
    }
  }

  /**
   * Best-effort geometry from absolute layout offsets when processSourceData/doLayout failed
   * (cannot use component getExpectedGeometry — it often depends on data details).
   */
  private LeanGeometry geometryFromLayoutOrDefault(LeanComponent leanComponent, LeanPage page) {
    int x = 0;
    int y = 0;
    int w = 400;
    int h = 200;
    LeanLayout layout = leanComponent != null ? leanComponent.getLayout() : null;
    if (layout != null) {
      if (layout.getLeft() != null) {
        x = layout.getLeft().getOffset();
      }
      if (layout.getTop() != null) {
        y = layout.getTop().getOffset();
      }
      if (layout.getRight() != null
          && layout.getRight().getAlignment() == LeanAttachment.Alignment.LEFT) {
        w = Math.max(40, layout.getRight().getOffset() - x);
      } else if (layout.getRight() != null
          && (layout.getRight().getAlignment() == LeanAttachment.Alignment.RIGHT
              || layout.getRight().getAlignment() == LeanAttachment.Alignment.DEFAULT)
          && page != null) {
        w = Math.max(40, page.getWidthBetweenMargins() - x + layout.getRight().getOffset());
      }
      if (layout.getBottom() != null
          && layout.getBottom().getAlignment() == LeanAttachment.Alignment.TOP) {
        h = Math.max(40, layout.getBottom().getOffset() - y);
      } else if (layout.getBottom() != null
          && (layout.getBottom().getAlignment() == LeanAttachment.Alignment.BOTTOM
              || layout.getBottom().getAlignment() == LeanAttachment.Alignment.DEFAULT)
          && page != null) {
        h = Math.max(40, getUsableHeight(page) - y + layout.getBottom().getOffset());
      }
    }
    return new LeanGeometry(x, y, w, h);
  }

  private static void drawFailedComponentPlaceholder(
      SVGGraphics2D gc, LeanGeometry geometry, String message) {
    if (gc == null || geometry == null) {
      return;
    }
    int x = geometry.getX();
    int y = geometry.getY();
    int w = Math.max(40, geometry.getWidth());
    int h = Math.max(24, geometry.getHeight());
    Color old = gc.getColor();
    java.awt.Font oldFont = gc.getFont();
    try {
      gc.setColor(new Color(255, 245, 245));
      gc.fillRect(x, y, w, h);
      gc.setColor(new Color(180, 40, 40));
      gc.drawRect(x, y, w - 1, h - 1);
      gc.setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 11));
      String text = message != null ? message : "Component error";
      // Keep first line short enough for the box
      if (text.length() > 120) {
        text = text.substring(0, 117) + "...";
      }
      gc.drawString(text, x + 6, y + Math.min(18, h - 4));
    } finally {
      gc.setColor(old);
      gc.setFont(oldFont);
    }
  }

  /**
   * Look for the connector with the given name and hand its implementation back.
   *
   * @param name The name of the connector to look for.
   * @return The connector implementation or null if the connector couldn't be found
   */
  public LeanConnector getConnector(String name) {
    for (LeanConnector connector : connectors) {
      if (connector != null
          && connector.getName() != null
          && connector.getName().equalsIgnoreCase(name)) {
        return connector;
      }
    }
    return null;
  }

  /**
   * Look for the theme with the given name
   *
   * @param themeName the theme name to look for
   * @return The theme or null if nothing could be found
   */
  public LeanTheme lookupTheme(String themeName) {
    if (themeName == null) {
      return null;
    }
    for (LeanTheme theme : themes) {
      if (theme.getName().equalsIgnoreCase(themeName)) {
        return theme;
      }
    }
    return null;
  }

  /**
   * @return The default theme using the default theme name or null if it couldn't be found.
   */
  @JsonIgnore
  public LeanTheme getDefaultTheme() {
    return lookupTheme(defaultThemeName);
  }

  /**
   * Calculate how much usable room is on the base. It's the height of the page minus the header
   * imageSize, the footer imageSize and the page margins
   *
   * @param page The page to render on.
   * @return The usable height on the page
   */
  public int getUsableHeight(LeanPage page) {
    int height = page.getHeight();
    height -= page.getTopMargin();
    height -= page.getBottomMargin();
    if (!page.isHeader() && !page.isFooter()) {
      height -= getHeaderHeight();
      height -= getFooterHeight();
    }
    return height;
  }

  @JsonIgnore
  public int getHeaderHeight() {
    if (header == null) {
      return 0;
    } else {
      return header.getHeight();
    }
  }

  @JsonIgnore
  public int getFooterHeight() {
    if (footer == null) {
      return 0;
    } else {
      return footer.getHeight();
    }
  }

  /**
   * Find the given interaction for the drawn item. Look in the list of defined interactions for
   * this presentation to see what needs to happen to the particular drawn item. We assumed it's
   * something
   *
   * @param method the method to look for or null for any method
   * @param drawnItem The drawn item
   * @return The first interaction found for this possibility.
   */
  public LeanInteraction findInteraction(LeanInteractionMethod method, DrawnItem drawnItem) {
    for (LeanInteraction interaction : interactions) {
      if (interaction.matches(method, drawnItem)) {
        return interaction;
      }
    }
    return null;
  }

  /**
   * Get the index of a logical page.
   *
   * @param page The page to index
   * @return The index (page number) of the page
   */
  public int getPageIndex(LeanPage page) {
    return pages.indexOf(page);
  }
}
