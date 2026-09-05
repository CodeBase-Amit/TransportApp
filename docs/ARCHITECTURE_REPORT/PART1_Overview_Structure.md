# Part 1 — Project Overview & Complete Project Structure

## 1. Project Overview

### 1.1 Purpose

TransportApp2 is an **offline-first native Android ERP for Indian road-transport operators**
(GTA — goods transport agencies). Its core promise: a transport clerk books a consignment
(bilty) once on a phone — possibly with **zero network signal** — and the app produces the
four printed copies of the consignment note, tracks the load through its journey (challan →
dispatch → hub → delivery → POD), computes freight/charges/GST with exact integer precision,
handles collections and freight billing, and exports accountant-ready CSV packs.

The product thesis (TransportApp.md §3): reduce booking to **two typed values and nine
taps**, and make the phone replace the physical bilty book — "the app works without signal;
the server mirrors what Room already holds."

### 1.2 Type of application

- **Native Android app** (Kotlin, Jetpack Compose, Material 3) — not Compose Multiplatform,
  not KMP-shared (the pure modules are JVM-only and backend-shareable by design, not by
  KMP toolchain).
- **Business/ERP class app**, single-activity, navigation-compose based.
- **Two "apps in one":** an offline ERP that must never require a server, and an
  online-synced client whose backend (a separate Next.js + MongoDB service in
  `Research/Backend/`) mirrors Room — never the reverse.

### 1.3 Target users

Per TransportApp.md §17.4.1, five ranked roles inside one company:

| Role | Rank | What they do |
|---|---|---|
| OWNER | 5 | everything; member management, numbering, company profile, delete |
| MANAGER | 4 | operations, overload approval, To-Pay waiver |
| ACCOUNTANT | 3 | billing, receipts, statements, reports, exports |
| BOOKING_CLERK | 2 | books bilties, prints copies |
| DELIVERY_CLERK | 1 | status updates, POD capture |

A "company" has branches; every row carries `company_id` (and where operational,
`branch_id`). The demo company is **Shivshakti Roadlines** (Indore HQ, Bhiwandi, Nagpur).

### 1.4 Main architecture

**MVVM-UDF with a strict multi-module Clean-Architecture slice** (Spec.md §2/§3):

```
UI (Compose Screen)  →  ViewModel (@HiltViewModel, StateFlow<UiState>)
        →  Repository interface (:data:transport, one per aggregate)
            →  Room DAOs (:core:database)          [reads AND writes]
            →  OutboxWriter (same transaction)     [sync side effects]
            →  engines (:doc-engine, :export-engine, pure JVM)
            →  :core:network (OkHttp)              [S23+; online tier]
```

The three architectural laws (violations fail CI via `checkPureModules`, defined in the
root `build.gradle.kts`):

1. **Pure modules may not import `android.*`**: `core/common`, `domain/transport`,
   `export-engine`, `doc-engine`, `core/network`. They are JVM-gradle modules.
2. **Features may not import `androidx.room`** or each other. Features see repositories
   (interfaces + implementations) and `core/ui` + `core/designsystem` only.
3. **Dependency direction is inward.** If code "needs Android" inside a pure module, that
   need belongs in a platform module.

### 1.5 Offline/online capabilities (D62)

- **Offline-first is the contract.** Room is the single source of truth for every read.
  The app books, prints (A4 PDF via WebView), dispatches, collects, bills, and exports
  with **zero network**.
- **Online tier (S23–S25)** adds: real email/password sign-in (with graceful degradation
  to the demo identity when the backend is unreachable), a masters refresh (server → Room
  upsert keyed by `server_id`), a real atomic number lease with local fallback, and the
  **outbox drain** (PARTY family → REST, more families pending). Pull refresh happens on
  company-picker entry and via T31 "Sync now".
- **The backend is never required to run.** Every network failure maps to
  `ErrorCode.OFFLINE_UNAVAILABLE` and the UI treats it as a normal, explained state.

### 1.6 Authentication method

- **Current (test tier):** `POST /api/auth/login` (email/password) and
  `POST /api/auth/dev-login` (seeded dev user, debug convenience). The JWT (7-day TTL,
  carries `sub/email/name/companyId/branchId/role`) is stored in DataStore
  (`SessionStore.token()`); an OkHttp interceptor attaches it as `Bearer`.
- **Mock identity:** a fresh install is signed **out**; sign-in without a reachable
  backend writes the demo identity (Mahesh Patidar / OWNER / Shivshakti Roadlines).
- **Production path (deferred):** Google Identity / Credential Manager. The seam is
  `SessionRepository.signIn()` — its body swaps, nothing else changes.

### 1.7 Sync strategy

**Transactional outbox with prerequisite ordering** (TransportApp.md §16.2, implemented
Phase 2): every repository write runs in **one Room `withTransaction`** that upserts the
entity **and** an `outbox` row (`client_op_id` UUID, op, entity_type, entity_local_id,
payload_json, state, attempt_count, next_attempt_at). Prerequisite edges
(`outbox_prereq`) guarantee a parent row drains before children that reference it.

- **Push (S25):** `OutboxDrainWorker` (WorkManager, 6 h periodic, CONNECTED constraint)
  → `OutboxPush.drain()` maps ready rows → REST. PARTY family first (INSERT → POST,
  UPDATE → PATCH by `server_id`, DELETE → DELETE); 2xx marks DONE and writes the server
  `_id` back onto the mirrored row; failures mark retriable with exponential backoff
  (60 s base, capped shift) and the typed error code for T31's queue.
- **Pull (S24):** `MastersRefresher` upserts remote docs keyed by `server_id`; rows created
  offline (`server_id = null`) are never touched by the refresher — the drain owns their
  server identity.
- **Full delta-sync/conflict machinery** (deterministic replay, `DUP_CLIENT_OP`
  idempotency, field-level merge) is **deliberately deferred** until the production
  backend exposes cursor + idempotency endpoints; the test backend's README declares the
  same boundary.

### 1.8 Main technologies

| Layer | Technology |
|---|---|
| UI | Jetpack Compose, Material 3, Navigation-Compose, Paging 3 (compose) |
| DI | Hilt (KSP), `@HiltViewModel`, `@HiltWorker` |
| Persistence | Room 2.8.4 (KSP), DataStore Preferences (session + active context) |
| Background | WorkManager 2.10.4 (periodic outbox drain) |
| Documents | doc-engine (custom JSON→HTML template engine) + pdf-android (headless WebView → `PrintManager`) |
| Export | export-engine (deterministic CSV, CRLF, golden-tested) |
| Network | OkHttp 4.12 + kotlinx-serialization (raw `ApiClient`, no Retrofit) |
| Tests | JUnit4, Robolectric (Compose UI tests, migration tests), MockWebServer, coroutine-test |
| CI | GitHub Actions: `checkPureModules` → `test` → `:app:assembleRelease` |

### 1.9 Build system

Gradle 9.x with **version catalogs** (`gradle/libs.versions.toml`) — every version bump
goes there and nowhere else. **Configuration cache is enabled**; build scripts capture
`projectDir` at configuration time only. KSP (not kapt) for Room + Hilt.

Key commands (PowerShell, from `TransportApp2/`):

```powershell
.\gradlew.bat :app:compileDebugKotlin      # fastest full-graph typecheck
.\gradlew.bat test                          # all JVM unit tests (Robolectric)
.\gradlew.bat checkPureModules              # architecture guard (CI runs it)
.\gradlew.bat :app:installDebug             # emulator-5554
.\gradlew.bat :app:assembleRelease          # signed 2.63 MB APK (needs key.properties)
.\gradlew.bat :doc-engine:test "-Pgolden.update=true"   # regenerate golden HTML (QUOTED in PS)
```

### 1.10 SDK versions

- **compileSdk 37**, **targetSdk 37**, **minSdk 24** (Android 7.0+)
- **Kotlin 2.2.10**, AGP 9.3.2 (**built-in Kotlin**: Android library modules must NOT also
  apply `org.jetbrains.kotlin.android` — "extension already registered" failure)
- Compose BOM 2026.02.01, Room 2.8.4, Hilt 2.60.1, Paging 3.5.1, WorkManager 2.10.4,
  kotlinx-serialization-json 1.11.0, OkHttp 4.12.0, Robolectric 4.16

### 1.11 Libraries (beyond the above)

Compose UI/graphics/tooling/foundation/material3 + **material-icons-extended**,
lifecycle-runtime/viewmodel/compose, activity-compose, navigation-compose, paging-runtime+
compose, datastore-preferences, hilt-android + hilt-navigation-compose, WorkManager,
pdf-android's `com.tom-roush`-style WebView approach (actually AndroidPdfRenderer using
`android.webkit` + `PrintManager` — see Part 3 §11), OkHttp, kotlinx-serialization-json.
Tests: junit4, robolectric, androidx.test core+junit, compose-ui-test-junit4+manifest,
mockwebserver, kotlinx-coroutines-test.

### 1.12 Design pattern

- **MVVM-UDF** with the exact shape fixed by Spec.md §3: `Screen(callbacks…, viewModel:
  HiltViewModel)` → stateless `Content(state: UiState, onEvent: (Event) -> Unit,
  callbacks…)`; UiState is a complete immutable data class; events are verb-named members
  of a `sealed interface XxxEvent`; **one-shot effects are callbacks from the nav graph**,
  never a shared event bus.
- **Repository per aggregate** with interface + `@Singleton` Impl in the same file
  (`:data:transport`); DAOs injected, mappers as extension functions inside the repo file.
- **Append-only event sourcing** for consignment status: `STATUS_EVENT_E` is the truth;
  `status_projection` is a materialised fold refreshed by
  `StatusRepository.rebuildProjection` — no other writer exists (Spec §14 forbids writing
  it directly).
- **State machine as data**: `ConsignmentStateMachine` and `TripStateMachine` are objects
  holding transition tables; repositories consult them before any append.

### 1.13 Modularization strategy

25 Gradle modules (see §2 tree). Split by: **purity** (five JVM modules that CI keeps
Android-free), **platform** (`pdf-android`, `sync-android`), **data slice**
(`core:database`, `core:datastore`, `data:transport`), **UI slice** (`core:designsystem`
tokens+components, `core:ui` routes+drawer), and **ten feature modules** that each own
their screens/ViewModels and depend only inward.

### 1.14 Package organization

Root package `com.example.transportapp`, then per-module:

- features: `com.example.transportapp.feature.<name>.screen` (screens/VMs/UiStates) and
  `…feature.<name>.navigation` (nav-graph builders)
- data: `…data.transport.<aggregate>` (repos) + `…data.transport.mapper`-style extension
  functions co-located + `…data.transport.di`
- core: `…core.database.{entity,dao,outbox,envelope,seed,di}`,
  `…core.datastore.{session,context}`, `…core.designsystem.{theme,component}`,
  `…core.ui` (Routes + AppNavDrawer + NavTab), `…core.network`
- domain: `…domain.transport.{calc,consignment,masters,org,tracking,trip}`

**Known wart (documented, not fixed):** source *directories* under `feature/` use dotted
folder names (`feature.auth/`, `feature.settings/`…) while package declarations are
dotted too. It compiles and IDE-navigates fine; a rename to nested folders was judged
cosmetic (analysis report flagged it; deliberately skipped).

### 1.15 Coding conventions

From Spec.md §4 + established practice (all verified in code):

- Screens: `XxxScreen.kt` holds entry `XxxScreen(...)` + stateless `XxxContent(...)`.
- UiState: `XxxUiState.kt`, one data class, `loading/error` fields always present.
- Events: `sealed interface XxxEvent`, verbs (`Submit`, `ChangeWeight`, `DismissException`).
- Entities: server table name + `_E`, one file per table; **the `_E` suffix is the only
  permitted rename** from the server schema.
- DAOs: per aggregate, methods `observe*` (Flow), `get*` (suspend once), `upsert*`,
  `softDelete*`/`tombstone*`.
- Repos: `XxxRepository` interface + `XxxRepositoryImpl`, `Result<T>` returns on writes,
  `Result.failure(code, message)` with `ErrorCode` §18.3 catalogue (20 codes; UI copy is
  centralised in `core/ui/ErrorCopy.kt`).
- Money: `Money` value class over paise `Long`; Weight: grams `Long`;
  `formatIndianGrouping` for lakh/crore display. **`Double`/`Float` for money/weight at
  rest is forbidden** (§14.1; enforced by review, S18 removed the last `toDoubleOrNull`
  from `MinQty.parse`).
- No data imports in `Content`; no `Date()`/RNG/File I/O in ViewModels (clock injected or
  `System.currentTimeMillis()` at the call boundary); every error reaches the UiState with
  its typed code.
- Encoding rule (project-critical on Windows): **never** read/write Kotlin sources with
  PowerShell `Get-Content`/`Set-Content`/`Out-File` — pipelines drop newlines and misread
  UTF-8; use the editor/file tools or .NET `[IO.File]::ReadAllText/WriteAllText` with
  `UTF8Encoding($false)`.

---

## 2. Complete Project Structure

Root: `Research/TransportApp2/` (git repo, branch `main`).

```
TransportApp2/
├── AgentChanges.md               # work log + decision table D1–D65 (append-only)
├── Analysis.md / Analysis_Report.md / TransportApp/Project_Analysis_2.md
│                                 # historical audits (partly stale — see Part 1 index)
├── Analysis_Report.md
├── INSTALL_CHECKLIST.md          # S26: the one-page hand-off note (sideload, first run,
│                                 # offline vs online, keystore backup warning)
├── Spec.md                       # operating manual: §2 modules, §3 MVVM/UDF, §4 naming,
│                                 # §11 commands, §12 testing, §13 DoD, §14 forbidden
├── Sprints.md                    # phase-1 sprint history
├── key.properties                # S26 release-signing credentials (GITIGNORED)
├── settings.gradle.kts           # 25 modules + google() + mavenCentral()
├── build.gradle.kts              # plugin aliases + checkPureModules guard task
├── gradle.properties             # config cache ON, JVM 2g
├── gradle/libs.versions.toml     # THE version catalog (single source of versions)
├── gradlew / gradlew.bat
├── screen_map.png … sprint7_final.png  # phase-1 screenshots (historical)
├── TransportApp/                 # sibling spec documents + Project_Analysis_2.md
│
├── app/                          # :app — the only Android APPLICATION module
│   ├── build.gradle.kts          # applicationId com.haulmate.transportapp, signing,
│   │                             # R8 (minify+shrink), proguard-rules.pro
│   ├── proguard-rules.pro        # keep rules: Room, Hilt, serialization, pdf bridge, fonts
│   ├── src/main/
│   │   ├── AndroidManifest.xml   # INTERNET, network_security_config, MainActivity, FileProvider
│   │   ├── java/com/example/transportapp/
│   │   │   ├── MainActivity.kt   # sole activity; CurrentActivity registration (S13)
│   │   │   ├── TransportApp.kt   # @HiltAndroidApp; debug-gated DemoSeeder call; WorkManager enqueue
│   │   │   ├── AppNavHost.kt     # the NavHost; startDestination = Routes.SPLASH (S17)
│   │   │   └── ScreenIndexScreen.kt  # dev screen map (debug long-press from T31 only, D53)
│   │   └── res/
│   │       ├── xml/network_security_config.xml        # HTTPS-only base (release)
│   │       ├── xml/file_paths.xml                     # FileProvider: pdfs/, pod/, exports/
│   │       └── values/, drawable/ (launcher icons etc.)
│   └── src/debug/res/xml/network_security_config.xml  # cleartext to 10.0.2.2/localhost ONLY
│
├── core/
│   ├── common/                   # pure JVM: Money, Weight, ErrorCode(20), Result, SeedIds
│   ├── database/                 # Room v12: entity/ (33) dao/ (10+3 infra) outbox/ envelope/
│   │   │                         #   seed/ (DemoSeeder, SeedVersion) TransportDatabase.kt
│   │   │                         #   di/DatabaseModule.kt; MIGRATION_1_2 … 11_12
│   │   └── src/test/             # Migration1to2…11to12Test, MigrationSmokeTest, seed tests
│   ├── datastore/                # DataStore: session/SessionStore.kt (+Snapshot), context/
│   │                             #   ActiveContextStore.kt — JWT, identity, active company
│   ├── designsystem/             # theme/ (Color, Type, Shape, Dimens, Motion, Theme) +
│   │                             #   component/ (AppBars, Buttons, Cards, Chips, DocketRow,
│   │                             #   Fields, RouteLine, SignaturePad, States)
│   ├── network/                  # S23 pure JVM: ApiClient, AuthApi, MastersApi(+NumberingApi)
│   │   └── src/test/             # ApiClientTest (MockWebServer, 6 cases)
│   └── ui/                       # Routes.kt, AppNavDrawer.kt, NavTab.kt, ErrorCopy.kt,
│                                 #   sample/ (residual sample rows: Party, RegisterRow…)
│
├── data/transport/               # the data slice: per-aggregate repositories + mappers
│   ├── build.gradle.kts          # deps: database, datastore, network, common, domain
│   └── src/main/java/…/data/transport/
│       ├── account/              # AccountDataRepository, SettingsRepository (profile, logo,
│       │                         #   branches, members+invites, series counter, profiles)
│       ├── billing/              # BillingRepository (unbilled pool, bills, receipts, statements)
│       ├── company/              # CompanyRepository (register company, picker, invitations)
│       ├── consignment/          # ConsignmentRepository (book/amend/cancel), RegisterRepository
│       │                         #   (Paging 3 + summary), CaseFileRepository
│       ├── dashboard/            # DashboardRepository (10 §13 tile queries in parallel)
│       ├── documents/            # DocumentRepository + PdfPort/PdfActions + OperationalDocuments
│       ├── masters/              # MastersRepository (9 master CRUD, merge), RateCardRepository,
│       │                         #   MastersRefresher (S24 pull)
│       ├── numbering/            # NumberingRepository (lease/peek/provisional/ensureSeries)
│       ├── outbox/               # OutboxWriter
│       ├── rate/                 # RateCardRepository (5-step resolution + booking settings)
│       ├── reports/              # ReportsRepository (freight register, CSV pack builder)
│       ├── session/              # SessionRepository + UserSession (+ signInWithPassword)
│       ├── templates/            # TemplateRepository (pinned-version lookup, install)
│       ├── tracking/             # StatusRepository (append, POD, waiver, refresh), PhotoImporter
│       ├── trip/                 # TripRepository (create/issue/dispatch/close, board, costs)
│       └── di/                   # DataModule (18 @Binds), DatabaseModule, NetworkModule,
│                                 #   DeviceModule, DispatcherModule, Qualifiers
│   └── src/test/                 # repo test suites (Robolectric, in-memory Room, fakes)
│
├── domain/transport/             # pure domain: calc/ (ChargeCalculator, RateResolver,
│                                 #   ChargeableWeight), consignment/ConsignmentStateMachine,
│                                 #   trip/TripStateMachine, masters/, org/, tracking/Ageing,
│                                 #   PaymentMode, RoleRank, TransportEnums
│
├── export-engine/                # pure: BiltyRegisterRow + CsvWriter (CRLF, UTC dates,
│                                 #   golden-tested byte determinism)
├── doc-engine/                   # pure: TemplateModel/TemplateParser/Expressions/HtmlRenderer
│                                 #   + src/test/resources/golden/bilty-04188.html
├── pdf-android/                  # AndroidPdfRenderer (headless WebView→PDF), PdfCallbackBridge
│                                 #   (android.print package), CurrentActivity registry
├── sync-android/                 # OutboxDrainWorker (S25: real drain via OutboxPush)
│
└── feature/                      # TEN feature modules, dotted source dirs (cosmetic wart)
    ├── auth/         # T0,T1,T2,T3,T32,T33 + LegalDoc; Carousel/SignIn/SetupWizard/Profile/
    │                 #   CompanyPicker/Splash VMs; AuthNavGraph
    ├── dashboard/    # T4 + DashboardViewModel (10 tiles)
    ├── booking/      # T5 BookingForm + T6 BiltyPreview; the money screen
    ├── consignment/  # T7 Register (Paging 3) + T8 CaseFile + T9 StatusUpdateSheet
    ├── challan/      # T10 ChallanBuilder + T11 ChallanDetail + T12 VehicleBoard
    ├── billing/      # T13 UnbilledPool + T14 FreightBill + T15 Payments + T16 Statement
    ├── masters/      # T17 hub + T18 list + T19 editor + T20 rate card
    ├── reports/      # T21 hub + T22 viewer + T23 ExportCentre
    ├── settings/     # T24 hub + T25 profile + T26 branches + T27 members + T28 numbering
    │                 #   + T31 account&data (+ NavigationTest)
    └── templates/    # T29 templates + T30 template requests
```

### Per-folder contract (what belongs / what must never appear)

| Folder | Belongs | Never | Key relationships |
|---|---|---|---|
| `app/` | MainActivity, AppNavHost, Application class, debug screen map, manifest, proguard, signing | screens' business logic, repositories | wires every feature graph; decides debuggability (features can't) |
| `core/common` | value types (Money/Weight), ErrorCode, Result, SeedIds | Android imports, Room, Compose | depended on by literally everything |
| `core/database` | entities, DAOs, outbox, migrations, seeder | domain imports, Compose, network | the only persistence; consumed by `data/transport` + `sync-android` |
| `core/datastore` | DataStore prefs: identity/JWT, active company/branch | Room, domain | consumed by `data/transport` (session, active context) |
| `core/designsystem` | theme tokens + all shared composables | repositories, ViewModels, navigation | consumed by every feature; S20 Expressive tokens live here |
| `core/ui` | Routes, drawer, tab helper, ErrorCopy, residual sample rows | Room, network | consumed by features; wraps designsystem |
| `core/network` | ApiClient/AuthApi/MastersApi/NumberingApi | Android imports, Room | consumed by `data/transport` only |
| `data/transport` | repositories (interface+impl), mappers, DI bindings, refresher, push | Compose, `android.*` UI | the ONLY module features may get data from |
| `domain/transport` | state machines, calculator, rate resolver, domain models | Android imports, Room, Compose | consumed by data + features (pure logic) |
| `export-engine` | CSV row model + writer | Android imports | consumed by reports/dashboard export paths |
| `doc-engine` | template parse/render | Android imports | consumed by `data/transport/documents` + `pdf-android` |
| `pdf-android` | WebView→PDF renderer, print/share bridges | Room, feature code | consumed by `data/transport/documents` (PdfPort) |
| `sync-android` | the drain worker only | UI | consumed by `app` (WorkManager enqueue) |
| `feature/*` | Screens, Contents, UiStates, Events, ViewModels, NavGraph builders | Room, other features | see `:data:transport` + `:core:*` only |
