package org.lean.core.gui.form;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.List;
import org.apache.hop.core.plugins.IPlugin;
import org.apache.hop.core.plugins.PluginRegistry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.lean.core.LeanEnvironment;
import org.lean.presentation.component.type.LeanComponentPluginType;
import org.lean.presentation.connector.type.LeanConnectorPluginType;

/**
 * Ensures every registered Lean component and connector plugin can produce a form schema (so web
 * editors do not require hand-written HTML).
 */
class PluginFormCoverageTest {

  @BeforeAll
  static void init() throws Exception {
    LeanEnvironment.init();
  }

  @Test
  void everyComponentPluginHasFormSchema() {
    GuiFormSchemaBuilder builder = new GuiFormSchemaBuilder();
    List<String> failures = new ArrayList<>();
    for (IPlugin plugin : PluginRegistry.getInstance().getPlugins(LeanComponentPluginType.class)) {
      String id = plugin.getIds()[0];
      try {
        GuiFormSchema schema = builder.buildComponentSchema(id);
        assertNotNull(schema);
        assertFalse(schema.getSections().isEmpty(), id + " has no form sections");
        // Generated HTML should at least include save/load scripts
        String html = new GuiFormHtmlRenderer().render(schema);
        assertTrue(html.contains("componentSaveScript"), id + " HTML missing save script");
      } catch (Exception e) {
        failures.add(id + ": " + e.getMessage());
      }
    }
    if (!failures.isEmpty()) {
      fail("Component form schema failures:\n" + String.join("\n", failures));
    }
  }

  @Test
  void everyConnectorPluginHasFormSchema() {
    GuiFormSchemaBuilder builder = new GuiFormSchemaBuilder();
    List<String> failures = new ArrayList<>();
    for (IPlugin plugin : PluginRegistry.getInstance().getPlugins(LeanConnectorPluginType.class)) {
      String id = plugin.getIds()[0];
      try {
        GuiFormSchema schema = builder.buildConnectorSchema(id);
        assertNotNull(schema);
        assertTrue(
            schema.isHasPluginWidgets() || !schema.getSections().isEmpty(),
            id + " produced an empty connector schema");
        String html = new GuiFormHtmlRenderer().renderConnector(schema);
        assertTrue(html.contains("connectorSaveScript"), id + " connector HTML missing save script");
      } catch (Exception e) {
        failures.add(id + ": " + e.getMessage());
      }
    }
    if (!failures.isEmpty()) {
      fail("Connector form schema failures:\n" + String.join("\n", failures));
    }
  }

  @Test
  void simpleFilterAndSortHaveTypedLists() throws Exception {
    GuiFormSchemaBuilder builder = new GuiFormSchemaBuilder();

    GuiFormSchema sort = builder.buildConnectorSchema("SortConnector");
    GuiFormField sortMethods = findField(sort, "sortMethods");
    assertNotNull(sortMethods);
    assertTrue(sortMethods.getType() == GuiFormFieldType.LIST);
    assertTrue("sort".equals(sortMethods.getItemKind()));

    GuiFormSchema filter = builder.buildConnectorSchema("SimpleFilterConnector");
    GuiFormField filters = findField(filter, "filterValues");
    assertNotNull(filters);
    assertTrue(filters.getType() == GuiFormFieldType.LIST);
    assertTrue("filter".equals(filters.getItemKind()));
  }

  @Test
  void connectorFormsRenderHtmlWithSaveScript() throws Exception {
    GuiFormSchema schema = new GuiFormSchemaBuilder().buildConnectorSchema("SqlConnector");
    String html = new GuiFormHtmlRenderer().renderConnector(schema);
    assertTrue(html.contains("connectorName"));
    assertTrue(html.contains("connectorSaveScript"));
    assertTrue(html.contains("saveConnector()"));
    assertTrue(html.contains("form-action-bar"));
    assertTrue(html.contains("Apply"));
    assertTrue(html.contains("closeConnector()"));
    assertTrue(html.contains("databaseConnectionName") || html.contains("sql"));
  }

  @Test
  void componentFormsHaveTopApplyAndClose() throws Exception {
    GuiFormSchema schema = new GuiFormSchemaBuilder().buildComponentSchema("LeanLabelComponent");
    String html = new GuiFormHtmlRenderer().render(schema);
    assertTrue(html.contains("form-action-bar"));
    assertTrue(html.contains("saveComponent()"));
    assertTrue(html.contains("closeComponent()"));
    // Action bar appears before the first field section widgets
    int bar = html.indexOf("form-action-bar");
    int labelField = html.indexOf("id=\"label\"");
    assertTrue(bar >= 0 && labelField > bar, "Apply/Close bar should be above field widgets");
  }

  private GuiFormField findField(GuiFormSchema schema, String id) {
    for (GuiFormSection section : schema.getSections()) {
      for (GuiFormField field : section.getFields()) {
        if (id.equals(field.getId()) || id.equals(field.getFieldName())) {
          return field;
        }
      }
    }
    return null;
  }
}
