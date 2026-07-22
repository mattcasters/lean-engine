# Lean Ecosystem

| Repository | Purpose | Platform / status |
|------------|---------|-------------------|
| **lean-engine** | Core library | Java 21, Hop 2.18.1, published to Nexus `lean` as `1.0.0-SNAPSHOT` |
| **lean-rest** | REST API + HTML/SVG delivery | Java 21, Hop 2.18.1 — primary web path; [smoke test](https://github.com/mattcasters/lean-rest/blob/main/docs/smoke-test.md) |
| **lean-hop-plugins** | Pipeline connector + pipeline/workflow SVG components | Java 21, Hop 2.18.1; tests green |
| **hop-lean-plugins** | Hop GUI AutoDoc | Java 21, Hop 2.18.1; GUI features only |
| **lean-swt-viewer** | SWT desktop presentation viewer | Java 21, Hop 2.18.1; thin consumer of lean-engine |
| **lean-viewer** | Legacy Jetty viewer | **Deprecated** → use lean-rest |
| **lean-frontend** | Vaadin UI | **Archived** (Hop 0.60) |

## Nexus

Hosted repository: **https://repository.data-hopper.com/repository/lean/**  
Coordinates: `org.lean:*:1.0.0-SNAPSHOT`  
Credentials / deploy: [publishing.md](publishing.md)

## Dependency direction

```
lean-rest / hop-lean-plugins / lean-swt-viewer
            │
            ├── lean-hop-plugins (Hop-specific Lean plugins)
            │
            └── lean-engine
                    │
                    └── hop-core / hop-engine (+ Batik, Jackson, …)
```

Publish order: **lean-engine** → **lean-hop-plugins** → **hop-lean-plugins** / **lean-rest** / **lean-swt-viewer**.
