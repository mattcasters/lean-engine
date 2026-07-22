package org.lean.core.gui.form;

/**
 * UI-agnostic field types for generated plugin configuration forms.
 *
 * <p>Maps from Lean {@code LeanWidgetType} plus Lean-specific composites used by the browser editor.
 */
public enum GuiFormFieldType {
  TEXT,
  MULTI_LINE_TEXT,
  PASSWORD,
  CHECKBOX,
  COMBO,
  FILENAME,
  FOLDER,
  METADATA,
  /** HTML color input bound to {@code LeanColorRGB} JSON. */
  COLOR,
  /** Composite font editor bound to {@code LeanFont} JSON. */
  FONT,
  /** One of left/right/top/bottom layout attachment groups. */
  LAYOUT_SIDE,
  /**
   * Width/height pair bound to {@code LeanSize} JSON ({@code {width, height}}), e.g. clip size.
   */
  SIZE,
  /**
   * Editable list of beans (e.g. {@code List<LeanColumn>}, {@code List<LeanFact>}). Item shape is
   * described by {@link GuiFormField#getItemKind()}.
   */
  LIST,
  /**
   * Single nested {@code LeanComponent} (name + layout + typed plugin payload). Uses the schema
   * {@link GuiFormSchema#getComponentCatalog()} for type-specific fields.
   */
  COMPONENT,
  BUTTON,
  LINK,
  UNKNOWN
}
