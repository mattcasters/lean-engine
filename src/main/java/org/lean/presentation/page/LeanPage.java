package org.lean.presentation.page;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.apache.hop.metadata.api.HopMetadataBase;
import org.apache.hop.metadata.api.HopMetadataProperty;
import org.lean.core.exception.LeanException;
import org.lean.presentation.component.LeanComponent;

/** This represents one page in a presentation. */
@JsonIgnoreProperties(value = {"pageNumber"})
public class LeanPage {
  /** An ID to uniquely identify a page while rendering. We don't serialize this. */
  @JsonIgnore private final String id;

  @HopMetadataProperty private int width;

  @HopMetadataProperty private int height;

  @HopMetadataProperty private int leftMargin;

  @HopMetadataProperty private int rightMargin;

  @HopMetadataProperty private int topMargin;

  @HopMetadataProperty private int bottomMargin;

  @HopMetadataProperty private List<LeanComponent> components;

  @HopMetadataProperty private boolean header;

  @HopMetadataProperty private boolean footer;

  public LeanPage() {
    this.id = UUID.randomUUID().toString();
    this.components = new ArrayList<>();
  }

  public LeanPage(
      int width, int height, int leftMargin, int rightMargin, int topMargin, int bottomMargin) {
    this();
    this.width = width;
    this.height = height;
    this.leftMargin = leftMargin;
    this.rightMargin = rightMargin;
    this.topMargin = topMargin;
    this.bottomMargin = bottomMargin;
  }

  /**
   * Create a copy of the given page with everything on it.
   *
   * @param p
   */
  public LeanPage(LeanPage p) {
    this();
    this.width = p.width;
    this.height = p.height;
    this.leftMargin = p.leftMargin;
    this.rightMargin = p.rightMargin;
    this.topMargin = p.topMargin;
    this.bottomMargin = p.bottomMargin;
    this.header = p.header;
    this.footer = p.footer;
    for (LeanComponent c : p.components) {
      this.components.add(new LeanComponent(c));
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    LeanPage leanPage = (LeanPage) o;
    return id.equals(leanPage.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  public static LeanPage getA4(boolean portrait) {
    int width = 794;
    int height = 1123;
    if (portrait) {
      return new LeanPage(width, height, 25, 25, 25, 25);
    } else {
      return new LeanPage(height, width, 25, 25, 25, 25);
    }
  }

  public static LeanPage getHeaderFooter(boolean header, boolean portrait, int size) {
    // Exclude the margins of parent page!
    //
    int width = 794 - 25 - 25;
    int height = 1123 - 25 - 25;
    LeanPage page;
    if (portrait) {
      page = new LeanPage(width, size, 0, 0, 0, 0);
    } else {
      page = new LeanPage(height, size, 0, 0, 0, 0);
    }
    page.setHeader(header);
    page.setFooter(!header);
    return page;
  }

  @JsonIgnore
  public int getWidthBetweenMargins() {
    return width - leftMargin - rightMargin;
  }

  /**
   * Find a component with a given name
   *
   * @param componentName
   * @return the component or null in case we can't find the component with the given name
   * @throws
   */
  public LeanComponent findComponent(String componentName) throws LeanException {
    for (LeanComponent component : components) {
      if (component.getName().equalsIgnoreCase(componentName)) {
        return component;
      }
    }
    return null;
  }

  /**
   * Perform a topological sort
   *
   * @return a sorted copy of the components
   */
  @JsonIgnore
  public List<LeanComponent> getSortedComponents() throws LeanException {
    // Create a map with the simple name to component relationships
    //
    Map<String, LeanComponent> componentsMap = new HashMap<>();
    components.forEach(leanComponent -> componentsMap.put(leanComponent.getName(), leanComponent));

    // Now create 2 lists: non-referenced components and the rest
    //
    List<LeanComponent> nonReferenced = new ArrayList<>();
    List<LeanComponent> referenced = new ArrayList<>();

    // Calculate the referenced components for each component...
    //
    Map<LeanComponent, Set<LeanComponent>> referencesMap = new HashMap<>();
    for (LeanComponent component : components) {
      Set<LeanComponent> dependencies = component.getDependentComponents(componentsMap);
      referencesMap.put(component, dependencies);
      if (dependencies.isEmpty()) {
        nonReferenced.add(component);
      } else {
        referenced.add(component);
      }
    }

    // Simply sort the non-referenced components by name...
    //
    Collections.sort(nonReferenced, Comparator.comparing(HopMetadataBase::getName));

    // Do a cocktail sort, a safe way to sort a directed graph...
    //
    for (int i = 0; i < referenced.size(); i++) {
      for (int j = 0; j < referenced.size() - 1; j++) {
        LeanComponent a = referenced.get(j);
        LeanComponent b = referenced.get(j + 1);
        if (!a.equals(b)) {
          if (!referencesMap.get(b).contains(a)) {
            referenced.set(j + 1, a);
            referenced.set(j, b);
          }
        }
      }
    }

    // Add the 2 lists...
    //
    nonReferenced.addAll(referenced);
    return nonReferenced;
  }

  /**
   * @return the components
   */
  public List<LeanComponent> getComponents() {
    return components;
  }

  /**
   * @param components the components to set
   */
  public void setComponents(List<LeanComponent> components) {
    this.components = components;
  }

  /**
   * Gets width
   *
   * @return value of width
   */
  public int getWidth() {
    return width;
  }

  /**
   * @param width The width to set
   */
  public void setWidth(int width) {
    this.width = width;
  }

  /**
   * Gets height
   *
   * @return value of height
   */
  public int getHeight() {
    return height;
  }

  /**
   * @param height The height to set
   */
  public void setHeight(int height) {
    this.height = height;
  }

  /**
   * Gets leftMargin
   *
   * @return value of leftMargin
   */
  public int getLeftMargin() {
    return leftMargin;
  }

  /**
   * @param leftMargin The leftMargin to set
   */
  public void setLeftMargin(int leftMargin) {
    this.leftMargin = leftMargin;
  }

  /**
   * Gets rightMargin
   *
   * @return value of rightMargin
   */
  public int getRightMargin() {
    return rightMargin;
  }

  /**
   * @param rightMargin The rightMargin to set
   */
  public void setRightMargin(int rightMargin) {
    this.rightMargin = rightMargin;
  }

  /**
   * Gets topMargin
   *
   * @return value of topMargin
   */
  public int getTopMargin() {
    return topMargin;
  }

  /**
   * @param topMargin The topMargin to set
   */
  public void setTopMargin(int topMargin) {
    this.topMargin = topMargin;
  }

  /**
   * Gets bottomMargin
   *
   * @return value of bottomMargin
   */
  public int getBottomMargin() {
    return bottomMargin;
  }

  /**
   * @param bottomMargin The bottomMargin to set
   */
  public void setBottomMargin(int bottomMargin) {
    this.bottomMargin = bottomMargin;
  }

  /**
   * Gets header
   *
   * @return value of header
   */
  public boolean isHeader() {
    return header;
  }

  /**
   * @param header The header to set
   */
  public void setHeader(boolean header) {
    this.header = header;
  }

  /**
   * Gets footer
   *
   * @return value of footer
   */
  public boolean isFooter() {
    return footer;
  }

  /**
   * @param footer The footer to set
   */
  public void setFooter(boolean footer) {
    this.footer = footer;
  }
}
