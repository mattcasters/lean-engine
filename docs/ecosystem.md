# Lean Ecosystem

| Repository | Purpose | Platform target |
|------------|---------|-----------------|
| **lean-engine** | Core library (this project) | Java 21, Hop 2.18.1 |
| **lean-rest** | Jersey REST + HTML/SVG delivery | Upgrade next |
| **lean-hop-plugins** | Pipeline connector + pipeline/workflow components | Upgrade next |
| **hop-lean-plugins** | Hop GUI AutoDoc | Upgrade next |
| **lean-viewer** | Older Jetty WAR viewer | Prefer lean-rest |
| **lean-swt-viewer** | SWT desktop preview | Optional |
| **lean-frontend** | Vaadin UI (legacy Hop 0.60) | Rewrite or archive |

Dependency direction:

```
lean-rest / hop plugins / viewers
            │
            ▼
       lean-engine
            │
            ▼
       hop-core (+ Batik, Jackson, …)
```

Publish snapshots of lean-engine first; then bump consumer modules.
