package org.lean.core.gui.form;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.hop.core.plugins.IPlugin;
import org.apache.hop.core.plugins.PluginRegistry;
import org.lean.core.LeanColorRGB;
import org.lean.core.LeanColumn;
import org.lean.core.LeanDimension;
import org.lean.core.LeanFact;
import org.lean.core.LeanFont;
import org.lean.core.LeanHorizontalAlignment;
import org.lean.core.LeanSize;
import org.lean.core.LeanSortMethod;
import org.lean.core.LeanVerticalAlignment;
import org.lean.core.exception.LeanException;
import org.lean.core.gui.plugin.LeanComboSource;
import org.lean.core.gui.plugin.LeanGuiRegistry;
import org.lean.core.gui.plugin.LeanWidgetElements;
import org.lean.core.gui.plugin.LeanWidgetType;
import org.lean.presentation.component.LeanComponent;
import org.lean.presentation.component.type.ILeanComponent;
import org.lean.presentation.component.type.LeanComponentPlugin;
import org.lean.presentation.component.type.LeanComponentPluginType;
import org.lean.presentation.connector.type.ILeanConnector;
import org.lean.presentation.connector.type.LeanConnectorPlugin;
import org.lean.presentation.connector.type.LeanConnectorPluginType;
import org.lean.presentation.connector.types.filter.SimpleFilterValue;

/**
 * Builds {@link GuiFormSchema} from {@link org.lean.core.gui.plugin.LeanWidgetElement} annotations
 * on Lean plugin classes (via {@link LeanGuiRegistry}), plus shared wrapper / base / layout
 * sections for the browser editor.
 */
public class GuiFormSchemaBuilder {

  /** Max nesting depth for COMPONENT fields inside the component catalog (Group→Composite→…). */
  public static final int MAX_NESTED_COMPONENT_DEPTH = 3;

  /**
   * Build a form schema for a component plugin id (e.g. {@code LeanLabelComponent}).
   *
   * @param pluginId plugin id from {@link LeanComponentPlugin#id()}
   * @return schema including shared sections; {@link GuiFormSchema#isHasPluginWidgets()} is true
   *     when the plugin class declared at least one {@code @LeanWidgetElement}
   */
  public GuiFormSchema buildComponentSchema(String pluginId) throws LeanException {
    GuiFormSchema schema = buildComponentSchemaInternal(pluginId, true, 0);
    if (schemaNeedsComponentCatalog(schema)) {
      schema.setComponentCatalog(buildComponentCatalog(0));
    }
    return schema;
  }

  /**
   * Build plugin + base (+ optional wrapper/layout) schema without attaching a catalog (used when
   * building catalog entries).
   */
  private GuiFormSchema buildComponentSchemaInternal(
      String pluginId, boolean includeWrapperAndLayout, int nestedDepth) throws LeanException {
    IPlugin plugin = PluginRegistry.getInstance().findPluginWithId(LeanComponentPluginType.class, pluginId);
    if (plugin == null) {
      throw new LeanException("Component plugin not found: " + pluginId);
    }

    Class<? extends ILeanComponent> componentClass = loadComponentClass(plugin, pluginId);

    LeanComponentPlugin annotation = componentClass.getAnnotation(LeanComponentPlugin.class);
    String name = annotation != null ? annotation.name() : plugin.getName();
    String description = annotation != null ? annotation.description() : plugin.getDescription();

    GuiFormSchema schema = new GuiFormSchema(pluginId, name);
    schema.setPluginDescription(description);
    schema.setPluginClassName(componentClass.getName());

    Map<String, List<GuiFormField>> byParent = collectAnnotatedFields(componentClass);

    if (includeWrapperAndLayout) {
      schema.getSections().add(buildWrapperSection());
    }

    // Plugin-specific (parent LeanComponent-Plugin or any parent not base)
    GuiFormSection pluginSection =
        buildSectionFromFields(
            LeanGuiFormConstants.SECTION_PLUGIN,
            name,
            true,
            byParent.getOrDefault(LeanGuiFormConstants.PARENT_PLUGIN, List.of()));
    // Also include fields with empty parent or custom parents under plugin section
    for (Map.Entry<String, List<GuiFormField>> entry : byParent.entrySet()) {
      String parentId = entry.getKey();
      if (LeanGuiFormConstants.PARENT_PLUGIN.equals(parentId)
          || LeanGuiFormConstants.PARENT_BASE.equals(parentId)
          || LeanGuiFormConstants.PARENT_WRAPPER.equals(parentId)
          || LeanGuiFormConstants.PARENT_LAYOUT.equals(parentId)
          || LeanGuiFormConstants.PARENT_COMPONENT_PROPS.equals(parentId)) {
        continue;
      }
      pluginSection.getFields().addAll(entry.getValue());
    }

    // At max depth, strip nested component fields so the catalog stays finite
    if (nestedDepth >= MAX_NESTED_COMPONENT_DEPTH) {
      pluginSection
          .getFields()
          .removeIf(
              f ->
                  f.getType() == GuiFormFieldType.COMPONENT
                      || (f.getType() == GuiFormFieldType.LIST
                          && "component".equals(f.getItemKind())));
    }

    sortFields(pluginSection.getFields());
    if (!pluginSection.getFields().isEmpty()) {
      schema.setHasPluginWidgets(true);
      schema.getSections().add(pluginSection);
    }

    // Base component options from annotations on LeanBaseComponent, else defaults.
    List<GuiFormField> baseFields =
        byParent.getOrDefault(LeanGuiFormConstants.PARENT_BASE, new ArrayList<>());
    if (baseFields.isEmpty()) {
      baseFields = defaultBaseFields();
    }
    GuiFormSection baseSection =
        buildSectionFromFields(
            LeanGuiFormConstants.SECTION_BASE, "General component options", false, baseFields);
    schema.getSections().add(baseSection);

    if (includeWrapperAndLayout) {
      // LeanComponent-level rotation / transparency / clip size
      schema.getSections().add(buildComponentPropertiesSection());
      schema.getSections().add(buildLayoutSection());
    }

    return schema;
  }

  private Class<? extends ILeanComponent> loadComponentClass(IPlugin plugin, String pluginId)
      throws LeanException {
    try {
      String className = plugin.getClassMap().get(ILeanComponent.class);
      if (className == null) {
        throw new LeanException("No main class mapping for component plugin " + pluginId);
      }
      ClassLoader classLoader = PluginRegistry.getInstance().getClassLoader(plugin);
      @SuppressWarnings("unchecked")
      Class<? extends ILeanComponent> loaded =
          (Class<? extends ILeanComponent>) classLoader.loadClass(className);
      return loaded;
    } catch (LeanException e) {
      throw e;
    } catch (Exception e) {
      throw new LeanException("Unable to load class for component plugin " + pluginId, e);
    }
  }

  private boolean schemaNeedsComponentCatalog(GuiFormSchema schema) {
    for (GuiFormSection section : schema.getSections()) {
      for (GuiFormField field : section.getFields()) {
        if (field.getType() == GuiFormFieldType.COMPONENT) {
          return true;
        }
        if (field.getType() == GuiFormFieldType.LIST && "component".equals(field.getItemKind())) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Build a catalog of all registered component plugins for nested editors.
   *
   * @param nestedDepth depth used when building each type's field list (0 = top of catalog)
   */
  public List<GuiFormComponentTypeInfo> buildComponentCatalog(int nestedDepth)
      throws LeanException {
    List<GuiFormComponentTypeInfo> catalog = new ArrayList<>();
    PluginRegistry registry = PluginRegistry.getInstance();
    List<IPlugin> plugins = registry.getPlugins(LeanComponentPluginType.class);
    for (IPlugin plugin : plugins) {
      String pluginId = plugin.getIds()[0];
      try {
        GuiFormSchema typeSchema = buildComponentSchemaInternal(pluginId, false, nestedDepth + 1);
        GuiFormComponentTypeInfo info =
            new GuiFormComponentTypeInfo(
                pluginId, typeSchema.getPluginName(), typeSchema.getPluginDescription());
        info.setSections(typeSchema.getSections());
        catalog.add(info);
      } catch (Exception e) {
        // Skip plugins that cannot be schema-built
      }
    }
    catalog.sort(Comparator.comparing(GuiFormComponentTypeInfo::getPluginId));
    return catalog;
  }

  /**
   * Whether a component plugin has a usable generated form (always true for known plugins once
   * shared sections exist; preferred when annotations are present).
   */
  public boolean canBuildComponentSchema(String pluginId) {
    try {
      IPlugin plugin =
          PluginRegistry.getInstance().findPluginWithId(LeanComponentPluginType.class, pluginId);
      return plugin != null;
    } catch (Exception e) {
      return false;
    }
  }

  private Map<String, List<GuiFormField>> collectAnnotatedFields(Class<?> clazz) {
    Map<String, List<LeanWidgetElements>> byParentWidgets =
        LeanGuiRegistry.getInstance().getElementsByParent(clazz);
    Map<String, List<GuiFormField>> byParent = new LinkedHashMap<>();
    for (Map.Entry<String, List<LeanWidgetElements>> entry : byParentWidgets.entrySet()) {
      List<GuiFormField> fields = new ArrayList<>();
      for (LeanWidgetElements widget : entry.getValue()) {
        if (widget.isIgnored()) {
          continue;
        }
        fields.add(toFormField(widget));
      }
      sortFields(fields);
      byParent.put(entry.getKey(), fields);
    }
    return byParent;
  }

  private GuiFormField toFormField(LeanWidgetElements widget) {
    Field field = widget.getField();
    Class<?> ownerClass = widget.getOwnerClass();
    GuiFormField formField = new GuiFormField();
    formField.setId(widget.getId());
    formField.setOrder(widget.getOrder());
    formField.setLabel(
        StringUtils.isEmpty(widget.getLabel()) ? widget.getFieldName() : widget.getLabel());
    formField.setToolTip(widget.getToolTip());
    formField.setTabName(widget.getTabName());
    formField.setTabTooltip(widget.getTabTooltip());
    formField.setFieldName(widget.getFieldName());
    formField.setPassword(widget.isPassword());
    formField.setVariablesEnabled(widget.isVariablesEnabled());
    formField.setMultiLineTextHeight(widget.getMultiLineTextHeight());
    formField.setBinding(bindingForParent(widget.getParentId()));
    formField.setType(mapType(widget, field));

    Class<?> fieldType = field.getType();
    if (fieldType == int.class
        || fieldType == Integer.class
        || fieldType == long.class
        || fieldType == Long.class) {
      formField.setIntegerValue(true);
    }

    if (formField.getType() == GuiFormFieldType.LIST) {
      Class<?> itemClass = resolveListItemClass(field);
      if (itemClass != null) {
        formField.setItemClassName(itemClass.getName());
        formField.setItemKind(resolveItemKind(itemClass));
      } else {
        formField.setItemKind("string");
      }
    }

    if (formField.getType() == GuiFormFieldType.COMBO
        || formField.getType() == GuiFormFieldType.METADATA) {
      formField.setComboValues(resolveComboValues(widget, field, ownerClass));
      applyComboSource(formField, widget, field);
    }
    return formField;
  }

  /**
   * Resolve dynamic combo source from annotation or well-known field names / widget types.
   */
  private void applyComboSource(GuiFormField formField, LeanWidgetElements widget, Field field) {
    LeanComboSource source =
        widget.getComboSource() == null ? LeanComboSource.NONE : widget.getComboSource();
    String metadataKey = StringUtils.defaultString(widget.getMetadataKey());
    String dependsOn = StringUtils.defaultString(widget.getDependsOn());

    // Infer when not explicitly set
    if (source == LeanComboSource.NONE) {
      String name = field.getName();
      if ("sourceConnectorName".equals(name)) {
        source = LeanComboSource.CONNECTORS;
      } else if ("themeName".equals(name)) {
        source = LeanComboSource.THEMES;
      } else if ("databaseConnectionName".equals(name)
          || formField.getType() == GuiFormFieldType.METADATA) {
        source = LeanComboSource.METADATA;
        if (StringUtils.isEmpty(metadataKey)) {
          if ("databaseConnectionName".equals(name)) {
            metadataKey = "lean-database-connection";
          } else if ("themeName".equals(name)) {
            metadataKey = "theme";
          }
        }
      }
    }

    formField.setComboSource(comboSourceName(source));
    if (StringUtils.isNotEmpty(dependsOn)) {
      formField.setComboDependsOn(dependsOn);
    } else if (source == LeanComboSource.CONNECTOR_COLUMNS) {
      formField.setComboDependsOn("sourceConnectorName");
    }
    if (StringUtils.isNotEmpty(metadataKey)) {
      formField.setMetadataKey(metadataKey);
    } else if (source == LeanComboSource.METADATA && StringUtils.isEmpty(formField.getMetadataKey())) {
      // keep empty — client may still list nothing useful
    }
  }

  private static String comboSourceName(LeanComboSource source) {
    if (source == null || source == LeanComboSource.NONE) {
      return "none";
    }
    return switch (source) {
      case CONNECTORS -> "connectors";
      case THEMES -> "themes";
      case COMPONENTS -> "components";
      case CONNECTOR_COLUMNS -> "connectorColumns";
      case METADATA -> "metadata";
      default -> "none";
    };
  }

  private Class<?> resolveListItemClass(Field field) {
    Type generic = field.getGenericType();
    if (generic instanceof ParameterizedType parameterized) {
      Type[] args = parameterized.getActualTypeArguments();
      if (args.length == 1 && args[0] instanceof Class<?> itemClass) {
        return itemClass;
      }
    }
    return null;
  }

  private String resolveItemKind(Class<?> itemClass) {
    if (LeanFact.class.isAssignableFrom(itemClass)) {
      return "fact";
    }
    if (LeanColumn.class.isAssignableFrom(itemClass)
        || LeanDimension.class.isAssignableFrom(itemClass)) {
      return "column";
    }
    if (LeanComponent.class.isAssignableFrom(itemClass)) {
      return "component";
    }
    if (LeanSortMethod.class.isAssignableFrom(itemClass)) {
      return "sort";
    }
    if (SimpleFilterValue.class.isAssignableFrom(itemClass)) {
      return "filter";
    }
    if (ILeanConnector.class.isAssignableFrom(itemClass)) {
      return "connector";
    }
    if (String.class.equals(itemClass)) {
      return "string";
    }
    return "bean";
  }

  private String bindingForParent(String parentId) {
    if (LeanGuiFormConstants.PARENT_WRAPPER.equals(parentId)
        || LeanGuiFormConstants.PARENT_LAYOUT.equals(parentId)
        || LeanGuiFormConstants.PARENT_COMPONENT_PROPS.equals(parentId)) {
      return "wrapper";
    }
    return "plugin";
  }

  private GuiFormFieldType mapType(LeanWidgetElements widget, Field field) {
    Class<?> type = field.getType();
    if (LeanComponent.class.isAssignableFrom(type)) {
      return GuiFormFieldType.COMPONENT;
    }
    if (List.class.isAssignableFrom(type)) {
      return GuiFormFieldType.LIST;
    }
    if (LeanColorRGB.class.isAssignableFrom(type)) {
      return GuiFormFieldType.COLOR;
    }
    if (LeanFont.class.isAssignableFrom(type)) {
      return GuiFormFieldType.FONT;
    }
    if (LeanSize.class.isAssignableFrom(type)) {
      return GuiFormFieldType.SIZE;
    }
    if (widget.isPassword()) {
      return GuiFormFieldType.PASSWORD;
    }
    if (type == boolean.class || type == Boolean.class) {
      return GuiFormFieldType.CHECKBOX;
    }
    if (type.isEnum()) {
      return GuiFormFieldType.COMBO;
    }
    LeanWidgetType elementType = widget.getType();
    if (elementType == null) {
      return GuiFormFieldType.TEXT;
    }
    return switch (elementType) {
      case TEXT -> GuiFormFieldType.TEXT;
      case MULTI_LINE_TEXT -> GuiFormFieldType.MULTI_LINE_TEXT;
      case FILENAME -> GuiFormFieldType.FILENAME;
      case FOLDER -> GuiFormFieldType.FOLDER;
      case CHECKBOX -> GuiFormFieldType.CHECKBOX;
      case COMBO -> GuiFormFieldType.COMBO;
      case METADATA -> GuiFormFieldType.METADATA;
      case BUTTON -> GuiFormFieldType.BUTTON;
      case LINK -> GuiFormFieldType.LINK;
      default -> GuiFormFieldType.TEXT;
    };
  }

  private List<String> resolveComboValues(
      LeanWidgetElements widget, Field field, Class<?> ownerClass) {
    Class<?> type = field.getType();
    if (type.isEnum()) {
      Object[] constants = type.getEnumConstants();
      List<String> values = new ArrayList<>();
      for (Object c : constants) {
        values.add(((Enum<?>) c).name());
      }
      return values;
    }
    if (StringUtils.isNotEmpty(widget.getComboValuesMethod())) {
      try {
        Method method = ownerClass.getMethod(widget.getComboValuesMethod());
        Object instance = ownerClass.getDeclaredConstructor().newInstance();
        Object result = method.invoke(instance);
        if (result instanceof String[] array) {
          return Arrays.asList(array);
        }
        if (result instanceof List<?> list) {
          List<String> values = new ArrayList<>();
          for (Object o : list) {
            values.add(String.valueOf(o));
          }
          return values;
        }
      } catch (Exception e) {
        // leave empty; client may fill dynamically
      }
    }
    return new ArrayList<>();
  }

  private GuiFormSection buildWrapperSection() {
    GuiFormSection section =
        new GuiFormSection(LeanGuiFormConstants.SECTION_WRAPPER, "Component", true);
    GuiFormField name = new GuiFormField("componentName", GuiFormFieldType.TEXT, "Component name", "name");
    name.setBinding("wrapper");
    name.setOrder("00100");
    section.getFields().add(name);
    return section;
  }

  private List<GuiFormField> defaultBaseFields() {
    List<GuiFormField> fields = new ArrayList<>();
    // COLOR fields include their enable checkboxes (border / background / setDefaultColor)
    GuiFormField source =
        baseField("sourceConnectorName", GuiFormFieldType.COMBO, "Source connector", "01000");
    source.setComboSource("connectors");
    fields.add(source);
    GuiFormField theme = baseField("themeName", GuiFormFieldType.COMBO, "Theme name", "01100");
    theme.setComboSource("themes");
    fields.add(theme);
    fields.add(baseField("borderColor", GuiFormFieldType.COLOR, "Border color", "01200"));
    fields.add(baseField("backGroundColor", GuiFormFieldType.COLOR, "Background color", "01300"));
    fields.add(baseField("defaultFont", GuiFormFieldType.FONT, "Default font", "01400"));
    fields.add(baseField("defaultColor", GuiFormFieldType.COLOR, "Default color", "01500"));
    return fields;
  }

  private GuiFormField baseField(String name, GuiFormFieldType type, String label, String order) {
    GuiFormField f = new GuiFormField(name, type, label, name);
    f.setBinding("plugin");
    f.setOrder(order);
    return f;
  }

  /**
   * Wrapper properties on {@link LeanComponent}: rotation (degrees), transparency (0–100), and
   * optional clip size (width × height).
   */
  private GuiFormSection buildComponentPropertiesSection() {
    GuiFormSection section =
        new GuiFormSection(
            LeanGuiFormConstants.SECTION_COMPONENT_PROPS, "Component properties", false);

    GuiFormField rotation =
        new GuiFormField("rotation", GuiFormFieldType.TEXT, "Rotation (degrees)", "rotation");
    rotation.setBinding("wrapper");
    rotation.setOrder("03010");
    rotation.setToolTip("Rotation angle in degrees around the component center");
    section.getFields().add(rotation);

    GuiFormField transparency =
        new GuiFormField(
            "transparency", GuiFormFieldType.TEXT, "Transparency (0-100)", "transparency");
    transparency.setBinding("wrapper");
    transparency.setOrder("03020");
    transparency.setToolTip("Opacity as a percentage: 0 = invisible, 100 = fully opaque");
    section.getFields().add(transparency);

    GuiFormField clipSize =
        new GuiFormField("clipSize", GuiFormFieldType.SIZE, "Clip size", "clipSize");
    clipSize.setBinding("wrapper");
    clipSize.setOrder("03030");
    clipSize.setToolTip("When width and height are set (>0), drawing is clipped to the component geometry");
    section.getFields().add(clipSize);

    return section;
  }

  private GuiFormSection buildLayoutSection() {
    GuiFormSection section =
        new GuiFormSection(LeanGuiFormConstants.SECTION_LAYOUT, "Layout options", false);
    for (String side : List.of("left", "right", "top", "bottom")) {
      GuiFormField field =
          new GuiFormField(side + "Layout", GuiFormFieldType.LAYOUT_SIDE, capitalize(side) + " alignment", side);
      field.setBinding("wrapper");
      field.setOrder("02000-" + side);
      section.getFields().add(field);
    }
    return section;
  }

  private GuiFormSection buildSectionFromFields(
      String id, String title, boolean open, List<GuiFormField> fields) {
    GuiFormSection section = new GuiFormSection(id, title, open);
    section.setFields(new ArrayList<>(fields));
    sortFields(section.getFields());
    return section;
  }

  private void sortFields(List<GuiFormField> fields) {
    fields.sort(
        Comparator.comparing(
                (GuiFormField f) -> StringUtils.defaultString(f.getOrder()),
                Comparator.naturalOrder())
            .thenComparing(f -> StringUtils.defaultString(f.getId())));
  }

  private static String capitalize(String s) {
    if (s == null || s.isEmpty()) {
      return s;
    }
    return Character.toUpperCase(s.charAt(0)) + s.substring(1);
  }

  /**
   * Build a form schema for a connector plugin id (e.g. {@code SqlConnector}).
   *
   * <p>No presentation wrapper/layout sections — connectors are edited as standalone metadata.
   */
  public GuiFormSchema buildConnectorSchema(String pluginId) throws LeanException {
    IPlugin plugin =
        PluginRegistry.getInstance().findPluginWithId(LeanConnectorPluginType.class, pluginId);
    if (plugin == null) {
      throw new LeanException("Connector plugin not found: " + pluginId);
    }

    Class<? extends ILeanConnector> connectorClass;
    try {
      String className = plugin.getClassMap().get(ILeanConnector.class);
      if (className == null) {
        throw new LeanException("No main class mapping for connector plugin " + pluginId);
      }
      ClassLoader classLoader = PluginRegistry.getInstance().getClassLoader(plugin);
      @SuppressWarnings("unchecked")
      Class<? extends ILeanConnector> loaded =
          (Class<? extends ILeanConnector>) classLoader.loadClass(className);
      connectorClass = loaded;
    } catch (LeanException e) {
      throw e;
    } catch (Exception e) {
      throw new LeanException("Unable to load class for connector plugin " + pluginId, e);
    }

    LeanConnectorPlugin annotation = connectorClass.getAnnotation(LeanConnectorPlugin.class);
    String name = annotation != null ? annotation.name() : plugin.getName();
    String description = annotation != null ? annotation.description() : plugin.getDescription();

    GuiFormSchema schema = new GuiFormSchema(pluginId, name);
    schema.setPluginDescription(description);
    schema.setPluginClassName(connectorClass.getName());

    Map<String, List<GuiFormField>> byParent = collectAnnotatedFields(connectorClass);
    List<GuiFormField> pluginFields = new ArrayList<>();
    // Base fields first (e.g. source connector), then plugin-specific, then any other parents
    pluginFields.addAll(
        byParent.getOrDefault(LeanGuiFormConstants.PARENT_BASE, List.of()));
    pluginFields.addAll(
        byParent.getOrDefault(LeanGuiFormConstants.PARENT_PLUGIN, List.of()));
    for (Map.Entry<String, List<GuiFormField>> entry : byParent.entrySet()) {
      String parentId = entry.getKey();
      if (LeanGuiFormConstants.PARENT_PLUGIN.equals(parentId)
          || LeanGuiFormConstants.PARENT_BASE.equals(parentId)) {
        continue;
      }
      pluginFields.addAll(entry.getValue());
    }

    sortFields(pluginFields);
    if (!pluginFields.isEmpty()) {
      schema.setHasPluginWidgets(true);
      GuiFormSection section =
          buildSectionFromFields(
              LeanGuiFormConstants.SECTION_PLUGIN, name, true, pluginFields);
      schema.getSections().add(section);
    }
    return schema;
  }

  public boolean canBuildConnectorSchema(String pluginId) {
    try {
      return PluginRegistry.getInstance()
              .findPluginWithId(LeanConnectorPluginType.class, pluginId)
          != null;
    } catch (Exception e) {
      return false;
    }
  }

  /** Convenience for tests: enum names used by label alignment combos. */
  public static List<String> horizontalAlignments() {
    return Arrays.stream(LeanHorizontalAlignment.values()).map(Enum::name).toList();
  }

  public static List<String> verticalAlignments() {
    return Arrays.stream(LeanVerticalAlignment.values()).map(Enum::name).toList();
  }
}
