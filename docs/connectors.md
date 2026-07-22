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
| `DistinctConnector` | `LeanDistinctConnector` | Drop rows equal to the previous row |
| `SelectionConnector` | `LeanSelectionConnector` | Project a subset of fields |
| `PassthroughConnector` | `LeanPassthroughConnector` | Pass all rows from source |
| `ChainConnector` | `LeanChainConnector` | Encapsulate a pipeline of connectors |
| Metadata connectors | `LeanMetadata*Connector` | List Hop/Lean metadata types and elements |

Filter support: `LeanSimpleFilterConnector` (field equals set of values). Check annotation/`@LeanConnectorPlugin` for discovery ID when chaining.

## Configuration tips

- **SQL**: store connection as Hop metadata key `lean-database-connection`; reference by name. Prefer parameters/variables for dynamic filters; avoid unsafe string concatenation of untrusted input.
- **Sort**: supply parallel lists of `LeanColumn` and `LeanSortMethod` (same size).
- **Selection**: column names must exist in the source `IRowMeta`.
- **Chain**: first step uses the chain’s `sourceConnectorName`; last step is exposed as `_RESULT_OF_CHAIN_`.

## Testing

Unit tests under `src/test/java/org/lean/presentation/connector/` use `ConnectorTestSupport` and `PresentationDataContext` with in-memory connectors. SQL tests use H2 via `hop-databases-h2` and `TablePresentationUtil`.
