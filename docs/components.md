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
| `LeanPieChartComponent` | Pie chart | Categories + values (optional donut, legend, %) |
| `LeanCrosstabComponent` | Crosstab | Horizontal/vertical dimensions + facts/aggregations |
| `LeanImageComponent` | Image | Raster image from path/URL (server-side load) |
| `LeanSvgComponent` | SVG | Embed/scale SVG artwork |
| `LeanCompositeComponent` | Composite | Nested child components |
| `LeanGroupComponent` | Group | Repeat a child layout per group key; nested connectors can be filtered by group keys (same-name match or explicit `keyMappings`) |

## Plugin icons (`@LeanComponentPlugin.image`)

Component type icons ship **with the plugin JAR** (lean-engine for built-ins).

```java
@LeanComponentPlugin(
    id = "LeanLabelComponent",
    name = "Label",
    description = "...",
    image = "ui/images/components/label.svg")
```

| Piece | Detail |
|--------|--------|
| Resources | `ui/images/components/*.svg` under lean-engine |
| Registration | `LeanComponentPluginType.extractImageFile` → `IPlugin.getImageFile()` |
| List API | `GET plugins/components` includes `"image"` |
| Image API | `GET plugins/components/{pluginId}/image` |
| Browser | Palette + page component list use the image API with name/description tooltips |

## Pie chart (`LeanPieChartComponent`)

- **Data:** one or more **horizontal dimensions** (slice categories; multi-dim labels join with `-`) and **exactly one fact** with aggregation (SUM / COUNT / AVERAGE as supported by the pivot). Vertical dimensions are ignored in v1.
- **Options:** title, margins, legend (`RIGHT` / `BOTTOM`), on-slice labels, percentages, fact values, **inner radius %** (0 = pie, e.g. 50 = donut), start angle (degrees, default −90 = top), clockwise.
- **Values:** null/missing → 0; **negative values are skipped**; total 0 draws an empty outline only.
- **Colors:** theme stable colors keyed by category label (`getStableColor`).
- **Interactions:** `DrawnItem`s for title, each slice (`ChartLabel`), and legend entries (`LegendEntry`).

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
