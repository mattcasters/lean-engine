# Lean Engine Architecture

## Purpose

**Lean Enterprise Analytics** renders presentations (reports and dashboards) **entirely on the server**:

1. **Data acquisition** runs server-side through pluggable **connectors**.
2. **Layout and drawing** run server-side into **SVG** (and optionally PDF).
3. Clients display the rendered result; they do not query databases or pull raw datasets into browser JavaScript.

This model suits enterprises that reject “data in the browser” architectures (JS calling web services or databases directly).

## High-level flow

```
LeanEnvironment.init()
        │
        ▼
LeanPresentation  (metadata: pages, components, connectors, themes, interactions)
        │
        ├─ doLayout(parentLog, renderContext, metadataProvider, parameters)
        │     for each page (topologically sorted components):
        │       processSourceData()  → connectors stream rows into component state
        │       doLayout()           → sizes, geometries, multi-page results
        │
        └─ render(layoutResults, metadataProvider)
              for each LeanRenderPage:
                draw background, header/footer, components onto SVGGraphics2D
                record DrawnItem hit regions for interactions
```

## Core concepts

| Concept | Type | Role |
|---------|------|------|
| **Presentation** | `LeanPresentation` | Top-level document (Hop `@HopMetadata`) |
| **Page** | `LeanPage` | Fixed canvas (e.g. A4) with margins and components |
| **Component** | `ILeanComponent` | Visual widget (table, chart, label, …) |
| **Connector** | `ILeanConnector` | Streaming data source or transform |
| **Theme** | `LeanTheme` | Colors and fonts |
| **Data context** | `IDataContext` | Variables + connector lookup + metadata provider |
| **Render context** | `IRenderContext` | Themes, stable colors, canvas size |
| **Layout results** | `LeanLayoutResults` | Pages, geometries, SVG GCs, drawn items |

## Plugin system

Lean reuses Apache Hop’s plugin registry and **Jandex** annotation indexes:

- `@LeanComponentPlugin` + `LeanComponentPluginType`
- `@LeanConnectorPlugin` + `LeanConnectorPluginType`
- Hop `@HopMetadata` types: presentation, connector, theme, lean-database-connection

`LeanEnvironment.init()`:

1. Initializes `HopClientEnvironment` (value metas, databases, VFS, …).
2. Registers Lean metadata/component/connector plugin types.
3. Scans the classpath for annotated plugins.

Call `init()` once per JVM (thread-safe and idempotent).

## Layout pipeline (per component)

1. **`processSourceData`** — optionally run connectors and cache rows/aggregates.
2. **`getExpectedSize`** — fixed size from metadata or dynamic size from data.
3. **`getExpectedGeometry`** — resolve attachments (`LeanAttachment`) relative to page or other components.
4. **`doLayout`** — place results on one or more `LeanRenderPage`s (tables/crosstabs may paginate).
5. **`render`** — paint using Batik `SVGGraphics2D` / Hop `HopSvgGraphics2D`.

Components on a page are ordered via a **topological sort** of attachment dependencies.

## Data streaming contract

Connectors implement `ILeanDataStreaming`:

- `describeOutput(IDataContext)` → `IRowMeta`
- `startStreaming(IDataContext)` → push rows to `ILeanRowListener`s
- end-of-stream signal: `rowReceived(null, null)` via `outputDone()`
- `waitUntilFinished()` for async sources

Transforms (sort, filter, distinct, selection, chain, passthrough) attach listeners to a **source connector name**, then start the source.

`LeanConnector.retrieveRows()` is a convenience that collects all rows for parameter mapping and tests.

## Interactions

After render, geometry of interactive regions is stored as `DrawnItem`s. A host application (e.g. lean-rest) maps pointer coordinates to items and evaluates `LeanInteraction` rules (parameters, navigation).

## Rendering outputs

| Output | Implementation |
|--------|----------------|
| SVG | Primary path; one document per render page |
| PDF | FOP / PDFBox helpers under `org.lean.render.pdf` |
| HTML | Host concern (lean-rest wraps SVG in pages) |

## Threading notes

- `LeanEnvironment` / Hop `PluginRegistry` initialization must be sequential.
- A connector instance should not be `startStreaming`’d twice without finishing (internal queues).
- `PresentationDataContext.getConnector()` returns a **copy** so concurrent queries stay isolated.

## Related modules

- **lean-rest** — HTTP metadata + render API
- **lean-hop-plugins** — pipeline connector and pipeline/workflow diagram components
- **hop-lean-plugins** — Hop GUI AutoDoc
