# Lean Enterprise Analytics (lean-engine)

Server-side analytics presentation engine for enterprises that prefer their **data never leaves the server** to end up in browser JavaScript.

Lean acquires data and **renders reports and dashboards as SVG** (and PDF) on the server. Clients display the rendered output; they do not call databases or pull raw row sets into the browser.

## Platform

| Requirement | Version |
|-------------|---------|
| Java | **21** |
| Apache Hop | **2.18.1** |
| Build | Maven 3.8+ |
| Artifact | `org.lean:lean-engine:1.0.0-SNAPSHOT` |

## Quick start

```java
LeanEnvironment.init();

IHopMetadataProvider metadata = new MemoryMetadataProvider();
LeanPresentation presentation = LeanPresentation.fromJsonString(json);

LeanLayoutResults layout =
    presentation.doLayout(parentLog, new PresentationRenderContext(presentation, metadata),
        metadata, List.of());

presentation.render(layout, metadata);

// Each layout page has SVG via HopSvgGraphics2D / Batik
String svg = layout.getRenderPages().get(0).getSvgXml();
```

```bash
mvn clean install
```

## Architecture (summary)

1. **Presentation metadata** — pages, components, themes, connectors, interactions (`LeanPresentation`).
2. **Connectors** — server-side data sources (SQL, sample data, REST, sort/filter/chain, metadata, …).
3. **Layout** — size and position components (attachments, dynamic tables/charts).
4. **Render** — draw to SVG (Batik / Hop SVG); optional PDF via FOP/PDFBox.
5. **Plugins** — components and connectors discovered via Jandex + Hop `PluginRegistry`.

See the planned `docs/` set for components, connectors, and API details (in progress).

## Ecosystem

| Project | Role |
|---------|------|
| **lean-engine** (this repo) | Core library |
| **lean-rest** | REST API delivering SVG/HTML from the engine |
| **lean-hop-plugins** | Hop pipeline connector + pipeline/workflow components |
| **hop-lean-plugins** | Hop GUI AutoDoc using Lean |

## License

Apache License 2.0
