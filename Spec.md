# TransportApp2 — Spec.md

**What this file is.** The operating manual for every agent (or human) writing code in this repository: the
architecture contract, naming rules, the exact MVVM/UDF shapes, data-layer contracts, commands, and the
definition of done. Read this before touching any file. It is deliberately practical — shapes and rules, not prose.

**Source-of-truth map.**

| File | Owns |
|---|---|
| `..\TransportApp\TransportApp.md` | The domain: vocabulary (§2), screen inventory (§6), state machines (§7.1, §11.1), calculation (§10), numbering (§9), client schema (§16.2), offline rules (§17.2–17.3), roles (§17.4.1), budgets (§17.5), error codes (§18.3) |
| `..\TransportApp\Design.md` | The UI: tokens, chips/stamps, route line, all 34 screen prompts |
| `..\TransportApp\Phase2.md` | The current build plan: sprints, Room storage plan, data-flow diagrams |
| `Spec.md` (this file) | How to work in this repo: contracts, conventions, commands, DoD |

---

## Contents

- [§1 Project state](#1-project-state)
- [§2 Module rules](#2-module-rules)
- [§3 The MVVM/UDF contract](#3-the-mvvmudf-contract)
- [§4 Naming conventions](#4-naming-conventions)
- [§5 The no-data-in-screens rule](#5-the-no-data-in-screens-rule)
- [§6 Data-layer contracts](#6-data-layer-contracts)
- [§7 Hilt conventions](#7-hilt-conventions)
- [§8 Routes and navigation](#8-routes-and-navigation)
- [§9 Error handling](#9-error-handling)
- [§10 How to add a feature — the recipe](#10-how-to-add-a-feature--the-recipe)
- [§11 Commands](#11-commands)
- [§12 Testing](#12-testing)
- [§13 Definition of Done](#13-definition-of-done)
- [§14 Forbidden patterns](#14-forbidden-patterns)

---

## §1 Project state

Native Android, Kotlin 2.2.10, Jetpack Compose (BOM 2026.02.01), Material 3, AGP 9.3.2, version catalog in
`gradle/libs.versions.toml`, applicationId `com.example.transportapp`. Phase 1 delivered all 34 screens (T0–T33)
as UI-only MVVM with per-screen sample data. Phase 2 (see `..\TransportApp\Phase2.md`) adds the offline data layer:
Hilt, Room, repositories, outbox scaffold, seeded demo dataset. The dev "Screen map" (`Routes.SCREEN_INDEX`) is the
start destination and reaches every screen in one tap.

## §2 Module rules

| Module | May depend on | Must never |
|---|---|---|
| `:app` | all feature modules, `:core:*`, `:data:transport`, `:sync-android` | contain screens or business logic |
| `:feature:*` | `:core:designsystem`, `:core:ui`, `:core:common`, `:domain:transport`, `:data:transport` | import `androidx.room`, `core.ui.sample`, or another feature module |
| `:data:transport` | `:core:database`, `:core:datastore`, `:core:common`, `:domain:transport` | import Compose or `androidx.activity` |
| `:core:database` / `:core:datastore` | `:core:common` | import `:domain:transport` |
| `:domain:transport`, `:core:common`, `:export-engine` | JVM stdlib + kotlinx only | **import `android.*` — CI fails the build** |
| `:sync-android` | `:data:transport`, WorkManager | touch UI |

Dependency direction is inward only. If code in a pure module "needs Android", the thing it needs belongs in a
platform module (TransportApp.md §5).

## §3 The MVVM/UDF contract

One direction of data, one owner of truth. Per screen:

```
Screen (entry)      fun XxxScreen(callbacks…, viewModel: XxxViewModel)   — @HiltViewModel via hiltViewModel()
                    collectAsState() → delegates, nothing else
Content (stateless) fun XxxContent(state: XxxUiState, onEvent: (XxxEvent) -> Unit, callbacks…)  — ALL Compose here
UiState             data class XxxUiState( … )                            — immutable; everything the screen shows
Event               sealed interface XxxEvent { … }                       — every user intent, named as a verb
ViewModel           class XxxViewModel @Inject constructor(repo: …) : ViewModel()
                      private val _uiState = MutableStateFlow(XxxUiState())
                      val uiState: StateFlow<XxxUiState> = _uiState.asStateFlow()
                      fun onEvent(event: XxxEvent) { when (event) { … _uiState.update { … } } }
Truth               Repository returns Flow/cold queries; Room is the single source of truth.
```

Contract rules:

1. **UiState is complete.** A screen must never reach outside `state` for visible data — not constants, not
   `SampleData`, not `BuildConfig`. Loading/empty/error are fields (`isLoading`, `error: ErrorCode?`) rendered
   via `core.designsystem` `States.kt`.
2. **Events are the only input.** One `onEvent` funnel per screen; one-shot effects (navigate, toast) are
   callbacks passed by the nav graph, never a shared event bus.
3. **ViewModels are Android-free of UI.** They may use `viewModelScope`, `StateFlow`, repositories. No Compose types.
4. **Derived UI values are computed in the UiState/reducer**, not remembered in the composable.
5. **Screen signatures stay stable**: `fun XxxScreen(onX: () -> Unit, …, viewModel: XxxViewModel = hiltViewModel())`
   so nav graphs read exactly like today's.
6. State transitions must be testable with a fake repository and a fake clock — no `Date()` inside a ViewModel.

## §4 Naming conventions

| Thing | Convention | Example |
|---|---|---|
| Package | feature-scoped, dots not underscores | `com.example.transportapp.feature.booking.screen` |
| Screen files | `XxxScreen.kt` holds entry + stateless Content | `RegisterScreen.kt` |
| UiState / ViewModel | `XxxUiState.kt`, `XxxViewModel.kt` beside the screen | |
| Sample data (until its screen migrates) | `core/ui/sample/XxxSampleData.kt`, `object XxxSampleData` | deleted at migration (D10) |
| Room entity | server table name + `_E`, one file per table | `CONSIGNMENT_E` → `ConsignmentEntity.kt` |
| DAO | per aggregate, `XxxDao.kt`, methods named `observe*` (Flow), `get*` (suspend once), `upsert*`, `softDelete*` | |
| Mapper | `XxxMapper.kt` in `data/transport/mapper`, `fun ConsignmentEntity.toDomain()` / `.toEntity()` | |
| Repository | `XxxRepository` interface in `:data:transport` + `XxxRepositoryImpl` (or single class while there is one impl) | |
| Route constant | UPPER_SNAKE in `Routes.kt` matching the §6 screen id comment | `const val REGISTER = "register"` |
| Events | verb phrases | `Submit`, `ChangeWeight`, `SelectConsignor`, `DismissException` |
| Money/Weight | `Money` (paise), `Weight` (grams) from `:core:common` only | `Money.fromRupees(3944)` |

## §5 The no-data-in-screens rule

A screen's `Content` composable renders `state` and nothing else. During Phase 2 migration the **only** permitted
data import in a `*Screen.kt` file is its own `*UiState`. Screens not yet migrated keep their `XxxSampleData`
import in the *entry* composable — never in the stateless Content. When a screen's repository lands, the
per-screen sample file is deleted and `SampleData.kt` retains only what the seeder needs.

## §6 Data-layer contracts

### 6.1 Repository
- Constructor-injected DAOs + `OutboxWriter` + dispatchers via Hilt.
- Reads return `Flow<DomainModel>` (cold Room flows). Lists that can grow unbounded use Paging 3 (`RegisterRepository`).
- Writes: `suspend fun x(): Result<T, ErrorCode>`. Inside, one Room `withTransaction { entity upsert + outbox enqueue }`.
- Validation calls `:domain:transport` (state machine, calculator, numbering) **before** any write; a failure writes nothing.
- Repos never return entities; mappers live in `:data:transport`.

### 6.2 Sync envelope (every synced table)
`server_id: String?` · `local_id: String` (UUID, PK) · `updated_at_local: Long` · `updated_at_server: Long?` ·
`sync_state: SyncState` (SYNCED/PENDING/CONFLICTED) · `deleted_at: Long?`.
Every read filters `deleted_at IS NULL`. Writes set `sync_state = PENDING`. Never hard-delete a synced row.

### 6.3 Outbox
`OutboxWriter.enqueue(op, entityType, entityLocalId, payloadJson, prerequisites: List<String> clientOpIds)`
inside the caller's transaction. `client_op_id` is one UUID per user action. The drain worker is a no-op until
Phase 3 — its ordering (prerequisites first) and idempotency (`DUP_CLIENT_OP`) are already guaranteed by the rows.

### 6.4 Status events
Append-only. `client_event_id` unique per company. `Held` requires `remark.length >= 10` and a reason code.
After append, `StatusRepository.rebuildProjection(consignmentId)` recomputes `status_projection` — no other writer exists.

### 6.5 Numbering
`NumberingRepository.nextNumber(series)` under a lease: consume `next_value`, refill locally at low water,
`PROV-<deviceShort>-n` + `ErrorCode.LEASE_EXHAUSTED` when a debug flag blocks grants. The stamped number goes into
the consignment **and** its `DOC_SNAPSHOT_E`.

### 6.6 Seeding / reset
`DemoSeeder` (versioned by `SEED_VERSION`) builds the §B6 canonical dataset on first launch; values must equal the
prototype's displayed numbers. Debug reset (T31) = delete DB + DataStore active context + reseed. Seed data lives in
`:core:database/seed` reading constants from `core.ui.sample.SampleData` — the only permitted consumer.

## §7 Hilt conventions

- `@HiltAndroidApp` on `TransportApp`; `@AndroidEntryPoint` on `MainActivity`; workers via `@HiltWorker` + factory.
- ViewModels: `@HiltViewModel class XxxViewModel @Inject constructor(…) : ViewModel()`; screens get them with
  `hiltViewModel()` (nav-graph scoped by default).
- Modules: `DatabaseModule` (DB + DAOs, `@Singleton`), `DataStoreModule`, `DataModule` (`@Binds` repo interfaces),
  `DispatcherModule` (`@IoDispatcher` qualifiers). No `@Inject` on Composables, no service locator, no `object` singletons for state.

## §8 Routes and navigation

All routes live in `core/ui/Routes.kt` with a `// Tn` comment per §6 of TransportApp.md. Any route argument that can
contain `/` (document numbers) **must** go through the `Uri.encode` helpers — `Routes.caseFile("IND/2627/04188")`.
Nav graphs are `NavGraphBuilder` extensions per feature (`XxxNavGraph.kt`) called from `AppNavHost` in declaration
order. The screen map (`SCREEN_INDEX`) navigates by route; production flows navigate by domain id.

## §9 Error handling

`Result<out T>` from `:core:common` with `ErrorCode` (the §18.3 catalogue, already defined). Rules:

- User-facing copy per code is centralised (`core/designsystem` `ErrorCopy.kt` once Phase 2 S5 introduces the first
  offline error); never a raw message or HTTP status at the call site.
- `OFFLINE_UNAVAILABLE` is a *typed, expected* state (freight-bill issue, XLSX): render the explanation + what
  happens when connectivity returns — not a toast.
- A failure must never corrupt local state: validation errors precede writes; write failures leave the outbox row
  retriable (replay-safe by `client_op_id`).

## §10 How to add a feature — the recipe

1. **Spec check:** find the screen id (§6) and its rules in TransportApp.md; its visual spec in Design.md; its sprint in Phase2.md.
2. **Route:** add/confirm in `Routes.kt` (+ encode helper if args), wire in the feature's `XxxNavGraph.kt`.
3. **UiState:** `XxxUiState` with every visible value + `isLoading`/`error`; defaults from `XxxSampleData` only if not yet migrated.
4. **Events:** `sealed interface XxxEvent` covering every user intent.
5. **Content:** stateless `XxxContent(state, onEvent, callbacks)` — move existing Compose here unchanged; compose from `core/designsystem` components only.
6. **Screen entry:** `XxxScreen(callbacks…, viewModel)` collecting `uiState`.
7. **ViewModel:** `@HiltViewModel`, inject the repository, reducer in `onEvent`, no Android UI imports.
8. **Data (Phase 2):** entity (`_E` + envelope) → DAO → mapper → repository methods → outbox enqueue in the same transaction → register in `DataModule`.
9. **Tests:** VM reducer test (fake repo), repo test (in-memory Room), DAO test; then the sprint's named Compose test.
10. **Delete the sample file** for that screen if migration is complete; run `:app:installDebug` and click the sprint demo path.

## §11 Commands

Run from `C:\Users\Lenovo\Desktop\Personal Project\Research\TransportApp2` (PowerShell):

```
.\gradlew.bat :app:compileDebugKotlin     # fastest full-graph typecheck
.\gradlew.bat :feature:<name>:compileDebugKotlin   # single module
.\gradlew.bat :app:installDebug           # build + install on the running emulator
.\gradlew.bat :core:database:testDebugUnitTest     # DAO/room tests (module name varies per suite)
.\gradlew.bat test                        # all JVM unit tests
.\gradlew.bat :app:connectedDebugAndroidTest       # instrumented (emulator required)
adb logcat --pid=<pid>                    # live logs
adb shell am start -n com.example.transportapp/.MainActivity
```

Version bumps go in `gradle/libs.versions.toml` only. Emulator: `emulator-5554`, 1280×2856 @ 480dpi.

## §12 Testing

| Layer | Framework/shape |
|---|---|
| `:domain:transport` | JVM, JUnit4: table-driven calculation matrix (§10.6 fixture first), state-machine transition table, rate resolution order |
| `:core:database` | Robolectric or in-memory Room on JVM: DAO CRUD, FTS, migration harness (`MigrationTestHarness`), seed fixtures |
| `:data:transport` | In-memory Room + fakes: transaction atomicity, outbox prerequisites, mapper round-trips |
| ViewModels | JUnit4 + fake repos + fake clock: reducer transitions only |
| Compose UI | `createComposeRule` / `createAndroidComposeRule`: booking-form validation, challan multi-select, every empty/error state named in §6 |
| Numbering | Coroutine concurrency test: N virtual devices, no duplicate/reuse across thousands of issues |
| Handwritten UI verification | `adb exec-out uiautomator dump` text vs `stitch_text_all.txt` ground truth (data-level parity) |

Naming: `given_when_then`. Every sprint's tests are listed in its Phase2.md section; they are part of DoD.

## §13 Definition of Done

A change is done when **all** hold:

- [ ] `.\gradlew.bat :app:compileDebugKotlin` green (no new warnings beyond the codebase's existing deprecations)
- [ ] New/changed screens render real `UiState`; no data imports in `Content` (§5)
- [ ] Writes transactional with outbox rows; validation before write; `Result` errors typed
- [ ] Reads filter tombstones; envelope fields maintained; no direct `status_projection` writes
- [ ] Unit tests for the changed domain/repo/DAO/VM paths; sprint-named Compose tests where listed
- [ ] `:app:installDebug` on `emulator-5554` runs; the sprint demo path from Phase2.md §7 is clickable end-to-end offline
- [ ] Migrated screens: per-screen `XxxSampleData.kt` deleted; `SampleData.kt` still compiles for remaining screens
- [ ] No secrets, no hardcoded keys, no TODOs without a sprint reference

## §14 Forbidden patterns

1. `Double`/`Float` for money or weight at rest — paise/grams `Long` only.
2. Network, `Date()`, `File` I/O, or RNG inside a ViewModel or Composable — behind seams (`:core:common` clock, repos).
3. Writing `status_projection`, totals "as truth" (unrecomputable), or hard-deleting a synced row.
4. `SampleData` imports inside a stateless `Content`, or in any file once its screen has migrated.
5. Feature→feature imports; `androidx.room` imports in features; `android.*` in pure modules.
6. Shadows, gradients, glassmorphism, emoji in UI, or any colour outside `Color.kt` (Design.md §A7/§A13).
7. Editing generated Room schemas by hand; skipping the migration test when schema version changes.
8. Silently swallowing a `Result.Err` — every error reaches the UiState with its code.
9. Renaming a client entity away from its server table base name (`_E` suffix is the only rename allowed).
10. Starting a new sprint before the previous sprint's DoD checklist passes.

---

*Glossary: Bilty = consignment note (GR/LR); Challan = loading challan; Party = customer/consignor/consignee;
To Pay/TBB/Paid = payment modes; Hamali = loading charge; bhada = lorry hire. Full table: TransportApp.md §2.*
