package org.lean.core.gui.plugin;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.lean.core.LeanEnvironment;
import org.lean.core.gui.form.LeanGuiFormConstants;
import org.lean.presentation.component.types.label.LeanLabelComponent;

class LeanGuiRegistryTest {

  @BeforeAll
  static void init() throws Exception {
    LeanEnvironment.init();
  }

  @Test
  void scansLabelComponentWidgets() {
    LeanGuiRegistry registry = LeanGuiRegistry.getInstance();
    assertTrue(registry.isScanned());

    Map<String, List<LeanWidgetElements>> byParent =
        registry.getElementsByParent(LeanLabelComponent.class);
    assertFalse(byParent.isEmpty());

    List<LeanWidgetElements> pluginFields =
        byParent.getOrDefault(LeanGuiFormConstants.PARENT_PLUGIN, List.of());
    assertTrue(pluginFields.stream().anyMatch(w -> "label".equals(w.getFieldName())));
    assertTrue(pluginFields.stream().anyMatch(w -> "underline".equals(w.getFieldName())));

    List<LeanWidgetElements> baseFields =
        byParent.getOrDefault(LeanGuiFormConstants.PARENT_BASE, List.of());
    assertTrue(baseFields.stream().anyMatch(w -> "themeName".equals(w.getFieldName())));
  }
}
