# Lean Connectors

Connectors supply or transform row streams for components. All processing is **server-side**.

## Lifecycle

```
describeOutput(ctx) → IRowMeta
addRowListener(listener)
startStreaming(ctx)  → listener.rowReceived(meta, data)* → listener.rowReceived(null, null)
waitUntilFinished()
```

Source connectors produce data. Transform connectors set `sourceConnectorName` and wrap another connector from the data context.

## Built-in connectors

| Plugin ID | Class | Role |
|-----------|-------|------|
| `SampleDataConnector` | `LeanSampleDataConnector` | Deterministic sample rows (`id`, `name`, `updated`, `important`, `random`, `color`, `country`) |
| `LeanListConnector` | `LeanListConnector` | In-memory list of strings (one column) |
| `SqlConnector` | `LeanSqlConnector` | SQL against a `LeanDatabaseConnection` in metadata |
| `LeanRestConnector` | `LeanRestConnector` | HTTP JSON → rows |
| `SortConnector` | `LeanSortConnector` | Sort by columns / `LeanSortMethod` |
| `DistinctConnector` | `LeanDistinctConnector` | **Adjacent** distinct: drops rows equal to the previous row only (sort first for full uniqueness) |
| `SelectionConnector` | `LeanSelectionConnector` | Project a subset of fields |
| `PassthroughConnector` | `LeanPassthroughConnector` | Pass all rows from source |
| `ChainConnector` | `LeanChainConnector` | Encapsulate a pipeline of connectors |
| Metadata connectors | `LeanMetadata*Connector` | List Hop/Lean metadata types and elements |

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
- **Chain**: first step uses the chain’s `sourceConnectorName`; last step is exposed as `_RESULT_OF_CHAIN_`.

## Testing

Unit tests under `src/test/java/org/lean/presentation/connector/` use `ConnectorTestSupport` and `PresentationDataContext` with in-memory connectors. SQL tests use H2 via `hop-databases-h2` and `TablePresentationUtil`.
