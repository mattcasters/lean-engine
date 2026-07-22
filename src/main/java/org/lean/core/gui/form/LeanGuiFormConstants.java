package org.lean.core.gui.form;

/** Parent ids and section ids for Lean component configuration forms. */
public final class LeanGuiFormConstants {

  /** Plugin-specific fields (label text, chart title, …). */
  public static final String PARENT_PLUGIN = "LeanComponent-Plugin";

  /** Shared fields from {@code LeanBaseComponent}. */
  public static final String PARENT_BASE = "LeanComponent-Base";

  /** Wrapper fields on {@code LeanComponent} (name, …). */
  public static final String PARENT_WRAPPER = "LeanComponent-Wrapper";

  /** Layout attachments on {@code LeanComponent.layout}. */
  public static final String PARENT_LAYOUT = "LeanComponent-Layout";

  /**
   * Wrapper transform/render properties on {@code LeanComponent} (rotation, transparency,
   * clip size).
   */
  public static final String PARENT_COMPONENT_PROPS = "LeanComponent-Props";

  public static final String SECTION_WRAPPER = "wrapper";
  public static final String SECTION_PLUGIN = "plugin";
  public static final String SECTION_BASE = "base";
  public static final String SECTION_COMPONENT_PROPS = "componentProps";
  public static final String SECTION_LAYOUT = "layout";

  private LeanGuiFormConstants() {}
}
