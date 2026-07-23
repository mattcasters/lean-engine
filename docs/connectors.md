# Lean Connectors

Connectors supply or transform row streams for components. All processing is **server-side**.

## Lifecycle

```
describeOutput(ctx) → IRowMeta
addRowListener(listener)
startStreaming(ctx)  → listener.rowReceived(meta, data)* → listener.rowReceived(null, null)
waitUntilFinished()
```

## Connector roles

| Role | Meaning | Typical plugins |
|------|---------|-----------------|
| **Source** | Produces rows without requiring an upstream connector. `sourceConnectorName` is ignored or unused at runtime. | `SampleDataConnector`, `SqlConnector`, `LeanListConnector`, `LeanRestConnector`, `Metadata*Connector` |
| **Transform** | Reads from another connector named by `sourceConnectorName` (resolved via `IDataContext`). | `SortConnector`, `SelectionConnector`, `SimpleFilterConnector`, `DistinctConnector`, `PassthroughConnector` |
| **Chain** | Ordered pipeline of nested `ILeanConnector` instances. Outer chain has optional external source; nested steps are wired through `ChainDataContext`. | `ChainConnector` |

`LeanBaseConnector` always exposes `sourceConnectorName` in the form (combo of connector names) so sources and transforms share one base schema. UI should treat empty source as normal for sources; transforms and chains that need upstream should surface clear errors when it is missing or unresolved.

### How transforms get input

```
transform.sourceConnectorName
  → IDataContext.getConnector(name)   // presentation-local, then shared metadata
  → LeanConnector (copy)
  → attachToSource(source, listener)
  → source.startStreaming(dataContext)
```

### Chain wiring

`LeanChainConnector` builds a `ChainDataContext`:

1. Nested step `i` is registered as `__ChainConnector_i` (last step: `_RESULT_OF_CHAIN_`).
2. **First nested step** receives the chain’s outer `sourceConnectorName` (external upstream), when set.
3. **Later steps** receive the previous synthetic name as their source.
4. Streaming starts at the **last** nested connector; data flows through the chain via source links.

**Decision (connector studio Phase 0):** support **both** modes:

| Mode | Outer `sourceConnectorName` | First nested step |
|------|-----------------------------|-------------------|
| External source + transforms | Required (e.g. `Sample Data`) | Transforms (Filter, Select, Sort, …) |
| Embedded source pipeline | Optional / empty | A **source** plugin (Sample, SQL, REST, List, …), then transforms |

Today’s engine validates that the external source exists before streaming. Phase 4/5 may relax that when the first nested step is a source. Until then, studio docs and UI should prefer the external-source pattern (matches unit tests) and treat embedded-source chains as a planned engine enhancement.

## Built-in connectors

| Plugin ID | Class | Role |
|-----------|-------|------|
| `SampleDataConnector` | `LeanSampleDataConnector` | Source — deterministic sample rows (`id`, `name`, `updated`, `important`, `random`, `color`, `country`) |
| `LeanListConnector` | `LeanListConnector` | Source — in-memory list of strings (one column) |
| `SqlConnector` | `LeanSqlConnector` | Source — SQL against a `LeanDatabaseConnection` in metadata |
| `LeanRestConnector` | `LeanRestConnector` | Source — HTTP JSON → rows |
| `SortConnector` | `LeanSortConnector` | Transform — sort by columns / `LeanSortMethod` |
| `DistinctConnector` | `LeanDistinctConnector` | Transform — **adjacent** distinct only (sort first for full uniqueness) |
| `SelectionConnector` | `LeanSelectionConnector` | Transform — project a subset of fields |
| `SimpleFilterConnector` | `LeanSimpleFilterConnector` | Transform — exact string equality filters |
| `PassthroughConnector` | `LeanPassthroughConnector` | Transform — pass all rows from source |
| `ChainConnector` | `LeanChainConnector` | Chain — encapsulate a pipeline of connectors |
| Metadata connectors | `LeanMetadata*Connector` | Source — list Hop/Lean metadata types and elements |

### Simple filter

`LeanSimpleFilterConnector` keeps rows by **exact string equality**:

- Multiple filter entries on **different fields** are combined with **AND** (all must match).
- Multiple allowed values for the **same field** act as **OR** within that field.

There are no operators such as “greater than” or regex yet.

### Transform listener cleanup

Sort, filter, distinct, selection, passthrough, and chain attach a row listener to their source (or last chain step). After streaming finishes, `waitUntilFinished()` **detaches** that listener so reused source instances do not accumulate listeners.

### REST

`LeanRestConnector` uses the **JDK `HttpClient`** and **Jackson**. Empty `body` → GET; non-empty `body` → POST with JSON. Map fields with JSON tags to Hop types (`String`, `Integer`, `Number`, `Boolean`). Treat user-controlled URLs as an SSRF risk.

## Configuration tips

- **SQL**: store connection as Hop metadata key `lean-database-connection`; reference by name. Prefer parameters/variables for dynamic filters; avoid unsafe string concatenation of untrusted input.
- **Sort**: supply parallel lists of `LeanColumn` and `LeanSortMethod` (same size).
- **Selection**: column names must exist in the source `IRowMeta`.
- **Chain (current)**: first nested step uses the chain’s outer `sourceConnectorName`; last step is exposed as `_RESULT_OF_CHAIN_`.

---

## Connector studio preview (design contract)

Lean-rest will host a **connector data studio** (side panel): sample input rows (if any) at the top, settings in the middle, sample output rows and errors at the bottom. **Row layout** (name/type/length/precision) is **not** always visible — each pane has a **Show layout details** button.

This section is the Phase 0 contract for engine helpers and lean-rest APIs (implementation in later phases).

### Sampling semantics

| Rule | Detail |
|------|--------|
| Default sample size | **20** rows (client may send `maxRows`; UI range ~10–25) |
| Hard server max | **100** rows returned to the client |
| Limited collect | Prefer a row listener that keeps the first `maxRows` **output** rows, then stops collecting (still may need full stream for Sort / Distinct / some SQL) |
| Buffering transforms | Sort, Distinct, and similar may process the full source before emitting; **always** truncate the payload returned over REST to `maxRows` |
| Do not rewrite SQL | Never inject `LIMIT` into user SQL blindly; cap after streaming |
| Cell serialization | Display strings via `IValueMeta.getString(row)`; types live only in `rowMeta` |
| Timeouts | Reuse connector timeouts (REST already has connect/request timeouts). Preview endpoint should bound wall-clock time and return a structured error on exceed (lean-rest responsibility) |

Engine entry point: {@code org.lean.presentation.connector.preview.ConnectorPreviewSupport}, given
`IDataContext`, a `LeanConnector` (possibly unsaved), and `maxRows`, returns structured
input/output samples without throwing for expected failures.

```
preview(ctx, connector, maxRows)
  → input:  { connectorName?, rowMeta?, rows?, truncated? }   // only if sourceConnectorName set
  → output: { rowMeta?, rows?, truncated?, rowCountReturned? }
  → error:  { summary, detail } | null
```

- **Input path:** resolve `sourceConnectorName` from context → `describeOutput` + limited sample of the **source**.
- **Output path:** `describeOutput` on the target connector → limited sample of the **target** (via `retrieveRows`-style streaming with a collecting listener).
- Existing `LeanConnector.retrieveRows` still pulls **all** rows; preview must not call it without a max-rows guard.

### Unsaved preview model

Describe/sample must work from **form state**, not only saved metadata:

| Field | Purpose |
|-------|---------|
| `leanConnectorJson` | Full Hop-format connector JSON (`name`, `connector.{PluginId}: {…}`), same shape as save |
| `maxRows` | Optional; default 20; clamped to server max |
| `renderId` | Optional; when present, presentation-local connectors + layout data context participate |
| (optional later) | Parameter values for variable-driven SQL/REST |

Data context assembly:

1. If `renderId` is set → use that presentation’s connectors + metadata provider (same idea as `POST render/connector/describe/`).
2. Else → `PresentationDataContext` over a minimal presentation + metadata provider so **shared** connectors resolve.
3. The connector under edit is the **inline** instance from `leanConnectorJson` (unsaved field values), not a re-load by name (unless the client only sends a name for convenience later).

Today’s `POST render/connector/describe/` only describes a **named** connector from presentation/metadata. Preview will accept **inline** JSON; describe may later be extended the same way.

### Error model

Preview APIs return **HTTP 200** with a body that always includes success/failure flags (same spirit as component diagnostics). Transport failures remain 4xx/5xx.

```json
{
  "ok": true,
  "maxRows": 20,
  "input": {
    "connectorName": "Sample Data",
    "rowMeta": [ { "name": "id", "type": "Integer", "length": -1, "precision": -1 } ],
    "rows": [ ["1", "Name 1", "..."] ],
    "truncated": true
  },
  "output": {
    "rowMeta": [ ... ],
    "rows": [ ... ],
    "truncated": false,
    "rowCountReturned": 8
  },
  "error": null
}
```

On failure (missing source, bad SQL, REST error, timeout):

```json
{
  "ok": false,
  "maxRows": 20,
  "input": null,
  "output": null,
  "error": {
    "summary": "Unable to find connector source '…'",
    "detail": "… full cause chain …"
  }
}
```

Partial success is allowed: e.g. input sample OK, output fails → populate `input`, set `ok: false`, fill `error` from the output failure. Prefer still returning any successful `rowMeta` so **Show layout details** works when only sampling fails.

### Planned lean-rest endpoints

| Method | Path (under `/lean/api`) | Status |
|--------|--------------------------|--------|
| `POST` | `edit/connector/preview/` | **Implemented** — full studio preview from `leanConnectorJson` |
| `POST` | `render/connector/describe/` | **Exists** — named connector output schema only |
| `POST` | `metadata/modify/connector/` | **Exists** — persist connector |
| `GET` | `edit/connector/{pluginId}/` | **Exists** — generated settings form HTML |

**UI action split (studio — implemented in lean-rest):**

| Button | Behavior |
|--------|----------|
| **Apply** | Build JSON from form → `POST edit/connector/preview/` → refresh sample tables (no metadata write) |
| **Save** | Persist via `metadata/modify/connector/` (+ soft-reload presentation when in editor) |
| **Show layout details** | Client-side toggle of `rowMeta` table for input or output (default hidden) |

Generated connector HTML uses `applyConnectorPreview()` / `saveConnector()`; lean-rest wraps the form in the studio shell (input / settings / output).

### Annotation / form gaps (remaining after Phase 1)

| Location | Status |
|----------|--------|
| `LeanRestConnector.fields` / `JsonField` | **Done** — list + typed row editor (`itemKind=jsonField`) |
| `SimpleFilterValue.fieldName` | **Done** — filter rows use source-connector column select |
| `ChainConnector` nested list | **Done** — ordered step editor (type select, settings, reorder/delete) |
| Fallback plugin list in lean-rest JS | **Done** — includes Chain, Distinct, Passthrough |

---

## Plugin icons (`@LeanConnectorPlugin.image`)

Connector type icons ship **with the plugin JAR** (lean-engine for built-ins, or an external plugin).

```java
@LeanConnectorPlugin(
    id = "SqlConnector",
    name = "Execute a SQL query",
    description = "...",
    image = "ui/images/connectors/sql.svg")  // classpath resource in the same JAR
```

| Piece | Detail |
|--------|--------|
| Resource path | Classpath path, e.g. `ui/images/connectors/sql.svg` under `src/main/resources/` |
| Registration | `LeanConnectorPluginType.extractImageFile` → Hop `IPlugin.getImageFile()` |
| List API | `GET plugins/connectors` includes `"image": "<classpath path>"` |
| Image API | `GET plugins/connectors/{pluginId}/image` streams SVG (fallback: `ui/images/connectors/default.svg`) |
| Browser | lean-rest uses the image API for the connector list and chain step icons (no hardcoded map) |

Third-party plugins only need the annotation + resource on their own classpath; no lean-rest change for a new icon.

## Browser configuration forms (`@LeanWidgetElement`)

Connector fields use the same contract as components: `@LeanWidgetElement` next to
`@HopMetadataProperty`. `LeanGuiRegistry` scans connectors at `LeanEnvironment.init()`.

| Plugin ID | Form notes |
|-----------|------------|
| `SqlConnector` | database connection name, SQL |
| `SampleDataConnector` | row count |
| `SortConnector` | columns list + sort methods list |
| `SelectionConnector` | selected columns |
| `LeanListConnector` | column name + string values |
| `LeanRestConnector` | URL, path, body, rows path, **output fields** (`jsonField` list: tag, name, type, …) |
| `SimpleFilterConnector` | filter field/value pairs |
| `DistinctConnector` / `PassthroughConnector` | base source connector only |
| `MetadataElementsConnector` | metadata type key |
| `MetadataTypesConnector` / `MetadataPresentationsConnector` | no plugin-specific fields |
| `ChainConnector` | nested step list (type, expand settings; source wired at runtime) |

Schema and metadata APIs (lean-rest):

| Endpoint | Purpose |
|----------|---------|
| `GET edit/schema/connector/{pluginId}/` | JSON form schema |
| `GET edit/connector/{pluginId}/` | Generated HTML editor |
| `POST metadata/modify/connector/` | Save connector metadata (`oldConnectorName`, `leanConnectorJson`) |
| `GET metadata/list/connector/` | Connector names |
| `GET metadata/connector-json/{name}` | Load Hop-format connector JSON for the form editor |
| `GET plugins/connectors` | Available connector plugin types (`LeanPluginInfoRest` in lean-engine) |
| `POST render/connector/describe/` | Output **schema** for a named connector |
| `POST edit/connector/preview/` | Input/output samples + schema for inline JSON (`ConnectorPreviewSupport`) |

In the presentation **edit** shell, the toolbar **`connector.svg`** icon opens the connector list/editor side panel (not only the add-item control).

## Testing

Unit tests under `src/test/java/org/lean/presentation/connector/` use `ConnectorTestSupport` and `PresentationDataContext` with in-memory connectors. SQL tests use H2 via `hop-databases-h2` and `TablePresentationUtil`.

`PluginFormCoverageTest` asserts every registered connector id builds a form schema.

Preview helper tests (planned) should cover: sample source, transform with source, missing source error, `maxRows` truncation, and chain with external sample source.