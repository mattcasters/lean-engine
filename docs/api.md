# Lean Engine Public API

Maven coordinates:

```xml
<dependency>
  <groupId>org.lean</groupId>
  <artifactId>lean-engine</artifactId>
  <version>1.0.0-SNAPSHOT</version>
</dependency>
```

**Java 21**, **Apache Hop 2.18.1**.

## Bootstrap

```java
LeanEnvironment.init();
```

Idempotent and synchronized. Required before plugin lookup, metadata serializers, and most connectors.

## Load a presentation

**From JSON:**

```java
LeanPresentation presentation = LeanPresentation.fromJsonString(json);
String jsonOut = presentation.toJsonString(true); // pretty
```

Uses `LeanJson.createMapper()` (ignores Hop runtime fields such as `fullName`).

**From Hop metadata:**

```java
IHopMetadataProvider provider = ...; // e.g. MemoryMetadataProvider or JSON folder provider
LeanPresentation p = provider.getSerializer(LeanPresentation.class).load("MyPresentation");
```

## Layout and render

```java
ILoggingObject parent = new LoggingObject("app");
IRenderContext renderContext = new PresentationRenderContext(presentation, provider);
List<LeanParameter> parameters = List.of(new LeanParameter("REGION", "EMEA"));

LeanLayoutResults results =
    presentation.doLayout(parent, renderContext, provider, parameters);

ILogChannel log = presentation.render(results, provider);

for (LeanRenderPage page : results.getRenderPages()) {
  String svgXml = page.getSvgXml(); // or page.getGc() for Batik API
}
```

### Parameters

- `LeanParameter` values are applied as variables on the presentation data context.
- `LeanParameterMapping` can load values from a connector field list before layout.

## Connectors programmatically

```java
LeanSampleDataConnector sample = new LeanSampleDataConnector(100);
LeanConnector connector = new LeanConnector("rows", sample);
presentation.getConnectors().add(connector);

// Or collect rows:
PresentationDataContext ctx = new PresentationDataContext(presentation, provider);
List<RowMetaAndData> rows = connector.retrieveRows(ctx);
```

## Themes

```java
LeanTheme theme = LeanTheme.getDefault();
presentation.getThemes().add(theme);
presentation.setDefaultThemeName(theme.getName());
```

Components fall back to the presentation default theme when `themeName` is unset.

## Database connections

```java
LeanDatabaseConnection db = new LeanDatabaseConnection(
    "steelwheels", "H2", "localhost", "0", "/path/to/db", "sa", "");
provider.getSerializer(LeanDatabaseConnection.class).save(db);

LeanSqlConnector sql = new LeanSqlConnector("steelwheels", "SELECT * FROM customers");
```

Database type codes match Hop (`H2`, `MYSQL`, `POSTGRESQL`, …). The corresponding Hop database plugin must be on the classpath (e.g. `hop-databases-h2`).

## Embedding vs Hop plugin

| Mode | Hop dependency | Notes |
|------|----------------|-------|
| Standalone library | compile (default) | Apps / lean-rest pull hop-core transitively |
| Inside Hop | provided profile (future) | Avoid duplicate hop-core on plugin classpath |

## Package overview

| Package | Contents |
|---------|----------|
| `org.lean.core` | Environment, geometry, colors, JSON, exceptions |
| `org.lean.presentation` | Presentation model, layout, pages, themes |
| `org.lean.presentation.component` | Components and plugin types |
| `org.lean.presentation.connector` | Connectors and plugin types |
| `org.lean.render` | Render contexts and PDF helpers |
