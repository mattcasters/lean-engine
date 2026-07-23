package org.lean.presentation.connector.types.filter;

import lombok.Getter;
import lombok.Setter;
import org.apache.hop.metadata.api.HopMetadataProperty;
import org.lean.core.gui.form.LeanGuiFormConstants;
import org.lean.core.gui.plugin.LeanComboSource;
import org.lean.core.gui.plugin.LeanWidgetElement;
import org.lean.core.gui.plugin.LeanWidgetType;

@Getter
@Setter
public class SimpleFilterValue {

  /**
   * Source field to match. Form list editors use connector column names from {@code
   * sourceConnectorName}; annotation documents the intended combo source for future nested schemas.
   */
  @LeanWidgetElement(
      order = "100-fieldName",
      parentId = LeanGuiFormConstants.PARENT_PLUGIN,
      type = LeanWidgetType.COMBO,
      comboSource = LeanComboSource.CONNECTOR_COLUMNS,
      dependsOn = "sourceConnectorName",
      label = "Field name",
      toolTip = "Column from the source connector")
  @HopMetadataProperty
  private String fieldName;

  @LeanWidgetElement(
      order = "200-filterValue",
      parentId = LeanGuiFormConstants.PARENT_PLUGIN,
      type = LeanWidgetType.TEXT,
      label = "Filter value")
  @HopMetadataProperty
  private String filterValue;

  public SimpleFilterValue() {}

  public SimpleFilterValue(String fieldName, String filterValue) {
    this.fieldName = fieldName;
    this.filterValue = filterValue;
  }
}
