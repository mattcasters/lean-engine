package org.lean.core.gui.plugin;

/**
 * Declares where a COMBO / METADATA field gets its options at edit time (project / presentation
 * context).
 */
public enum LeanComboSource {
  /** No dynamic source; use static {@link LeanWidgetElement#comboValuesMethod()} or enum names. */
  NONE,

  /** Shared connector metadata names (+ presentation-local connectors when available). */
  CONNECTORS,

  /** Theme metadata names. */
  THEMES,

  /** Component names on the current page (for layout attachments). */
  COMPONENTS,

  /**
   * Output column names of a connector. Pair with {@link LeanWidgetElement#dependsOn()} naming the
   * field that holds the connector name (default {@code sourceConnectorName}).
   */
  CONNECTOR_COLUMNS,

  /**
   * Names of elements for a Hop metadata type. Pair with {@link LeanWidgetElement#metadataKey()}
   * (e.g. {@code lean-database-connection}).
   */
  METADATA
}
