# TransportApp2 — UI Implementation Sprints

> **Goal.** Implement all 34 UI screens from `TransportApp/Design.md` (the Stitch "TransportApp UI"
> project) in Kotlin + Jetpack Compose, inside the `TransportApp2` project folder. UI only — screens
> driven by minimal ViewModels over the canonical sample dataset (`Design.md §B6`). No backend, Room,
> auth or business engines in this milestone.
>
> **Source of truth.** `TransportApp/Design.md` (screens, tokens, components) + the Stitch project's
> rendered screens as the visual reference. `TransportApp.md` owns the domain, roles, gates and budgets.
> **Visual consistency** is checked with the material-3 skill (see Sprint 7).
>
> **Decisions locked in chat:**
> - Full multi-module skeleton (15 Gradle modules, matching `TransportApp.md §5`).
> - Fonts via Google Fonts provider (Anek Latin, IBM Plex Sans/Mono) with Roboto fallback.
> - Minimal ViewModels with UiState + Events per screen (MVVM/UDF convention from the spec).

---

## Module map

| Module | Role |
|---|---|
| `:app` | MainActivity, AppNavHost wiring all feature graphs (Hilt deferred) |
| `:core:common` | pure Kotlin — Money(paise), Weight(grams), Result, ErrorCode |
| `:domain:transport` | pure Kotlin — PaymentMode, ConsignmentStatus, Role, TripState |
| `:core:designsystem` | theme (Color/Paper/Type/Shape/Dimens/Theme) + all components |
| `:core:ui` | shared composables + `sample/SampleData.kt` + `Routes.kt` |
| `:feature:auth` | T0, T32, T1, T2, T3, T33 |
| `:feature:dashboard` | T4 |
| `:feature:booking` | T5, T6 |
| `:feature:consignment` | T7, T8, T9 |
| `:feature:challan` | T10, T11, T12 |
| `:feature:billing` | T13, T14, T15, T16 |
| `:feature:masters` | T17, T18, T19, T20 |
| `:feature:reports` | T21, T22, T23 |
| `:feature:settings` | T24, T25, T26, T27, T28, T31 |
| `:feature:templates` | T29, T30 |

Deferred infra modules (no UI): `core:database`, `core:network`, `core:datastore`, `core:auth`,
`data:transport`, `pdf-android`, `file-android`, `sync-android`, `doc-engine`, `export-engine`.

Each feature uses the spec's package shape (§5.2): `navigation/`, `screen/` + `Screen`, `ViewModel`,
`UiState`, `Event`. ViewModels are plain `ViewModel` classes (Hilt deferred). Every screen registers
`@Preview`s for all its named states (loading/empty/error where §6 says they're reachable).

---

## Sprint 0 — Foundation & design system ✅ DONE

- Multi-module Gradle setup + version catalog updates; build green.
- Theme: Day Shift + Night Haul palettes as fixed light/dark schemes (never dynamic color); Paper
  palette as a separate non-theme object (two-palette rule enforced); 14-step type scale + 3 data*
  styles; radius ladder (2/4/12/16/20/28/pill); dimens (4dp grid, row heights 40/48/56/72/88, bars).
- Components: ContentCard/NestedCard, JourneyChip (11 fixed wordings), PaymentStamp (4dp, 2dp border,
  3° rotation), SyncChip, DocketRow (88dp), RouteLine (horizontal + vertical, reduced motion),
  TopAppBar, BottomNav, ExtendedFAB, StickyBar (72/88), buttons, fields, filter/segmented chips,
  states (LoadingBlock/Empty/Error), OfflineBar, ErrorBanner.
- Sample data (§B6) + Routes + placeholders for every screen.
- **Done:** `assembleDebug` green; app launches; all 15 modules compile.

## Sprint 1 — The booking loop (product hypothesis) ✅ DONE

Order per Design.md §D1: hardest screen first.

| Screen | Notes |
|---|---|
| T5 New booking form | Stamp landing, live total in sticky bar, party-search sheet, live charge recalculation |
| T6 Bilty preview | Paper palette, 4-copy carbon stack (the **one allowed shadow**), copy pager |
| T7 Consignment register | Docket rows, summary strip, two distinct empty states, FAB |
| T8 Consignment case file | Reusable docket header, vertical route-line event log, money, To Pay callout |

Verified on emulator (UI dump): T7 → FAB → T5 → Book and print → T6; T7 → docket → T8. Bug fixed:
bilty numbers contain `/` which broke navigation paths — all route args are `Uri.encode`d.

## Sprint 2 — First-run & onboarding

- **T0** Splash — 4-step route line (session/company/templates/sync), forced-update + resolve-failed frames
- **T32** First-run carousel — 3 panels + skipped frame (flat 2-colour illustrations)
- **T1** Sign in — 3 reassurances, Google button, loading/error states
- **T2** Company & branch picker — selected card with nested branch chooser, invitations, empty state
- **T3** Company setup wizard — 4 steps + done, live letterhead preview
- **T33** Your profile — finger signature pad, role chip, switches

## Sprint 3 — Operations: challan & vehicles

- **T9** Status update sheet — event-as-chip (not dropdown), Hold path with reason
- **T10** Challan builder — load meter, multi-select docket rows, vehicle/driver/hire, overload state
- **T11** Challan detail & preview — before/after dispatch frames
- **T12** Vehicle & trip board — per-vehicle route lines, idle card with no road

## Sprint 4 — Money

- **T13** Unbilled pool — ageing bars per party, live selection total in sticky bar
- **T14** Freight bill builder — draft / preview / issued frames, UNPAID stamp
- **T15** Payments & receipts — tabs, collect sheet, allocation sheet with running remainder
- **T16** Party statement — pinned opening/closing, frozen date column, ledger

## Sprint 5 — Dashboard & reporting

- **T4** Dashboard — exception strip, 10 tiles + sparkline, role-gated tiles, bottom nav + FAB
- **T17–T20** Masters — counts hub, duplicate merge, generic editor, rate matrix (resolution order)
- **T21** Reports hub — 4 groups + deep-link lock frame
- **T22** Report viewer — frozen first column, pinned totals row
- **T23** Export centre — 12-sheet route-line progress, recent exports, build/completion sheets

## Sprint 6 — Settings & admin ✅ DONE

- **T24** Settings hub — identity card, 4 groups, sync chip, owner/clerk views
- **T25** Company profile — live letterhead preview (paper colours)
- **T26** Branches — per-branch series pills, empty-branch invite
- **T27** Members & roles — active/invited tabs, role chips, self-row lock
- **T28** Numbering series — next-number preview bands, never-used warning
- **T29** Templates — paper thumbnails, default marker, version pills
- **T30** Template requests — 5-tick route line, quoted banner, past requests
- **T31** Account & data — sync queue with chips, sign out, leave vs delete (visibly different)

## Sprint 7 — Material-3 audit & consistency pass ✅ DONE

- Ran the **material-3 compliance audit** across all screens (color tokens, typography, shape,
  elevation, components, layout, navigation, accessibility, theming).
- **Color audit clean:** the only hardcoded `Color(0x…)` outside the design system is the single
  allowed shadow on T6's four-copy stack. The only `.shadow()` in the app is that same stack.
- **Fixed 4 emoji/unicode-glyph violations** (design §A13 forbids emoji): the "✓" done glyph,
  "🛡" role chip, "✕" close, and "⚠" overload warning were all replaced with proper Material icons.
- Hard rules held: paper palette separate from chrome, 17 fixed chip wordings, `#F7DFA6`/`#A32A1F`/
  `#C3DDCF` only with declared meanings, figures in Plex Mono, stamp rotated 3°, route line only on
  the declared surfaces.
- Final `assembleDebug` green; app installs and launches; screenshots saved for review.

> All 34 screens from Design.md are now implemented (T0–T33) with minimal ViewModels + UiState,
> light/dark theme, navigation between them, and a single `:app` NavHost. Remaining future work:
> bundling the real Anek/Plex font files (currently Google Fonts provider + Roboto fallback),
> and the infra modules (data/database/network/sync/pdf/engines) for the full app.

---

## Verification approach

- Build per sprint: `gradlew :app:assembleDebug` (must stay green).
- Compose previews for each screen/state.
- Emulator run + `uiautomator dump` / screenshots to confirm rendering and navigation.
- Final MD3 audit + visual diff against Stitch.

## Screen → sprint index (34 screens)

| Sprint | Screens |
|---|---|
| 1 | T5, T6, T7, T8 |
| 2 | T0, T32, T1, T2, T3, T33 |
| 3 | T9, T10, T11, T12 |
| 4 | T13, T14, T15, T16 |
| 5 | T4, T17, T18, T19, T20, T21, T22, T23 |
| 6 | T24, T25, T26, T27, T28, T29, T30, T31 |
| 7 | Audit + consistency across all 34 |

> Note: T33 (Your profile) is in Design.md but was not generated in Stitch — it is built from the spec.
> Infra modules are deferred; say the word if they should be added as empty stubs.
