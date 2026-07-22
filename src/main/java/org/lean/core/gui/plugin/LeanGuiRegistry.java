package org.lean.core.gui.plugin;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.hop.core.plugins.IPlugin;
import org.apache.hop.core.plugins.PluginRegistry;
import org.lean.core.gui.form.LeanGuiFormConstants;
import org.lean.presentation.component.type.ILeanComponent;
import org.lean.presentation.component.type.LeanComponentPluginType;
import org.lean.presentation.connector.type.ILeanConnector;
import org.lean.presentation.connector.type.LeanConnectorPluginType;

/**
 * Singleton registry of {@link LeanWidgetElement} declarations on Lean component and connector
 * plugins.
 *
 * <p>Populated by {@link #scanFromPluginRegistry()} during {@code LeanEnvironment.init()}.
 */
public final class LeanGuiRegistry {

  private static LeanGuiRegistry instance;

  /**
   * className → parentId → widget list (sorted by order after {@link #sortAll()}).
   */
  private final Map<String, Map<String, List<LeanWidgetElements>>> dataElementsMap =
      new LinkedHashMap<>();

  private boolean scanned;

  private LeanGuiRegistry() {}

  public static synchronized LeanGuiRegistry getInstance() {
    if (instance == null) {
      instance = new LeanGuiRegistry();
    }
    return instance;
  }

  /**
   * Scan all registered Lean component and connector plugin classes for {@link LeanWidgetElement}
   * fields. Safe to call more than once; clears and rebuilds the map.
   */
  public synchronized void scanFromPluginRegistry() {
    clear();
    PluginRegistry pluginRegistry = PluginRegistry.getInstance();

    for (IPlugin plugin : pluginRegistry.getPlugins(LeanComponentPluginType.class)) {
      scanPluginClass(plugin, ILeanComponent.class, pluginRegistry);
    }
    for (IPlugin plugin : pluginRegistry.getPlugins(LeanConnectorPluginType.class)) {
      scanPluginClass(plugin, ILeanConnector.class, pluginRegistry);
    }

    sortAll();
    scanned = true;
  }

  private void scanPluginClass(
      IPlugin plugin, Class<?> mainType, PluginRegistry pluginRegistry) {
    try {
      String className = plugin.getClassMap().get(mainType);
      if (className == null) {
        return;
      }
      ClassLoader classLoader = pluginRegistry.getClassLoader(plugin);
      Class<?> clazz = classLoader.loadClass(className);
      registerClass(clazz);
    } catch (Exception e) {
      // Skip plugins that cannot be loaded for widget scan
    }
  }

  /**
   * Reflect {@link LeanWidgetElement} fields on {@code clazz} (and superclasses) into the
   * registry. Used by the global scan and as a fallback when a class was not scanned yet.
   */
  public synchronized void registerClass(Class<?> clazz) {
    if (clazz == null) {
      return;
    }
    String className = clazz.getName();
    for (Field field : getAllFields(clazz)) {
      LeanWidgetElement annotation = field.getAnnotation(LeanWidgetElement.class);
      if (annotation == null) {
        continue;
      }
      addWidgetElement(className, annotation, field, clazz);
    }
  }

  /**
   * Add one annotated field. Honors {@link LeanWidgetElement#ignored()} override semantics.
   */
  public synchronized void addWidgetElement(
      String dataClassName, LeanWidgetElement annotation, Field field, Class<?> ownerClass) {
    LeanWidgetElements child = new LeanWidgetElements(annotation, field, ownerClass);
    String parentId =
        StringUtils.isEmpty(child.getParentId())
            ? LeanGuiFormConstants.PARENT_PLUGIN
            : child.getParentId();
    child.setParentId(parentId);

    Map<String, List<LeanWidgetElements>> byParent =
        dataElementsMap.computeIfAbsent(dataClassName, k -> new LinkedHashMap<>());
    List<LeanWidgetElements> list = byParent.computeIfAbsent(parentId, k -> new ArrayList<>());

    LeanWidgetElements existing = findById(list, child.getId());
    if (existing != null && existing.isIgnored()) {
      return;
    }
    if (existing != null && child.isIgnored()) {
      existing.setIgnored(true);
      return;
    }
    if (existing != null) {
      list.remove(existing);
    }
    if (!child.isIgnored()) {
      list.add(child);
    }
  }

  private static LeanWidgetElements findById(List<LeanWidgetElements> list, String id) {
    for (LeanWidgetElements e : list) {
      if (id != null && id.equals(e.getId())) {
        return e;
      }
    }
    return null;
  }

  /**
   * Widgets for a class, grouped by parent id. If the class is unknown, reflects it once and
   * returns the result (lazy fallback for tests / late-loaded classes).
   */
  public synchronized Map<String, List<LeanWidgetElements>> getElementsByParent(Class<?> clazz) {
    if (clazz == null) {
      return Map.of();
    }
    Map<String, List<LeanWidgetElements>> existing = dataElementsMap.get(clazz.getName());
    if (existing == null) {
      registerClass(clazz);
      existing = dataElementsMap.get(clazz.getName());
      if (existing != null) {
        sortClass(clazz.getName());
      }
    }
    if (existing == null) {
      return Map.of();
    }
    // defensive copy
    Map<String, List<LeanWidgetElements>> copy = new LinkedHashMap<>();
    for (Map.Entry<String, List<LeanWidgetElements>> e : existing.entrySet()) {
      copy.put(e.getKey(), Collections.unmodifiableList(new ArrayList<>(e.getValue())));
    }
    return copy;
  }

  public synchronized List<LeanWidgetElements> findElements(String dataClassName, String parentId) {
    Map<String, List<LeanWidgetElements>> byParent = dataElementsMap.get(dataClassName);
    if (byParent == null) {
      return List.of();
    }
    List<LeanWidgetElements> list = byParent.get(parentId);
    if (list == null) {
      return List.of();
    }
    return Collections.unmodifiableList(new ArrayList<>(list));
  }

  public synchronized void sortAll() {
    for (String className : dataElementsMap.keySet()) {
      sortClass(className);
    }
  }

  private void sortClass(String className) {
    Map<String, List<LeanWidgetElements>> byParent = dataElementsMap.get(className);
    if (byParent == null) {
      return;
    }
    for (List<LeanWidgetElements> list : byParent.values()) {
      list.sort(Comparator.naturalOrder());
    }
  }

  public synchronized void clear() {
    dataElementsMap.clear();
    scanned = false;
  }

  public boolean isScanned() {
    return scanned;
  }

  public Map<String, Map<String, List<LeanWidgetElements>>> getDataElementsMap() {
    return dataElementsMap;
  }

  private static List<Field> getAllFields(Class<?> clazz) {
    List<Field> fields = new ArrayList<>();
    Class<?> current = clazz;
    while (current != null && current != Object.class) {
      for (Field field : current.getDeclaredFields()) {
        if (Modifier.isStatic(field.getModifiers())) {
          continue;
        }
        fields.add(field);
      }
      current = current.getSuperclass();
    }
    return fields;
  }
}
