# Generated form review samples

Representative **annotation-driven** editor forms and JSON schemas produced by
`GuiFormSchemaBuilder` / `GuiFormHtmlRenderer`.

Full gallery (all plugins) is regenerated on every test run:

```bash
cd lean-engine
mvn test -Dtest=FormEndToEndTest
# then open: target/form-review/README.md
```

## Sample set in this folder

### Components

| Plugin | Form | Schema |
|--------|------|--------|
| Label | [LeanLabelComponent.form.html](LeanLabelComponent.form.html) | [schema](LeanLabelComponent.schema.json) |
| Table | [LeanTableComponent.form.html](LeanTableComponent.form.html) | [schema](LeanTableComponent.schema.json) |
| Group (nested component) | [LeanGroupComponent.form.html](LeanGroupComponent.form.html) | [schema](LeanGroupComponent.schema.json) |
| Composite (child list) | [LeanCompositeComponent.form.html](LeanCompositeComponent.form.html) | [schema](LeanCompositeComponent.schema.json) |

Open the `.form.html` files in a browser for a static markup preview. Interactive
load/save needs lean-rest (jQuery helpers, metadata APIs).

### Connectors

| Plugin | Form | Schema |
|--------|------|--------|
| SQL | [SqlConnector.form.html](SqlConnector.form.html) | [schema](SqlConnector.schema.json) |
| Sample data | [SampleDataConnector.form.html](SampleDataConnector.form.html) | [schema](SampleDataConnector.schema.json) |
| Sort | [SortConnector.form.html](SortConnector.form.html) | [schema](SortConnector.schema.json) |
| Simple filter | [SimpleFilterConnector.form.html](SimpleFilterConnector.form.html) | [schema](SimpleFilterConnector.schema.json) |

## How to exercise in lean-rest

```bash
cd lean-engine && mvn install -DskipTests
cd ../lean-rest
export LEAN_REST_CONFIG_PATH="$PWD/src/test/resources"
mvn jetty:run -DLEAN_REST_CONFIG_PATH="$LEAN_REST_CONFIG_PATH"
```

1. Open a presentation render page (e.g. from the home list).
2. **Components**: Ctrl+click a component on the SVG.
3. **Connectors**: toolbar **add-item** icon → list/edit/create connectors.

## Tests covering this path

| Test | Asserts |
|------|---------|
| `PluginFormCoverageTest` | Every registered component/connector builds schema + HTML |
| `FormEndToEndTest` | Field ids in HTML; connector metadata save/load/rename round-trip; writes `target/form-review/` |
| `GuiFormSchemaBuilderTest` | Label, Table lists, charts, Group catalog, Composite children |

Static per-plugin HTML under lean-rest `plugins/component/` has been **removed**.
