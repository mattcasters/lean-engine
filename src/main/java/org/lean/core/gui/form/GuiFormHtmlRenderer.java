package org.lean.core.gui.form;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

/**
 * Renders a {@link GuiFormSchema} to HTML snippets compatible with lean-rest's side-panel editor
 * ({@code initScript}, {@code loadScript}, {@code componentSaveScript}).
 */
public class GuiFormHtmlRenderer {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  /**
   * Render a component editor form (presentation side panel): presentation name, component name,
   * plugin fields, base/layout, save via {@code saveComponent()}.
   */
  public String render(GuiFormSchema schema) {
    StringBuilder html = new StringBuilder();
    html.append("<!-- Generated form for ").append(esc(schema.getPluginId())).append(" -->\n");

    // Sticky actions above widgets so Apply/Close stay reachable on long forms
    appendFormActionBar(
        html,
        "component",
        "saveComponent()",
        "closeComponent()",
        null);

    // Presentation name (read-only context)
    html.append("<label for=\"presentationName\">Presentation name: </label>\n");
    html.append(
        "<input type=\"text\" id=\"presentationName\" name=\"presentationName\" readonly style=\"background: lightgray\">\n");
    html.append("<br>\n\n");

    for (GuiFormSection section : schema.getSections()) {
      renderSection(html, section, schema);
    }

    html.append("\n<br>\n");
    appendFormActionBar(
        html,
        "component-bottom",
        "saveComponent()",
        "closeComponent()",
        null);

    renderInitScript(html, schema);
    renderLoadScript(html, schema);
    renderSaveScript(html, schema);

    return html.toString();
  }

  /**
   * Render a standalone connector metadata form (side panel): connector name, plugin fields, save
   * via {@code saveConnector()}. Reuses the same field load/save helpers as components by aliasing
   * {@code iComponent} to the nested connector plugin payload.
   */
  public String renderConnector(GuiFormSchema schema) {
    StringBuilder html = new StringBuilder();
    html.append("<!-- Generated connector form for ")
        .append(esc(schema.getPluginId()))
        .append(" -->\n");

    appendFormActionBar(
        html,
        "connector",
        "saveConnector()",
        "closeConnector()",
        "editConnectorsList()");

    html.append("<label for=\"connectorName\">Connector name: </label>\n");
    html.append("<input type=\"text\" id=\"connectorName\" name=\"connectorName\">\n<br>\n");
    html.append("<input type=\"checkbox\" id=\"connectorShared\">\n");
    html.append("<label for=\"connectorShared\">Shared?</label>\n<br><br>\n");

    for (GuiFormSection section : schema.getSections()) {
      renderSection(html, section, schema);
    }

    html.append("\n<br>\n");
    appendFormActionBar(
        html,
        "connector-bottom",
        "saveConnector()",
        "closeConnector()",
        "editConnectorsList()");

    renderInitScript(html, schema);
    renderConnectorLoadScript(html, schema);
    renderConnectorSaveScript(html, schema);

    return html.toString();
  }

  /**
   * Apply / Close (and optional Back) actions for the side-panel editor.
   *
   * @param suffix unique id suffix so top and bottom bars do not clash
   * @param applyOnclick e.g. {@code saveComponent()}
   * @param closeOnclick e.g. {@code closeComponent()}
   * @param backOnclick optional third button (connector list); null to omit
   */
  private void appendFormActionBar(
      StringBuilder html,
      String suffix,
      String applyOnclick,
      String closeOnclick,
      String backOnclick) {
    html.append("<div class=\"form-action-bar\" id=\"formActionBar-")
        .append(esc(suffix))
        .append("\">\n");
    html.append("  <button type=\"button\" class=\"form-action-apply\" id=\"editButtonApply-")
        .append(esc(suffix))
        .append("\" onclick=\"")
        .append(applyOnclick)
        .append("\" title=\"Save changes and reload the presentation\">Apply</button>\n");
    html.append("  <button type=\"button\" class=\"form-action-close\" id=\"editButtonClose-")
        .append(esc(suffix))
        .append("\" onclick=\"")
        .append(closeOnclick)
        .append("\" title=\"Close the editor without saving\">Close</button>\n");
    if (StringUtils.isNotEmpty(backOnclick)) {
      html.append("  <button type=\"button\" class=\"form-action-back\" id=\"editButtonBack-")
          .append(esc(suffix))
          .append("\" onclick=\"")
          .append(backOnclick)
          .append("\" title=\"Return to the connector list\">Back to list</button>\n");
    }
    html.append("</div>\n\n");
  }

  private void renderConnectorLoadScript(StringBuilder html, GuiFormSchema schema) {
    html.append("<script id=\"loadScript\">\n");
    html.append(
        """
        if (typeof connectorJson === "undefined" || connectorJson === null) {
          throw "connectorJson is not set";
        }
        let iComponent = connectorJson["connector"][connectorPluginId];
        if (iComponent === undefined || iComponent === null) {
          // Create empty payload for new/unknown shape
          iComponent = { "pluginId": connectorPluginId };
          if (!connectorJson["connector"]) { connectorJson["connector"] = {}; }
          connectorJson["connector"][connectorPluginId] = iComponent;
        }
        document.getElementById("connectorName").value = connectorJson["name"] || "";
        document.getElementById("connectorShared").checked = !!connectorJson["shared"];
        """);
    if (hasListFields(schema)) {
      html.append(
          """
          let sourceConnectorName = iComponent["sourceConnectorName"];
          let sourceConnectorColumnNames = getConnectorColumnNames(sourceConnectorName);
          """);
    }
    for (GuiFormSection section : schema.getSections()) {
      for (GuiFormField field : section.getFields()) {
        appendLoad(html, field);
      }
    }
    html.append("</script>\n\n");
  }

  private void renderConnectorSaveScript(StringBuilder html, GuiFormSchema schema) {
    html.append("<script id=\"connectorSaveScript\">\n");
    html.append(
        """
        let iComponent = connectorJson["connector"][connectorPluginId];
        if (iComponent === undefined || iComponent === null) {
          iComponent = { "pluginId": connectorPluginId };
          if (!connectorJson["connector"]) { connectorJson["connector"] = {}; }
          connectorJson["connector"][connectorPluginId] = iComponent;
        }
        connectorJson["name"] = document.getElementById("connectorName").value;
        connectorJson["shared"] = document.getElementById("connectorShared").checked;
        iComponent["pluginId"] = connectorPluginId;
        """);
    for (GuiFormSection section : schema.getSections()) {
      for (GuiFormField field : section.getFields()) {
        appendSave(html, field);
      }
    }
    html.append("</script>\n");
  }

  private void renderSection(StringBuilder html, GuiFormSection section, GuiFormSchema schema) {
    if (LeanGuiFormConstants.SECTION_WRAPPER.equals(section.getId())) {
      // Flat fields at top (component name)
      for (GuiFormField field : section.getFields()) {
        renderSimpleField(html, field);
      }
      html.append("<br>\n");
      return;
    }

    String display = section.isOpenByDefault() ? "block" : "none";
    html.append("<button type=\"button\" class=\"collapsible\">")
        .append(esc(section.getTitle()))
        .append("</button>\n");
    html.append("<div class=\"content\" style=\"display: ").append(display).append("\">\n");

    // Layout shortcuts: one-click presets matching LeanLayout.fullPage() / topLeftPage()
    if (LeanGuiFormConstants.SECTION_LAYOUT.equals(section.getId())) {
      html.append("<div class=\"layout-shortcuts\">\n");
      html.append(
          "  <button type=\"button\" class=\"layout-shortcut-btn\" "
              + "onclick=\"if(typeof applyLayoutFullPage==='function')applyLayoutFullPage();\" "
              + "title=\"Left/top/right/bottom to page (0 offset)\">Full page</button>\n");
      html.append(
          "  <button type=\"button\" class=\"layout-shortcut-btn\" "
              + "onclick=\"if(typeof applyLayoutTopLeft==='function')applyLayoutTopLeft();\" "
              + "title=\"Left and top to page (0 offset)\">Top left</button>\n");
      html.append("</div>\n");
    }

    for (GuiFormField field : section.getFields()) {
      switch (field.getType()) {
        case LAYOUT_SIDE -> renderLayoutSide(html, field);
        case COLOR -> renderColorField(html, field);
        case FONT -> renderFontField(html, field);
        case SIZE -> renderSizeField(html, field);
        case LIST -> {
          if ("component".equals(field.getItemKind())) {
            renderComponentListField(html, field);
          } else {
            renderListField(html, field);
          }
        }
        case COMPONENT -> renderComponentField(html, field);
        default -> renderSimpleField(html, field);
      }
    }

    html.append("<br>\n</div>\n\n");
  }

  private void renderSimpleField(StringBuilder html, GuiFormField field) {
    String id = esc(field.getId());
    switch (field.getType()) {
      case CHECKBOX -> {
        html.append("<input type=\"checkbox\" id=\"").append(id).append("\">\n");
        html.append("<label for=\"")
            .append(id)
            .append("\">")
            .append(esc(field.getLabel()))
            .append("</label>\n<br>\n");
      }
      case COMBO, METADATA -> {
        html.append("<label for=\"")
            .append(id)
            .append("\">")
            .append(esc(field.getLabel()))
            .append(" </label>\n");
        html.append("<select id=\"")
            .append(id)
            .append("\" style=\"width: 50%\"></select>\n<br>\n");
      }
      case MULTI_LINE_TEXT -> {
        html.append("<label for=\"")
            .append(id)
            .append("\">")
            .append(esc(field.getLabel()))
            .append(" </label>\n<br>\n");
        int rows = Math.max(1, field.getMultiLineTextHeight());
        html.append("<textarea id=\"")
            .append(id)
            .append("\" rows=\"")
            .append(rows)
            .append("\" style=\"width: 90%\"></textarea>\n<br>\n");
      }
      case PASSWORD -> {
        html.append("<label for=\"")
            .append(id)
            .append("\">")
            .append(esc(field.getLabel()))
            .append(" </label>\n");
        html.append("<input type=\"password\" id=\"").append(id).append("\">\n<br>\n");
      }
      default -> {
        html.append("<label for=\"")
            .append(id)
            .append("\">")
            .append(esc(field.getLabel()))
            .append(" </label>\n");
        html.append("<input type=\"text\" id=\"").append(id).append("\">\n<br>\n");
      }
    }
  }

  private void renderColorField(StringBuilder html, GuiFormField field) {
    String id = esc(field.getId());
    String setId = "set" + capitalize(field.getId());
    // Match existing lean-rest conventions where possible
    if ("borderColor".equals(field.getFieldName())) {
      html.append("<input type=\"checkbox\" id=\"border\">\n");
      html.append("<label for=\"border\">Draw border? </label>\n");
      html.append("<input id=\"borderColor\" type=\"color\" style=\"width: 50%;border: none transparent\">\n<br>\n");
      return;
    }
    if ("backGroundColor".equals(field.getFieldName())) {
      html.append("<input type=\"checkbox\" id=\"background\">\n");
      html.append("<label for=\"background\">Draw background? </label>\n");
      html.append(
          "<input id=\"backGroundColor\" type=\"color\" style=\"width: 50%;border: none transparent\">\n<br>\n");
      return;
    }
    if ("defaultColor".equals(field.getFieldName())) {
      html.append("<input type=\"checkbox\" id=\"setDefaultColor\">\n");
      html.append("<label for=\"defaultColor\">Default color </label>\n");
      html.append(
          "<input id=\"defaultColor\" type=\"color\" style=\"width: 50%;border: none transparent\">\n<br>\n");
      return;
    }
    html.append("<input type=\"checkbox\" id=\"").append(setId).append("\">\n");
    html.append("<label for=\"")
        .append(id)
        .append("\">")
        .append(esc(field.getLabel()))
        .append(" </label>\n");
    html.append("<input id=\"")
        .append(id)
        .append("\" type=\"color\" style=\"width: 50%;border: none transparent\">\n<br>\n");
  }

  private void renderFontField(StringBuilder html, GuiFormField field) {
    if ("defaultFont".equals(field.getFieldName())) {
      html.append("<input type=\"checkbox\" id=\"setDefaultFont\">\n");
      html.append("<label for=\"defaultFontName\">Default font </label>\n");
      html.append("<input id=\"defaultFontName\" type=\"text\" style=\"width: 25%\">\n");
      html.append("<input id=\"defaultFontSize\" type=\"text\" style=\"width: 10%\">\n");
      html.append("<label for=\"defaultFontBold\">bold? </label>\n");
      html.append("<input id=\"defaultFontBold\" type=\"checkbox\">\n");
      html.append("<label for=\"defaultFontItalic\">italic? </label>\n");
      html.append("<input id=\"defaultFontItalic\" type=\"checkbox\">\n<br>\n");
      return;
    }
    String p = esc(field.getId());
    html.append("<input type=\"checkbox\" id=\"set").append(capitalize(p)).append("\">\n");
    html.append("<label>").append(esc(field.getLabel())).append(" </label>\n");
    html.append("<input id=\"").append(p).append("Name\" type=\"text\" style=\"width: 25%\">\n");
    html.append("<input id=\"").append(p).append("Size\" type=\"text\" style=\"width: 10%\">\n");
    html.append("<label>bold? </label><input id=\"").append(p).append("Bold\" type=\"checkbox\">\n");
    html.append("<label>italic? </label><input id=\"")
        .append(p)
        .append("Italic\" type=\"checkbox\">\n<br>\n");
  }

  private void renderComponentField(StringBuilder html, GuiFormField field) {
    String id = esc(field.getId());
    html.append("<fieldset class=\"nested-component-fieldset\" style=\"border: 1px solid #777; margin: 8px 0; padding: 8px;\">\n");
    html.append("<legend>").append(esc(field.getLabel())).append("</legend>\n");
    html.append("<div id=\"")
        .append(id)
        .append("_panel\" class=\"nested-component-panel\" data-prefix=\"")
        .append(id)
        .append("\" data-field=\"")
        .append(esc(field.getFieldName()))
        .append("\">\n");
    html.append("  <!-- filled by setNestedComponent() -->\n");
    html.append("</div>\n");
    html.append("</fieldset>\n");
  }

  private void renderComponentListField(StringBuilder html, GuiFormField field) {
    String id = esc(field.getId());
    html.append("<fieldset class=\"nested-component-list-fieldset\" style=\"border: 1px solid #777; margin: 8px 0; padding: 8px;\">\n");
    html.append("<legend>").append(esc(field.getLabel())).append("</legend>\n");
    html.append("<div id=\"")
        .append(id)
        .append("_items\" class=\"nested-component-list\" data-prefix=\"")
        .append(id)
        .append("\" data-field=\"")
        .append(esc(field.getFieldName()))
        .append("\"></div>\n");
    html.append("<button type=\"button\" id=\"")
        .append(id)
        .append("_add\" onclick=\"nestedComponentListAdd('")
        .append(id)
        .append("')\">Add child component</button>\n");
    html.append("</fieldset>\n");
  }

  private void renderListField(StringBuilder html, GuiFormField field) {
    String id = esc(field.getId());
    String kind = StringUtils.defaultIfEmpty(field.getItemKind(), "column");
    // Header label with Add toolbar (works even when the table is empty).
    // Delete stays on each data row (last column).
    html.append("<div class=\"list-field-header\">\n");
    html.append("<label for=\"")
        .append(id)
        .append("\">")
        .append(esc(field.getLabel()))
        .append("</label>\n");
    html.append("<span class=\"list-field-toolbar\">\n");
    html.append("<button type=\"button\" class=\"list-toolbar-btn\" id=\"")
        .append(id)
        .append("_add\" title=\"Add row\" onclick=\"listFieldAdd('")
        .append(id)
        .append("')\">");
    html.append(
        "<img src=\"/lean/api/static/images/add-item.svg\" alt=\"Add\" width=\"16\" height=\"16\">");
    html.append("</button>\n");
    html.append("</span>\n</div>\n");

    html.append("<table id=\"")
        .append(id)
        .append("\" class=\"list-field-table\" data-list-kind=\"")
        .append(esc(kind))
        .append("\" data-column-prefix=\"")
        .append(id)
        .append("\">\n<tr>\n");
    if ("fact".equals(kind)) {
      html.append(
          """
          <th>Column name</th>
          <th>Header</th>
          <th>Width</th>
          <th>H-Align</th>
          <th>V-align</th>
          <th>Format</th>
          <th>H-Agg</th>
          <th>V-Agg</th>
          <th>Method</th>
          <th></th>
          <th></th>
          <th></th>
          """);
    } else if ("string".equals(kind)) {
      html.append("<th>Value</th><th></th><th></th><th></th>\n");
    } else if ("sort".equals(kind)) {
      html.append("<th>Type</th><th>Ascending</th><th></th><th></th><th></th>\n");
    } else if ("filter".equals(kind)) {
      html.append("<th>Field name</th><th>Filter value</th><th></th><th></th><th></th>\n");
    } else if ("connector".equals(kind) || "bean".equals(kind)) {
      html.append("<th>Plugin JSON (advanced)</th><th></th><th></th><th></th>\n");
    } else {
      // column / dimension
      html.append(
          """
          <th>Column name</th>
          <th>Header value</th>
          <th>Width</th>
          <th>H-Align</th>
          <th>V-align</th>
          <th>Format</th>
          <th></th>
          <th></th>
          <th></th>
          """);
    }
    html.append("</tr>\n</table>\n<br>\n");
  }

  private void renderSizeField(StringBuilder html, GuiFormField field) {
    String id = esc(field.getId());
    html.append("<label>")
        .append(esc(field.getLabel()))
        .append(" </label>\n");
    html.append("<label for=\"")
        .append(id)
        .append("Width\">Width </label>\n");
    html.append("<input type=\"text\" id=\"")
        .append(id)
        .append("Width\" style=\"width: 15%\">\n");
    html.append("<label for=\"")
        .append(id)
        .append("Height\"> Height </label>\n");
    html.append("<input type=\"text\" id=\"")
        .append(id)
        .append("Height\" style=\"width: 15%\">\n<br>\n");
  }

  private void renderLayoutSide(StringBuilder html, GuiFormField field) {
    String side = field.getFieldName(); // left/right/top/bottom
    String cap = capitalize(side);
    html.append("<fieldset style=\"border-width: 1px;border-color: #777777\">\n");
    html.append("  <Legend>").append(cap).append(" alignment</Legend>\n");
    html.append("  <label for=\"")
        .append(side)
        .append("Enabled\">")
        .append(cap)
        .append(" aligned</label>");
    html.append("<input type=\"checkbox\" id=\"").append(side).append("Enabled\">\n");
    html.append("  <label for=\"")
        .append(side)
        .append("ObjectName\"> to </label>\n");
    html.append("  <select id=\"")
        .append(side)
        .append("ObjectName\" style=\"width: 50%\"></select><br>\n");
    html.append("  <label for=\"")
        .append(side)
        .append("Offset\">Offset: </label>");
    html.append("<input type=\"text\" id=\"")
        .append(side)
        .append("Offset\" style=\"width: 20%\">\n");
    html.append("  <label for=\"")
        .append(side)
        .append("Percentage\">Percentage: </label>");
    html.append("<input type=\"text\" id=\"")
        .append(side)
        .append("Percentage\" style=\"width: 20%\">\n");
    html.append("  <label for=\"")
        .append(side)
        .append("Alignment\">From </label>");
    html.append("<select id=\"")
        .append(side)
        .append("Alignment\" style=\"width: 20%\"></select><br>\n");
    html.append("</fieldset>\n");
  }

  private void renderInitScript(StringBuilder html, GuiFormSchema schema) {
    html.append("<script id=\"initScript\">\n");
    if (schema.getComponentCatalog() != null && !schema.getComponentCatalog().isEmpty()) {
      try {
        String catalogJson = MAPPER.writeValueAsString(schema.getComponentCatalog());
        html.append("window.componentCatalog = ")
            .append(catalogJson)
            .append(";\n");
      } catch (Exception e) {
        html.append("window.componentCatalog = [];\n");
      }
    } else {
      html.append("window.componentCatalog = window.componentCatalog || [];\n");
    }

    // Ensure presentation metadata caches are warm before binding selects
    html.append(
        "if (typeof ensureFormMetadataCaches === 'function') { ensureFormMetadataCaches(); }\n");

    for (GuiFormSection section : schema.getSections()) {
      for (GuiFormField field : section.getFields()) {
        if (field.getType() == GuiFormFieldType.COMBO
            || field.getType() == GuiFormFieldType.METADATA) {
          appendComboInit(html, field);
        }
        if (field.getType() == GuiFormFieldType.LAYOUT_SIDE) {
          String side = field.getFieldName();
          html.append("bindSelectSource('")
              .append(side)
              .append("ObjectName', 'components');\n");
          // LeanAttachment.Alignment (CENTER for vertical), not content LeanVerticalAlignment (MIDDLE)
          if ("left".equals(side) || "right".equals(side)) {
            html.append("setSelectOptions(\"")
                .append(side)
                .append("Alignment\", LAYOUT_HORIZONTAL_ALIGNMENTS);\n");
          } else {
            html.append("setSelectOptions(\"")
                .append(side)
                .append("Alignment\", LAYOUT_VERTICAL_ALIGNMENTS);\n");
          }
        }
        if (field.getType() == GuiFormFieldType.LIST
            && ("column".equals(field.getItemKind()) || "fact".equals(field.getItemKind()))) {
          // Column/fact tables use connector columns; refresh when source connector changes
          html.append("registerConnectorColumnListTable('")
              .append(esc(field.getId()))
              .append("', '")
              .append(esc(StringUtils.defaultIfEmpty(field.getComboDependsOn(), "sourceConnectorName")))
              .append("', '")
              .append(esc(StringUtils.defaultIfEmpty(field.getItemKind(), "column")))
              .append("');\n");
        }
        if (field.getType() == GuiFormFieldType.COMPONENT) {
          html.append("initNestedComponentPanel('")
              .append(esc(field.getId()))
              .append("');\n");
        }
        if (field.getType() == GuiFormFieldType.LIST && "component".equals(field.getItemKind())) {
          html.append("initNestedComponentList('")
              .append(esc(field.getId()))
              .append("');\n");
        }
      }
    }
    html.append(
        """
        if (typeof wireConnectorDependentCombos === 'function') { wireConnectorDependentCombos(); }
        $('.collapsible').click(function () {
                let c = $(this).next();
                if (c.css('display') === "block") {
                    c.css('display', 'none');
                } else {
                    c.css('display', 'block');
                }
            }
        );
        """);
    html.append("</script>\n\n");
  }

  private void appendComboInit(StringBuilder html, GuiFormField field) {
    String id = esc(field.getId());
    String source = StringUtils.defaultIfEmpty(field.getComboSource(), "none");
    List<String> values = field.getComboValues();
    if ("none".equals(source) && values != null && !values.isEmpty()) {
      html.append("setSelectOptions('").append(id).append("', ").append(toJsArray(values)).append(");\n");
      return;
    }
    if ("none".equals(source) || StringUtils.isEmpty(source)) {
      // still try enum-like static values
      if (values != null && !values.isEmpty()) {
        html.append("setSelectOptions('").append(id).append("', ").append(toJsArray(values)).append(");\n");
      }
      return;
    }
    String depends =
        StringUtils.defaultIfEmpty(field.getComboDependsOn(), "sourceConnectorName");
    String metaKey = StringUtils.defaultString(field.getMetadataKey());
    html.append("bindSelectSource('")
        .append(id)
        .append("', '")
        .append(esc(source))
        .append("', { dependsOn: '")
        .append(esc(depends))
        .append("', metadataKey: '")
        .append(esc(metaKey))
        .append("', staticValues: ")
        .append(values != null && !values.isEmpty() ? toJsArray(values) : "[]")
        .append(" });\n");
  }

  private void renderLoadScript(StringBuilder html, GuiFormSchema schema) {
    html.append("<script id=\"loadScript\">\n");
    html.append("let iComponent = componentJson[\"component\"][componentPluginId];\n");
    html.append("document.getElementById(\"presentationName\").value = presentationName;\n");
    if (hasListFields(schema)) {
      html.append(
          """
          let sourceConnectorName = iComponent["sourceConnectorName"];
          let sourceConnectorColumnNames = getConnectorColumnNames(sourceConnectorName);
          """);
    }

    for (GuiFormSection section : schema.getSections()) {
      for (GuiFormField field : section.getFields()) {
        appendLoad(html, field);
      }
    }
    html.append("</script>\n\n");
  }

  private boolean hasListFields(GuiFormSchema schema) {
    for (GuiFormSection section : schema.getSections()) {
      for (GuiFormField field : section.getFields()) {
        if (field.getType() == GuiFormFieldType.LIST
            && !"component".equals(field.getItemKind())) {
          return true;
        }
      }
    }
    return false;
  }

  private void appendLoad(StringBuilder html, GuiFormField field) {
    String id = esc(field.getId());
    String jsonId = esc(field.getFieldName());
    String target = "wrapper".equals(field.getBinding()) ? "componentJson" : "iComponent";

    switch (field.getType()) {
      case CHECKBOX ->
          html.append("setChecked(")
              .append(target)
              .append(", \"")
              .append(id)
              .append("\", \"")
              .append(jsonId)
              .append("\");\n");
      case COLOR -> appendColorLoad(html, field);
      case FONT -> {
        String setId = "defaultFont".equals(field.getFieldName()) ? "setDefaultFont" : "set" + capitalize(field.getFieldName());
        String prefix = field.getFieldName();
        if ("defaultFont".equals(field.getFieldName())) {
          prefix = "defaultFont";
        }
        html.append("setFont(iComponent, \"")
            .append(jsonId)
            .append("\", \"")
            .append(setId)
            .append("\", \"")
            .append(prefix)
            .append("\");\n");
      }
      case LIST -> {
        String kind = StringUtils.defaultIfEmpty(field.getItemKind(), "column");
        String prefix = id;
        if ("component".equals(kind)) {
          html.append("setNestedComponentList(iComponent, \"")
              .append(jsonId)
              .append("\", \"")
              .append(id)
              .append("\");\n");
        } else if ("fact".equals(kind)) {
          html.append("setFacts(iComponent, \"")
              .append(jsonId)
              .append("\", \"")
              .append(id)
              .append("\", \"")
              .append(prefix)
              .append("\", sourceConnectorColumnNames);\n");
        } else if ("string".equals(kind)) {
          html.append("setStringList(iComponent, \"")
              .append(jsonId)
              .append("\", \"")
              .append(id)
              .append("\");\n");
        } else if ("sort".equals(kind)) {
          html.append("setSortMethods(iComponent, \"")
              .append(jsonId)
              .append("\", \"")
              .append(id)
              .append("\");\n");
        } else if ("filter".equals(kind)) {
          html.append("setFilterValues(iComponent, \"")
              .append(jsonId)
              .append("\", \"")
              .append(id)
              .append("\");\n");
        } else if ("connector".equals(kind) || "bean".equals(kind)) {
          html.append("setJsonObjectList(iComponent, \"")
              .append(jsonId)
              .append("\", \"")
              .append(id)
              .append("\");\n");
        } else {
          html.append("setColumns(iComponent, \"")
              .append(jsonId)
              .append("\", \"")
              .append(id)
              .append("\", \"")
              .append(prefix)
              .append("\", sourceConnectorColumnNames);\n");
        }
      }
      case COMPONENT ->
          html.append("setNestedComponent(iComponent, \"")
              .append(jsonId)
              .append("\", \"")
              .append(id)
              .append("\");\n");
      case LAYOUT_SIDE ->
          html.append("setLayout(componentJson, \"").append(jsonId).append("\");\n");
      case SIZE ->
          html.append("setSize(")
              .append(target)
              .append(", \"")
              .append(id)
              .append("\", \"")
              .append(jsonId)
              .append("\");\n");
      case BUTTON, LINK -> {
        // no value binding
      }
      default -> {
        if ("name".equals(field.getFieldName()) && "wrapper".equals(field.getBinding())) {
          html.append("setElement(componentJson, \"componentName\", \"name\");\n");
        } else {
          html.append("setElement(")
              .append(target)
              .append(", \"")
              .append(id)
              .append("\", \"")
              .append(jsonId)
              .append("\");\n");
        }
      }
    }
  }

  private void appendColorLoad(StringBuilder html, GuiFormField field) {
    String jsonId = field.getFieldName();
    if ("borderColor".equals(jsonId)) {
      html.append(
          "setColor(iComponent, \"borderColor\", \"border\", \"borderColor\", \"#000000\");\n");
    } else if ("backGroundColor".equals(jsonId)) {
      html.append(
          "setColor(iComponent, \"backGroundColor\", \"background\", \"backGroundColor\", \"#ffffff\");\n");
    } else if ("defaultColor".equals(jsonId)) {
      html.append(
          "setColor(iComponent, \"defaultColor\", \"setDefaultColor\", \"defaultColor\", \"#ffffff\");\n");
    } else {
      String setId = "set" + capitalize(jsonId);
      html.append("setColor(iComponent, \"")
          .append(jsonId)
          .append("\", \"")
          .append(setId)
          .append("\", \"")
          .append(jsonId)
          .append("\", \"#000000\");\n");
    }
  }

  private void renderSaveScript(StringBuilder html, GuiFormSchema schema) {
    html.append("<script id=\"componentSaveScript\">\n");
    html.append("let iComponent = componentJson[\"component\"][componentPluginId];\n");

    for (GuiFormSection section : schema.getSections()) {
      for (GuiFormField field : section.getFields()) {
        appendSave(html, field);
      }
    }
    html.append("</script>\n");
  }

  private void appendSave(StringBuilder html, GuiFormField field) {
    String id = esc(field.getId());
    String jsonId = esc(field.getFieldName());
    String target = "wrapper".equals(field.getBinding()) ? "componentJson" : "iComponent";

    switch (field.getType()) {
      case CHECKBOX ->
          html.append("getChecked(")
              .append(target)
              .append(", \"")
              .append(id)
              .append("\", \"")
              .append(jsonId)
              .append("\");\n");
      case COLOR -> appendColorSave(html, field);
      case FONT -> {
        String setId =
            "defaultFont".equals(field.getFieldName())
                ? "setDefaultFont"
                : "set" + capitalize(field.getFieldName());
        String prefix = field.getFieldName();
        html.append("getFont(iComponent, \"")
            .append(jsonId)
            .append("\", \"")
            .append(setId)
            .append("\", \"")
            .append(prefix)
            .append("\");\n");
      }
      case LIST -> {
        String kind = StringUtils.defaultIfEmpty(field.getItemKind(), "column");
        if ("component".equals(kind)) {
          html.append("getNestedComponentList(iComponent, \"")
              .append(jsonId)
              .append("\", \"")
              .append(id)
              .append("\");\n");
        } else if ("fact".equals(kind)) {
          html.append("getFacts(iComponent, \"")
              .append(jsonId)
              .append("\", \"")
              .append(id)
              .append("\");\n");
        } else if ("string".equals(kind)) {
          html.append("getStringList(iComponent, \"")
              .append(jsonId)
              .append("\", \"")
              .append(id)
              .append("\");\n");
        } else if ("sort".equals(kind)) {
          html.append("getSortMethods(iComponent, \"")
              .append(jsonId)
              .append("\", \"")
              .append(id)
              .append("\");\n");
        } else if ("filter".equals(kind)) {
          html.append("getFilterValues(iComponent, \"")
              .append(jsonId)
              .append("\", \"")
              .append(id)
              .append("\");\n");
        } else if ("connector".equals(kind) || "bean".equals(kind)) {
          html.append("getJsonObjectList(iComponent, \"")
              .append(jsonId)
              .append("\", \"")
              .append(id)
              .append("\");\n");
        } else {
          html.append("getColumns(iComponent, \"")
              .append(jsonId)
              .append("\", \"")
              .append(id)
              .append("\");\n");
        }
      }
      case COMPONENT ->
          html.append("getNestedComponent(iComponent, \"")
              .append(jsonId)
              .append("\", \"")
              .append(id)
              .append("\");\n");
      case LAYOUT_SIDE ->
          html.append("getLayout(componentJson, \"").append(jsonId).append("\");\n");
      case SIZE ->
          html.append("getSize(")
              .append(target)
              .append(", \"")
              .append(id)
              .append("\", \"")
              .append(jsonId)
              .append("\");\n");
      case BUTTON, LINK -> {}
      default -> {
        if ("name".equals(field.getFieldName()) && "wrapper".equals(field.getBinding())) {
          html.append(
              "componentJson[\"name\"] = document.getElementById(\"componentName\").value;\n");
        } else if (field.isIntegerValue()) {
          html.append("getElementInteger(")
              .append(target)
              .append(", \"")
              .append(id)
              .append("\", \"")
              .append(jsonId)
              .append("\");\n");
        } else {
          html.append("getElement(")
              .append(target)
              .append(", \"")
              .append(id)
              .append("\", \"")
              .append(jsonId)
              .append("\");\n");
        }
      }
    }
  }

  private void appendColorSave(StringBuilder html, GuiFormField field) {
    String jsonId = field.getFieldName();
    if ("borderColor".equals(jsonId)) {
      html.append("getColor(iComponent, \"borderColor\", \"border\", \"borderColor\");\n");
    } else if ("backGroundColor".equals(jsonId)) {
      html.append(
          "getColor(iComponent, \"backGroundColor\", \"background\", \"backGroundColor\");\n");
    } else if ("defaultColor".equals(jsonId)) {
      html.append(
          "getColor(iComponent, \"defaultColor\", \"setDefaultColor\", \"defaultColor\");\n");
    } else {
      String setId = "set" + capitalize(jsonId);
      html.append("getColor(iComponent, \"")
          .append(jsonId)
          .append("\", \"")
          .append(setId)
          .append("\", \"")
          .append(jsonId)
          .append("\");\n");
    }
  }

  private String toJsArray(List<String> values) {
    StringBuilder sb = new StringBuilder("[");
    for (int i = 0; i < values.size(); i++) {
      if (i > 0) {
        sb.append(", ");
      }
      sb.append("'").append(values.get(i).replace("'", "\\'")).append("'");
    }
    sb.append("]");
    return sb.toString();
  }

  private static String esc(String s) {
    if (s == null) {
      return "";
    }
    return s.replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
  }

  private static String capitalize(String s) {
    if (StringUtils.isEmpty(s)) {
      return s;
    }
    return Character.toUpperCase(s.charAt(0)) + s.substring(1);
  }
}
