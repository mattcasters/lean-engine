# Lean Ecosystem

| Repository | Purpose | Platform target |
|------------|---------|-----------------|
| **lean-engine** | Core library (this project) | Java 21, Hop 2.18.1, `1.0.0-SNAPSHOT` |
| **lean-rest** | Jersey REST + HTML/SVG delivery | Java 21, Hop 2.18.1 — smoke: lean-rest `docs/smoke-test.md` |
| **lean-hop-plugins** | Pipeline connector + pipeline/workflow SVG components | Java 21, Hop 2.18.1, lean-engine 1.0 |
| **hop-lean-plugins** | Hop GUI AutoDoc (uses lean-hop-plugins) | Java 21, Hop 2.18.1, lean-engine 1.0 |
| **lean-viewer** | Older Jetty WAR viewer | Prefer lean-rest |
| **lean-swt-viewer** | SWT desktop preview | Optional |
| **lean-frontend** | Vaadin UI (legacy Hop 0.60) | Rewrite or archive |

Dependency direction:

```
lean-rest / hop-lean-plugins
            │
            ▼
       lean-hop-plugins (optional Hop-specific components)
            │
            ▼
       lean-engine
            │
            ▼
       hop-core / hop-engine (+ Batik, Jackson, …)
```

Publish snapshots of lean-engine first; then lean-hop-plugins; then hop-lean-plugins / lean-rest.
