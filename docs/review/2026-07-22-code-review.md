# Code review notes — lean-engine (2026-07-22)

Review after Hop 2.18.1 / Java 21 port. Severity: **P0** must-fix, **P1** should-fix, **P2** backlog.

## Findings

### P1 — Connector streaming state

**Where:** Sort, filter, distinct, selection, passthrough, chain  
**Issue:** `finishedQueue != null` guards against double `startStreaming`, but listeners are never removed from the **source** connector after completion. Reusing a source connector instance for multiple transform runs can stack listeners.  
**Mitigation:** `PresentationDataContext` copies connectors; document that apps should not reuse raw connector instances across concurrent streams. Follow-up: remove source listeners in `waitUntilFinished`.

### P1 — Distinct is “previous row” distinct, not full-set distinct

**Where:** `LeanDistinctConnector`  
**Issue:** Compares only to the previous row (good for pre-sorted streams; surprising otherwise).  
**Mitigation:** Documented in tests/docs. Consider renaming or adding a full-hash distinct mode (P2).

### P1 — Simple filter AND of equality only

**Where:** `LeanSimpleFilterConnector`  
**Issue:** Multiple filter entries must all match; no OR/operators/comparators. Fine for now; document clearly.

### P1 — REST connector stack

**Where:** `LeanRestConnector`  
**Issue:** Uses Apache HttpClient + json-simple while Jersey client is also a dependency; mixed `jakarta` elsewhere.  
**Mitigation:** Consolidate on one HTTP + JSON stack in a later PR (Jackson already present).

### P2 — Large component classes

Crosstab (~1.2k LOC), table (~850 LOC), chart base (~600 LOC). Hard to unit-test edge cases without extraction. Prefer locking behavior with presentation tests before refactoring.

### P2 — Logging noise

`LeanPresentation.doLayout` logs every parameter at Basic. Prefer Debug for production embeds.

### P2 — `maxSurface` geometry semantics

`LeanGeometry.maxSurface` stores max right/bottom in width/height fields rather than true width/height. Call sites appear consistent; document or fix with tests if public.

### Fixed in this modernization pass

- Jandex version mismatch with Hop 2.18.1 (init failures)
- JSON `fullName` round-trip failure from `HopMetadataBase`
- Race-prone `LeanEnvironment.init` (now synchronized/idempotent)
- Removed dead `ITypeMetadata` adapters and engine `@Path` on DB connection
- SQL connector test restored on JUnit 5

## Test coverage status

| Area | Status |
|------|--------|
| Sample / list / sort / filter / distinct / passthrough / selection | Unit tests added |
| SQL (H2) | Unit test (JUnit 5) |
| Presentation SVG renders | Existing integration tests |
| REST / chain / metadata connectors | Still thin |
| Component unit tests (isolated) | Still thin (mostly presentation-level) |
| NPE JSON fixtures | Exercised via SteelWheels presentation suite |

## Recommended next fixes

1. Listener cleanup on transform connectors  
2. REST connector modernization + unit tests (WireMock)  
3. Chain connector unit test  
4. Assert SVG content (not only “did not throw”) in presentation tests  
5. Lombok pass on metadata beans  
