package org.lean.core;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.apache.hop.metadata.api.HopMetadataProperty;
import org.lean.core.gui.form.LeanGuiFormConstants;
import org.lean.core.gui.plugin.LeanWidgetElement;
import org.lean.core.gui.plugin.LeanWidgetType;

@Getter
@Setter
public class LeanSortMethod {

  @LeanWidgetElement(
      order = "100-type",
      parentId = LeanGuiFormConstants.PARENT_PLUGIN,
      type = LeanWidgetType.COMBO,
      label = "Sort type")
  @HopMetadataProperty
  private Type type;

  @LeanWidgetElement(
      order = "200-ascending",
      parentId = LeanGuiFormConstants.PARENT_PLUGIN,
      type = LeanWidgetType.CHECKBOX,
      label = "Ascending?")
  @HopMetadataProperty
  private boolean ascending;

  @HopMetadataProperty private List<String> customOrder;

  public LeanSortMethod() {
    type = Type.NATIVE_VALUE;
    ascending = true;
    customOrder = new ArrayList<>();
  }

  public LeanSortMethod(Type type, boolean ascending) {
    this();
    this.type = type;
    this.ascending = ascending;
  }

  public LeanSortMethod(Type type, boolean ascending, List<String> customOrder) {
    this.type = type;
    this.ascending = ascending;
    this.customOrder = customOrder;
  }

  public LeanSortMethod(LeanSortMethod m) {
    this();
    this.type = m.type;
    this.ascending = m.ascending;
    this.customOrder.addAll(m.customOrder);
  }

  public enum Type {
    NATIVE_VALUE,
    STRING_ALPHA,
    STRING_ALPHA_CASE_INSENSITIVE,
    STRING_NUMERIC,
    STRING_CUSTOM
  }
}
