package org.lean.presentation.component;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.hop.core.logging.ILogChannel;
import org.apache.hop.core.logging.LoggingObject;
import org.apache.hop.metadata.api.HopMetadataBase;
import org.apache.hop.metadata.api.HopMetadataProperty;
import org.apache.hop.metadata.api.IHopMetadata;
import org.apache.hop.metadata.api.IHopMetadataProvider;
import org.lean.core.LeanSize;
import org.lean.core.exception.LeanException;
import org.lean.presentation.LeanPresentation;
import org.lean.presentation.component.listeners.IDoLayoutListener;
import org.lean.presentation.component.listeners.IProcessSourceDataListener;
import org.lean.presentation.component.type.ILeanComponent;
import org.lean.presentation.connector.LeanConnector;
import org.lean.presentation.datacontext.RenderPageDataContext;
import org.lean.presentation.layout.LeanLayout;
import org.lean.presentation.layout.LeanLayoutResults;
import org.lean.presentation.layout.LeanRenderPage;
import org.lean.presentation.page.LeanPage;
import org.lean.presentation.theme.LeanTheme;
import org.lean.render.IRenderContext;
import org.lean.render.context.SimpleRenderContext;
import lombok.Getter;
import lombok.Setter;

/**
 * Main component class encapsulating component plugins through ILeanComponent
 *
 * @author matt
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class LeanComponent extends HopMetadataBase implements IHopMetadata {

  @HopMetadataProperty private LeanLayout layout;
  @HopMetadataProperty private ILeanComponent component;
  @HopMetadataProperty private boolean shared;
  @HopMetadataProperty private String rotation;
  @HopMetadataProperty private String transparency;
  @HopMetadataProperty private LeanSize clipSize;

  @JsonIgnore private List<IProcessSourceDataListener> processSourceDataListeners;
  @JsonIgnore private List<IDoLayoutListener> doLayoutListeners;

  public LeanComponent() {
    this.processSourceDataListeners = new ArrayList<>();
    this.doLayoutListeners = new ArrayList<>();
  }

  public LeanComponent(String name, ILeanComponent component) {
    this();
    this.name = name;
    this.component = component;
  }

  public LeanComponent(LeanComponent c) {
    this();
    this.name = c.name;
    if (c.component != null) {
      this.component = c.component.clone();
      this.component.setThemeName(c.component.getThemeName());
    }
    this.layout = c.layout == null ? null : new LeanLayout(c.layout);
    this.clipSize = c.clipSize == null ? null : new LeanSize(c.clipSize);
    this.processSourceDataListeners.addAll(c.processSourceDataListeners);
    this.doLayoutListeners.addAll(c.doLayoutListeners);
  }

  @Override
  public String toString() {
    return "LeanComponent("
        + name
        + ":"
        + (component == null ? "-" : component.getPluginId())
        + ")";
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof LeanComponent)) {
      return false;
    }
    if (obj == this) {
      return true;
    }
    return ((LeanComponent) obj).name.equals(name);
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }

  /**
   * Process data in the component. Then perform the layout of the component, modify the layout
   * results.
   *
   * @param log The logging channel to log to
   * @param leanPresentation the presentation
   * @param page the page
   * @param dataContext The data context to use
   */
  public void processAndLayout(
      ILogChannel log,
      LeanPresentation leanPresentation,
      LeanPage page,
      RenderPageDataContext dataContext,
      IRenderContext renderContext,
      LeanLayoutResults footerResults)
      throws LeanException {
    component.setLogChannel(log);

    // Header/footer path: apply presentation default when theme is unset/blank
    if (component != null
        && (component.getThemeName() == null || component.getThemeName().isBlank())
        && leanPresentation != null
        && leanPresentation.getDefaultThemeName() != null
        && !leanPresentation.getDefaultThemeName().isBlank()) {
      component.setThemeName(leanPresentation.getDefaultThemeName());
    }

    // Call the process source data listeners...
    //
    for (IProcessSourceDataListener listener : processSourceDataListeners) {
      listener.beforeProcessSourceDataCalled(
          leanPresentation, page, this, dataContext, renderContext, footerResults);
    }

    component.processSourceData(
        leanPresentation, page, this, dataContext, renderContext, footerResults);

    // Call the do layout listeners
    //
    component.doLayout(leanPresentation, page, this, dataContext, renderContext, footerResults);
  }

  /**
   * Build a complete set of all the components this component depends upon for doing layout
   *
   * @param components
   * @return
   */
  public Set<LeanComponent> getDependentComponents(Map<String, LeanComponent> components)
      throws LeanException {
    Set<LeanComponent> set = new HashSet<>();

    for (String referencedComponentName : layout.getReferencedLayoutComponentNames()) {
      LeanComponent referencedComponent = components.get(referencedComponentName);
      if (referencedComponent == null) {
        throw new LeanException(
            "Component "
                + getName()
                + " references "
                + referencedComponentName
                + " which isn't known");
      }
      // Now see if this component is in the list yet...
      //
      if (!set.contains(referencedComponent)) {
        // Do a recursive search and all the referenced components as well...
        //
        set.add(referencedComponent);
        set.addAll(referencedComponent.getDependentComponents(components));
      }
    }

    return set;
  }

  /**
   * Render this component alone on a fresh in-memory presentation: single page (no header/footer),
   * given size, full-page layout, with the supplied connectors and themes. Used for
   * property-editor previews.
   *
   * @param width page width in pixels
   * @param height page height in pixels
   * @param connectors connectors available to the component (copied)
   * @param themes themes available; if empty, {@link LeanTheme#getDefault()} is used
   * @param metadataProvider metadata (shared connectors / themes can still be resolved)
   * @return SVG XML of the rendered page
   */
  public String getSvgXml(
      int width,
      int height,
      List<LeanConnector> connectors,
      List<LeanTheme> themes,
      IHopMetadataProvider metadataProvider)
      throws LeanException {
    return getSvgXml(width, height, connectors, themes, metadataProvider, null);
  }

  /**
   * Same as {@link #getSvgXml(int, int, List, List, IHopMetadataProvider)} but when {@code
   * colorSourcePresentation} is provided, a full layout+render of that presentation is run first
   * so chart series colors ({@code getStableColor}) match the order used on the real page.
   */
  public String getSvgXml(
      int width,
      int height,
      List<LeanConnector> connectors,
      List<LeanTheme> themes,
      IHopMetadataProvider metadataProvider,
      LeanPresentation colorSourcePresentation)
      throws LeanException {

    LoggingObject loggingObject = new LoggingObject("componentPreview");

    // Pre-warm stable series-color maps from the full presentation (same theme discovery order)
    org.lean.render.context.PresentationRenderContext warmContext = null;
    if (colorSourcePresentation != null) {
      try {
        warmContext =
            new org.lean.render.context.PresentationRenderContext(
                colorSourcePresentation, metadataProvider);
        if (colorSourcePresentation.getThemes() != null) {
          warmContext.setThemes(new ArrayList<>(colorSourcePresentation.getThemes()));
        }
        LeanLayoutResults seedResults =
            colorSourcePresentation.doLayout(
                loggingObject, warmContext, metadataProvider, Collections.emptyList());
        colorSourcePresentation.render(seedResults, metadataProvider, warmContext);
      } catch (Exception e) {
        // Preview still works without perfect color matching
        warmContext = null;
      }
    }

    // --- build a throwaway presentation (never saved) ---
    LeanPresentation presentation = new LeanPresentation();
    presentation.setName("preview:" + (name != null ? name : "component"));
    presentation.setHeader(null);
    presentation.setFooter(null);
    presentation.setPages(new ArrayList<>());
    presentation.setConnectors(new ArrayList<>());
    presentation.setThemes(new ArrayList<>());
    presentation.setInteractions(new ArrayList<>());

    // Single page, no margins — page size is the preview canvas
    int pageW = Math.max(1, width);
    int pageH = Math.max(1, height);
    LeanPage page = new LeanPage(pageW, pageH, 0, 0, 0, 0);
    page.setHeader(false);
    page.setFooter(false);
    presentation.getPages().add(page);

    // Connectors (deep copy so streaming state is isolated)
    if (connectors != null) {
      for (LeanConnector connector : connectors) {
        if (connector != null) {
          presentation.getConnectors().add(new LeanConnector(connector));
        }
      }
    }

    // Themes: prefer source presentation default, then provided list, then built-in Default
    List<LeanTheme> themeList = new ArrayList<>();
    String preferredDefaultName =
        colorSourcePresentation != null ? colorSourcePresentation.getDefaultThemeName() : null;
    if (themes != null) {
      for (LeanTheme theme : themes) {
        if (theme != null) {
          themeList.add(theme);
        }
      }
    }
    if (themeList.isEmpty()) {
      themeList.add(LeanTheme.getDefault());
    }
    // Ensure preferred default is first so presentation defaultThemeName matches real page
    if (preferredDefaultName != null && !preferredDefaultName.isBlank()) {
      themeList.sort(
          (a, b) -> {
            boolean aDef = preferredDefaultName.equals(a.getName());
            boolean bDef = preferredDefaultName.equals(b.getName());
            if (aDef == bDef) {
              return 0;
            }
            return aDef ? -1 : 1;
          });
    }
    presentation.setThemes(themeList);
    LeanTheme primary = themeList.get(0);
    if (primary.getName() == null || primary.getName().isEmpty()) {
      primary.setName(org.lean.core.Constants.DEFAULT_THEME_NAME);
    }
    presentation.setDefaultThemeName(
        preferredDefaultName != null && !preferredDefaultName.isBlank()
            ? preferredDefaultName
            : primary.getName());

    // Component copy: fill the preview page (ignore original relative layout)
    LeanComponent previewComponent = new LeanComponent(this);
    previewComponent.setLayout(LeanLayout.fullPage());
    if (previewComponent.getComponent() != null) {
      // Keep explicit theme; otherwise use the same default as the source presentation
      String themeName = previewComponent.getComponent().getThemeName();
      if (themeName == null || themeName.isEmpty()) {
        previewComponent.getComponent().setThemeName(presentation.getDefaultThemeName());
      }
    }
    page.getComponents().add(previewComponent);

    // Layout + render; reuse warmed color maps so series colors match the full page
    org.lean.render.context.PresentationRenderContext renderContext =
        new org.lean.render.context.PresentationRenderContext(presentation, metadataProvider);
    renderContext.setThemes(new ArrayList<>(themeList));
    if (warmContext != null) {
      // Copy stable-color assignment state from full presentation render
      if (warmContext.getThemeValueColorMap() != null) {
        java.util.Map<String, java.util.Map<String, Integer>> copy = new java.util.HashMap<>();
        for (java.util.Map.Entry<String, java.util.Map<String, Integer>> e :
            warmContext.getThemeValueColorMap().entrySet()) {
          copy.put(e.getKey(), new java.util.HashMap<>(e.getValue()));
        }
        renderContext.setThemeValueColorMap(copy);
      }
      if (warmContext.getThemeColorIndexMap() != null) {
        renderContext.setThemeColorIndexMap(
            new java.util.HashMap<>(warmContext.getThemeColorIndexMap()));
      }
    }

    LeanLayoutResults results =
        presentation.doLayout(
            loggingObject, renderContext, metadataProvider, Collections.emptyList());
    presentation.render(results, metadataProvider, renderContext);

    if (results.getRenderPages().isEmpty()) {
      throw new LeanException("Component preview produced no render pages");
    }
    return results.getRenderPages().get(0).getSvgXml();
  }


  /**
   * @param processSourceDataListeners The processSourceDataListeners to set
   */
  public void setProcessSourceDataListeners(
      List<IProcessSourceDataListener> processSourceDataListeners) {
    this.processSourceDataListeners = processSourceDataListeners;
  }
}
