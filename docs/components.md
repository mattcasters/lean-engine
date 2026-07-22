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

## Known limitations (backlog)

- Crosstab: subtotals, multi-level sort, configurable “Total” label
- SVG component: centered bounds with magnification
- DrawnItem rotation
