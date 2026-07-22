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
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.apache.hop.metadata.api.HopMetadataBase;
import org.apache.hop.metadata.api.HopMetadataProperty;
import org.lean.core.exception.LeanException;
import org.lean.presentation.component.LeanComponent;

/** This represents one page in a presentation. */
@Getter
@Setter
@EqualsAndHashCode(of = "id")
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

  public LeanComponent findComponent(String componentName) throws LeanException {
    for (LeanComponent component : components) {
      if (component.getName().equalsIgnoreCase(componentName)) {
        return component;
      }
    }
    return null;
  }

  /** Perform a topological sort of components based on layout dependencies. */
  @JsonIgnore
  public List<LeanComponent> getSortedComponents() throws LeanException {
    Map<String, LeanComponent> componentsMap = new HashMap<>();
    components.forEach(leanComponent -> componentsMap.put(leanComponent.getName(), leanComponent));

    List<LeanComponent> nonReferenced = new ArrayList<>();
    List<LeanComponent> referenced = new ArrayList<>();

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

    Collections.sort(nonReferenced, Comparator.comparing(HopMetadataBase::getName));

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

    nonReferenced.addAll(referenced);
    return nonReferenced;
  }
}
