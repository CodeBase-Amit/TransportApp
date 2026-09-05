# TransportApp2 — Reverse-Engineered Architecture Report

**Scope:** complete technical documentation of the Android app in this repository.
**Audience:** an AI coding agent (or developer) who has never seen this codebase and must
continue development using only this report.
**Date of analysis:** September 2026, against the code state after Sprint S26 (ship build),
commits `550aaa3 → f96ca1a` on `main`.
**Test state at analysis time:** 233 tests, 0 failures, 53 suites, `checkPureModules` green.

---

## Index

| Part | File | Sections |
|---|---|---|
| 1 | `PART1_Overview_Structure.md` | 1. Project Overview · 2. Complete Project Structure |
| 2 | `PART2_Screens_Components.md` | 3. Every File Documentation · 4. Screen Documentation · 5. UI Components · 6. Buttons · 7. Cards |
| 3 | `PART3_Logic_Layers.md` | 8. ViewModels · 9. Repositories · 10. Database · 11. Network · 12. Domain · 13. Dependency Injection · 14. Navigation · 15. State Management |
| 4 | `PART4_Flows_Sync.md` | 16. App Flow · 17. Function Dependency Graph · 18. Data Flow · 19. Sync Architecture · 20. Feature Documentation |
| 5 | `PART5_Ops_Appendix.md` | 21. Globals · 22. Configuration · 23. Resources · 24. Security · 25. Performance · 26. Dependency Graph · 27. Sequence Diagrams · 28. Cross References · 29. AI Developer Guide · 30. Appendix |

**How to read this report:** Part 1 orients you. Part 2 is the UI surface. Part 3 is the
engine room. Part 4 is behaviour over time. Part 5 is everything operational. Cross-cutting
truths (money is paise `Long`, Room is the single truth, every write carries an outbox row)
are stated in Part 1 and referenced everywhere else by their D-numbers (decision log in
`AgentChanges.md`).

**Source of truth hierarchy (verified during analysis):**

| Document | Authority |
|---|---|
| `TransportApp2/Spec.md` | the operating manual: module rules, MVVM/UDF contract, commands, DoD |
| `Research/TransportApp/TransportApp.md` | the domain: state machines, §10 calculations, §16 sync schema, §17 roles |
| `Research/TransportApp/Design.md` + the S20 "Night Haul Expressive" addendum (D57–D59) | UI tokens and screen designs |
| `AgentChanges.md` | the decision log D1–D65 and the work history — **read the relevant D-numbers before touching those areas** |
| `Plans/TransportApp2-Future-Roadmap.md` | what remains, ticked per sprint |

**Known stale external documents** (do not trust their findings over the code):
`TransportApp/Project_Analysis_2.md` (many claims predate S15–S26 fixes; its "zero tests"
claim is wrong — 233 tests exist), `Analysis.md`, `Analysis_Report.md` (superseded by the
audit roadmap).

---

## Mermaid conventions used throughout

- `graph TD`/`LR` for dependency and structure graphs
- `sequenceDiagram` for runtime flows
- `classDiagram` sparingly where a class relationship is genuinely informative
- All arrows annotated with the mechanism (Flow collect, suspend call, Room query, HTTP)
