# Lean Components

Components are visual building blocks on a `LeanPage`. Each implements `ILeanComponent` and is registered with `@LeanComponentPlugin`.

## Layout properties (common)

Inherited via `LeanBaseComponent` / `LeanComponent` wrapper:

- **Name** — unique on the page (attachment target)
- **Size** — optional fixed width/height; otherwise dynamic
- **Layout / attachments** — `LeanLayout` with left/right/top/bottom `LeanAttachment`s (percentage, offset, relative component)
- **Theme** — optional override of presentation default theme
- **Source connector** — name of a presentation connector for data-driven components
- **Background / border / fonts / colors**

## Built-in components

| Plugin ID | Class | Description |
|-----------|-------|-------------|
| `LeanLabelComponent` | Label | Static or variable-substituted text |
| `LeanTableComponent` | Table | Tabular layout of connector rows; can paginate |
| `LeanBarChartComponent` | Bar chart | Categories + values (+ optional series) |
| `LeanLineChartComponent` | Line chart | Categories + values (+ optional series) |
| `LeanCrosstabComponent` | Crosstab | Horizontal/vertical dimensions + facts/aggregations |
| `LeanImageComponent` | Image | Raster image from path/URL (server-side load) |
| `LeanSvgComponent` | SVG | Embed/scale SVG artwork |
| `LeanCompositeComponent` | Composite | Nested child components |
| `LeanGroupComponent` | Group | Repeat a child layout per group key |

## Data-driven components

Typical pattern:

1. Resolve connector from `IDataContext`.
2. In `processSourceData`, stream or retrieve rows and compute aggregates/geometry hints.
3. In `doLayout`, allocate one or more `LeanComponentLayoutResult` entries (possibly multi-page).
4. In `render`, draw using the page’s `SVGGraphics2D` and register `DrawnItem`s if interactive.

## Attachment model

Attachments anchor a side of a component to:

- the **page** (null component name), or
- another **component** by name,

with optional **percentage** of the reference size and **pixel offset**.

Circular attachment graphs are invalid; the page sorts components topologically before layout.

## Browser configuration forms (`@LeanWidgetElement`)

Plugin classes declare editor widgets with Lean’s `@LeanWidgetElement` next to `@HopMetadataProperty`.
Example: `LeanLabelComponent`. Annotations are scanned into `LeanGuiRegistry` during
`LeanEnvironment.init()`; `GuiFormSchemaBuilder` exports a UI-agnostic schema.

```java
@LeanWidgetElement(
    order = "10000-label",
    parentId = LeanGuiFormConstants.PARENT_PLUGIN,
    type = LeanWidgetType.TEXT,
    label = "Label text",
    tabName = "",           // optional: group fields onto separate editor tabs
    tabTooltip = "")
@HopMetadataProperty
private String label;
```

Shared parent ids live in `org.lean.core.gui.form.LeanGuiFormConstants`:

| Parent id | Section |
|-----------|---------|
| `LeanComponent-Plugin` | Plugin-specific fields |
| `LeanComponent-Base` | Fields on `LeanBaseComponent` |
| `LeanComponent-Wrapper` / `LeanComponent-Layout` | Shared chrome (generated) |

Optional `tabName` / `tabTooltip` on the annotation (and on `GuiFormField`) let clients group
widgets onto separate tabs within a section.

### Dynamic combo sources (`LeanComboSource`)

| Source | Options filled from |
|--------|---------------------|
| `CONNECTORS` | Presentation-local connectors + shared `connector` metadata |
| `THEMES` | `theme` metadata names |
| `COMPONENTS` | Component names on the current render page (layout attachments) |
| `CONNECTOR_COLUMNS` | Output fields of the connector named by `dependsOn` (default `sourceConnectorName`) |
| `METADATA` | Element names for `metadataKey` (e.g. `lean-database-connection`) |

lean-rest binds these via `bindSelectSource()` and refreshes column options when the source
connector combo changes.

`GuiFormHtmlRenderer` emits lean-rest side-panel HTML. See lean-rest `EditPluginResource`
(`edit/component/{id}/`, `edit/schema/component/{id}/`).

Static per-plugin HTML under lean-rest has been removed; forms are **always** generated.

After `mvn test -Dtest=FormEndToEndTest`, open `target/form-review/README.md` for a local
gallery of generated schemas and form HTML previews.

## Nested component editors

`LeanGroupComponent.groupComponent` (`COMPONENT`) and `LeanCompositeComponent.children`
(`LIST` + `itemKind=component`) use a **component catalog** embedded in the form schema
(`GuiFormSchema.componentCatalog`). The lean-rest side panel builds nested name/type/layout/plugin
fields recursively via `setNestedComponent` / `setNestedComponentList` in `lean-rest.js`.

## Known limitations (backlog)

- Crosstab: subtotals, multi-level sort, configurable “Total” label
- SVG component: centered bounds with magnification
- DrawnItem rotation
- `List<LeanSortMethod>` and arbitrary bean lists (column/fact/string/component kinds today)
- Connector edit UI in lean-rest shell (schema API exists; page chrome is presentation-focused)
- Nested layout “relative to” dropdown only lists top-level page component names
