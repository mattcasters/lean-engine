package org.lean.presentation.interaction;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.apache.hop.metadata.api.HopMetadataProperty;
import org.lean.core.LeanColumn;
import org.lean.core.draw.DrawnItem;

/** Describe where an interaction can take place */
@Getter
@Setter
public class LeanInteractionLocation {

  @HopMetadataProperty private String componentName;
  @HopMetadataProperty private String componentPluginId;
  @HopMetadataProperty private String itemType;
  @HopMetadataProperty private String itemCategory;
  @HopMetadataProperty private List<String> dimensionColumns;

  public LeanInteractionLocation() {
    this.dimensionColumns = new ArrayList<>();
  }

  public LeanInteractionLocation(
      String componentName,
      String componentPluginId,
      String itemType,
      String itemCategory,
      List<String> dimensions) {
    this.componentName = componentName;
    this.componentPluginId = componentPluginId;
    this.itemType = itemType;
    this.itemCategory = itemCategory;
    this.dimensionColumns = dimensions;
  }

  public LeanInteractionLocation(LeanInteractionLocation location) {
    this();
    this.componentName = location.componentName;
    this.componentPluginId = location.componentPluginId;
    this.itemType = location.itemType;
    this.itemCategory = location.itemCategory;
    this.dimensionColumns.addAll(location.dimensionColumns);
  }

  public boolean matches(DrawnItem drawnItem) {
    if (StringUtils.isNotEmpty(componentName)
        && !drawnItem.getComponentName().equals(componentName)) {
      return false;
    }
    if (StringUtils.isNotEmpty(componentPluginId)
        && !drawnItem.getComponentPluginId().equals(componentPluginId)) {
      return false;
    }
    if (StringUtils.isNotEmpty(itemType) && !drawnItem.getType().name().equals(itemType)) {
      return false;
    }
    if (StringUtils.isNotEmpty(itemCategory)
        && !drawnItem.getCategory().equals(itemCategory)) {
      return false;
    }
    if (!dimensionColumns.isEmpty()) {
      for (String dimensionName : dimensionColumns) {
        boolean found = false;
        for (LeanColumn dimension : drawnItem.getContext().getDimensions()) {
          if (dimension.getColumnName().equals(dimensionName)) {
            found = true;
            break;
          }
        }
        if (!found) {
          return false;
        }
      }
    }
    return true;
  }
}
