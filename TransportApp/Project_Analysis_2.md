# TransportApp2 — Comprehensive Project Analysis

> **Prepared by:** Senior Developer & Product Manager Review
> **Date:** 2026-09-03
> **Scope:** Full codebase audit — UI, business logic, data layer, architecture, and roadmap

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [Architecture Overview](#2-architecture-overview)
3. [Module Map](#3-module-map)
4. [Dead-End Buttons & No-Op Handlers](#4-dead-end-buttons--no-op-handlers)
5. [Empty Navigation Callbacks](#5-empty-navigation-callbacks)
6. [Missing Features & Gaps](#6-missing-features--gaps)
7. [Data Flow & Entity Relationships](#7-data-flow--entity-relationships)
8. [Current Data Storage Architecture](#8-current-data-storage-architecture)
9. [Code Quality Issues](#9-code-quality-issues)
10. [State Machines & Business Rules](#10-state-machines--business-rules)
11. [Calculation Engine](#11-calculation-engine)
12. [Future Release Roadmap](#12-future-release-roadmap)
13. [Path 2 — Offline-First with Minimal Backend](#13-path-2--offline-first-with-minimal-backend)

---

## 1. Executive Summary

TransportApp2 is a **fully offline-first** Android application for Indian transport/logistics management. It handles consignment booking (bilty), trip management (challan), billing (freight bills, receipts, statements), and master data — all backed by a local Room database with **zero network calls**.

### What Exists Today (Phase 2)

| Layer | Status |
|-------|--------|
| **34 screens** across 8 feature modules | ✅ Complete UI with Material 3 design system |
| **37 Room entities**, 13 DAOs, 10 migrations | ✅ Full local persistence |
| **20 repositories** with transactional writes | ✅ Complete data access layer |
| **Calculation engine** (freight, GST, chargeable weight) | ✅ Functional |
| **State machines** (consignment 11-state, trip 5-state) | ✅ Enforced |
| **Outbox pattern** with dependency-aware ordering | ✅ Schema ready, drain is no-op |
| **Paging 3** register with filter chips | ✅ Functional |
| **Doc engine** (HTML bilty rendering) + PDF export | ✅ Functional |
| **CSV/XLSX export engine** | ✅ Functional |
| **Hilt DI** across all modules | ✅ 5 modules, 16 repository bindings |
| **Material 3 design system** (3-typeface, spring motion, tonal surfaces) | ✅ Complete |

### What's Missing (Phase 3+)

| Gap | Severity |
|-----|----------|
| **Zero network layer** — no Retrofit, no API, no backend | 🔴 Critical for production |
| **Zero authentication** — MockAuthTokenProvider only | 🔴 Critical |
| **Zero Firebase/Credential Manager** — no `google-services.json` | 🔴 Critical |
| **22 dead-end buttons** (onClick = {}) visible to users | 🟡 High |
| **7 no-op ViewModel event handlers** | 🟡 High |
| **7 empty navigation callbacks** | 🟡 Medium |
| **100% hardcoded strings** (zero i18n/stringResource) | 🟡 High — blocks localization |
| **No push notifications** | 🟡 Medium |
| **No analytics/crash reporting** | 🟡 Medium |
| **Logo upload not functional** (UI only) | 🟡 Medium |
| **Profile save is a no-op** | 🟡 Medium |
| **19 debug Log statements in production code** | 🟠 Low |

---

## 2. Architecture Overview

### Layered MVVM + Clean Architecture

```
┌─────────────────────────────────────────────────────┐
│                     UI Layer                         │
│  Jetpack Compose + Material 3 + Navigation Compose  │
│  34 Screens → 37 ViewModels (Hilt-injected)         │
├─────────────────────────────────────────────────────┤
│                   Domain Layer                       │
│  domain:transport module                            │
│  State machines, ChargeCalculator, RateResolver,    │
│  Money value class, pure Kotlin (no Android)        │
├─────────────────────────────────────────────────────┤
│                    Data Layer                        │
│  20 Repository classes → 13 Room DAOs               │
│  OutboxWriter for atomic outbox row creation        │
│  Entity-to-domain mappers (extension functions)     │
├─────────────────────────────────────────────────────┤
│                  Persistence Layer                   │
│  Room Database v11 ("transport.db")                 │
│  37 entity tables + 2 FTS4 virtual tables           │
│  10 migrations (schema 1 → 15)                      │
├─────────────────────────────────────────────────────┤
│                  Infrastructure Layer                │
│  DataStore (session, active context)                 │
│  WorkManager (OutboxDrainWorker — no-op)            │
│  Hilt DI (5 modules)                                │
│  sync-android (outbox, cursors — inert)             │
└─────────────────────────────────────────────────────┘
```

### Key Architectural Decisions

1. **Room is the single source of truth** — every read hits the local DB; dashboard tiles stamp results with "as of" timestamps.
2. **Every write is transactional with an outbox row** — repository methods wrap entity upsert + outbox upsert in a single Room `@Transaction`.
3. **No DTOs** — entities map directly to domain models via Kotlin extension functions.
4. **Dependency-aware outbox drain** — `OutboxPrereqEntity` ensures parent records sync before children (e.g., party before consignment that references it).
5. **Tombstone deletion** — rows are never hard-deleted; `deleted_at` is set, all queries filter `deleted_at IS NULL`.

---

## 3. Module Map

### 24 Gradle Modules

```
app                          → Application entry point
├── core/common              → Shared utilities (Money, ErrorCode, Result)
├── core/database            → Room DB, entities, DAOs, outbox, migrations
├── core/datastore           → DataStore (session, active context, auth)
├── core/designsystem        → Material 3 theme, shared components
├── core/ui                  → Routes, drawer, navigation helpers
├── domain/transport         → Domain models, state machines, calc engine
├── data/transport           → 20 repository implementations
├── sync-android             → OutboxDrainWorker (no-op sync infra)
├── export-engine            → CSV/XLSX generation
├── doc-engine               → Template parser, HTML renderer
├── pdf-android              → Android PDF rendering via WebView
├── feature/auth             → 7 screens: sign-in, company picker, setup, profile
├── feature/dashboard        → 1 screen: dashboard with 10 tiles
├── feature/booking          → 2 screens: booking form, bilty preview
├── feature/consignment      → 3 screens: register, case file, status sheet
├── feature/challan          → 3 screens: challan builder, detail, vehicle board
├── feature/billing          → 4 screens: unbilled pool, freight bill, payments, statement
├── feature/masters          → 4 screens: hub, list, editor, rate card editor
├── feature/reports          → 3 screens: hub, viewer, export centre
├── feature/settings         → 6 screens: hub, profile, branches, members, numbering, account data
└── feature/templates        → 2 screens: templates, template requests
```

### Dependency Graph (Simplified)

```
app → all feature/*, core/*
feature/* → core/designsystem, core/ui, core/database, domain/transport, data/transport
data/transport → core/database, domain/transport
domain/transport → core/common (no Android dependencies)
core/database → core/common
core/datastore → core/common
sync-android → core/database
export-engine → core/common
doc-engine → core/common
pdf-android → core/common, doc-engine
```

---

## 4. Dead-End Buttons & No-Op Handlers

### 4.1 Empty onClick Handlers — 22 Dead Buttons

These are buttons visible to users that do nothing when tapped:

| # | Screen | File | Element | Impact |
|---|--------|------|---------|--------|
| 1 | SplashScreen | `SplashScreen.kt:177` | "Force Update" primary button | Cannot trigger update |
| 2 | SplashScreen | `SplashScreen.kt:178` | "Force Update" note link | Cannot trigger update |
| 3 | BookingFormScreen | `BookingFormScreen.kt:553` | "Add charge" button | Cannot add charges |
| 4 | BiltyPreviewScreen | `BiltyPreviewScreen.kt:111` | Toolbar icon button | Unknown action |
| 5 | UnbilledPoolScreen | `UnbilledPoolScreen.kt:74` | Tune (filter) icon | Cannot filter pool |
| 6 | PaymentsScreen | `PaymentsScreen.kt:76` | Tune (filter) icon | Cannot filter payments |
| 7 | FreightBillScreen | `FreightBillScreen.kt:90` | MoreVert menu icon | No menu options |
| 8 | FreightBillScreen | `FreightBillScreen.kt:139` | Print action | Cannot print bill |
| 9 | FreightBillScreen | `FreightBillScreen.kt:140` | Share action | Cannot share bill |
| 10 | FreightBillScreen | `FreightBillScreen.kt:142` | More action | No additional actions |
| 11 | VehicleBoardScreen | `VehicleBoardScreen.kt:124` | Tune (filter) icon | Cannot filter board |
| 12 | TemplatesScreen | `TemplatesScreen.kt:164` | MoreVert menu icon | No template options |
| 13 | ReportsHubScreen | `ReportsHubScreen.kt:60` | History icon | No report history |
| 14 | ExportCentreScreen | `ExportCentreScreen.kt:75` | History icon | No export history |
| 15 | ExportCentreScreen | `ExportCentreScreen.kt:247` | Unknown button | Unknown action |
| 16 | NumberingScreen | `NumberingScreen.kt:119` | MoreVert menu icon | No numbering options |
| 17 | MembersScreen | `MembersScreen.kt:230` | Close (X) button | Cannot cancel invite |
| 18 | BranchesScreen | `BranchesScreen.kt:116` | MoreVert menu icon | No branch options |
| 19 | CaseFileScreen | `CaseFileScreen.kt:185` | Share icon | Cannot share case file |
| 20 | CaseFileScreen | `CaseFileScreen.kt:186` | MoreVert icon | No case file options |
| 21 | RegisterScreen | `RegisterScreen.kt:127` | Tune (filter) icon | Cannot filter register |
| 22 | RegisterScreen | `RegisterScreen.kt:128` | Export/download icon | Cannot export register |

### 4.2 No-Op ViewModel Event Handlers — 7 Instances

These events are dispatched by the UI but silently ignored in the ViewModel:

| # | ViewModel | Event | Code Location |
|---|-----------|-------|---------------|
| 1 | `ProfileViewModel` | `ProfileEvent.Save` | `feature/auth/.../ProfileViewModel.kt:50` |
| 2 | `ChallanDetailViewModel` | `ChallanDetailEvent.EditLoad` | `feature/challan/.../ChallanDetailViewModel.kt:211` |
| 3 | `ChallanDetailViewModel` | `ChallanDetailEvent.Print` | `feature/challan/.../ChallanDetailViewModel.kt:211` |
| 4 | `ChallanDetailViewModel` | `ChallanDetailEvent.Share` | `feature/challan/.../ChallanDetailViewModel.kt:211` |
| 5 | `ChallanDetailViewModel` | `ChallanDetailEvent.More` | `feature/challan/.../ChallanDetailViewModel.kt:211` |
| 6 | `StatementViewModel` | `StatementEvent.SendPdf` | `feature/billing/.../StatementViewModel.kt:59` |
| 7 | `SignInViewModel` | `SignInEvent.Terms` | `feature/auth/.../SignInViewModel.kt:41` |
| 8 | `SignInViewModel` | `SignInEvent.Privacy` | `feature/auth/.../SignInViewModel.kt:41` |
| 9 | `SplashViewModel` | `SplashEvent.UpdateNow` | `feature/auth/.../SplashViewModel.kt:35` |
| 10 | `CarouselViewModel` | `CarouselEvent.GetStarted` | `feature/auth/.../CarouselViewModel.kt:24` |
| 11 | `CarouselViewModel` | `CarouselEvent.Skip` | `feature/auth/.../CarouselViewModel.kt:24` |
| 12 | `RateCardEditorViewModel` | `RateCardEditorEvent.AddRate` | `feature/masters/.../RateCardEditorViewModel.kt:88` |

### 4.3 Summary by Priority

**P0 — User-visible dead buttons (22):**
- Filter icons on 4 screens (UnbilledPool, Payments, VehicleBoard, Register)
- Export/download icons on 2 screens (Register, ExportCentre)
- Print/Share on FreightBill and CaseFile
- MoreVert menus on 4 screens (FreightBill, Templates, Numbering, Branches)
- Add charge on BookingForm
- Force Update on SplashScreen

**P1 — No-op business events (12):**
- Profile save (user thinks profile is saved but it isn't)
- Challan print/share/edit load
- Statement PDF send
- Terms/Privacy pages
- Force update action
- Carousel navigation (GetStarted/Skip)
- Rate card add rate

---

## 5. Empty Navigation Callbacks

These navigation callbacks are wired as `{}` in NavGraph files, meaning the navigation action is silently swallowed:

| # | NavGraph | Callback | Impact |
|---|----------|----------|--------|
| 1 | `ConsignmentNavGraph.kt:37` | `onAddPhoto = {}` | Cannot add photo from case file |
| 2 | `ConsignmentNavGraph.kt:39` | `onCancel = {}` | Cannot cancel consignment from case file |
| 3 | `ConsignmentNavGraph.kt:41` | `onRaiseBill = {}` | Cannot raise bill from case file |
| 4 | `ConsignmentNavGraph.kt:42` | `onFullHistory = {}` | Cannot view full event history |
| 5 | `AuthNavGraph.kt:33` | `onTerms = {}` | Terms & Conditions page unreachable |
| 6 | `AuthNavGraph.kt:34` | `onPrivacy = {}` | Privacy Policy page unreachable |
| 7 | `ChallanNavGraph.kt:16` | `onCreate = {}` | Cannot create challan from navigation |

---

## 6. Missing Features & Gaps

### 6.1 Authentication & Backend

| Gap | Status | Notes |
|-----|--------|-------|
| Real authentication (Google Sign-In) | ❌ Not started | `MockAuthTokenProvider` returns `"mock.offline.token"` |
| Firebase / Credential Manager | ❌ Not started | No `google-services.json` |
| API service interfaces | ❌ Not started | Zero Retrofit/Ktor clients |
| Network layer (Retrofit/Ktor) | ❌ Not started | Entire app is local |
| Push notifications (FCM) | ❌ Not started | No notification infrastructure |
| Force update mechanism | ❌ Not started | Button exists but is dead |

### 6.2 Missing Business Features

| Gap | Screen | Details |
|-----|--------|---------|
| Profile save | ProfileScreen | Save event is no-op; changes lost on exit |
| Rate card "Add Rate" | RateCardEditorScreen | Button exists, event is no-op |
| Rate card entry validation | RateCardEditorScreen | No min/max validation visible |
| Terms & Conditions page | SignInScreen | Link exists, page doesn't |
| Privacy Policy page | SignInScreen | Link exists, page doesn't |
| Report history | ReportsHubScreen | History icon exists, no implementation |
| Export history | ExportCentreScreen | History icon exists, no implementation |
| Branch options menu | BranchesScreen | MoreVert icon exists, no dropdown |
| Member invite cancel | MembersScreen | Close button exists, no action |
| Template options menu | TemplatesScreen | MoreVert icon exists, no dropdown |
| Numbering options menu | NumberingScreen | MoreVert icon exists, no dropdown |
| Logo upload | CompanyProfileScreen | AddPhotoAlternate icon exists, no file picker |
| Receipt PDF generation | PaymentsScreen | No receipt document generation visible |
| Amendment workflow UI | CaseFileScreen | Amendment logic exists in repo, no UI path |

### 6.3 Missing Non-Functional Requirements

| Gap | Impact |
|-----|--------|
| Internationalization (i18n) | All 300+ strings hardcoded; app cannot be localized |
| Accessibility | No contentDescription on many icons; no semantics testing |
| Unit tests | No test files found in any module |
| UI tests | No Compose test files found |
| ProGuard/R8 rules | Not verified |
| App signing & release config | Not verified |
| Analytics (Firebase Analytics / custom) | Not started |
| Crash reporting (Firebase Crashlytics) | Not started |
| CI/CD pipeline | Not verified |
| Screen density assets | Not verified (ic_launcher adaptive icons exist but not audited) |

### 6.4 Incorrect/Missing Icons

| Location | Issue |
|----------|-------|
| `SignaturePad` in States.kt | Hardcoded `Color(0xFF71807A)` for ruler line — not theme-aware |
| `StatusUpdateSheet.kt:289` | `android.graphics.Color.WHITE` canvas background — not theme-aware |
| `StatusUpdateSheet.kt:293` | `android.graphics.Color.BLACK` for signature stroke — not theme-aware |
| `AndroidPdfRenderer.kt:335` | `android.graphics.Color.WHITE` for PDF background — not theme-aware |

---

## 7. Data Flow & Entity Relationships

### 7.1 Data Flow — Read Path

```
Room Database (transport.db)
       │
       ▼
   DAO (Flow<T> or suspend)
       │  • Flow queries: observeParties(), observeRegister(), observeBoard()
       │  • Suspend: getConsignment(), getBill(), getTrip()
       │
       ▼
   Repository
       │  • Maps entities → domain models via extension functions
       │  • Example: CompanyEntity.toDomain() → CompanySummary
       │  • Example: PartyEntity.toListRow() → PartyListRow
       │
       ▼
   ViewModel
       │  • Collects Flow<T> into StateFlow<UiState>
       │  • Applies business logic (state machine checks, validation)
       │
       ▼
   Composable UI
       │  • collectAsState() on viewModel.uiState
       │  • Renders using design system components
```

### 7.2 Data Flow — Write Path

```
UI Event (user tap)
       │
       ▼
   ViewModel.onEvent()
       │  • Validates input (e.g., state machine transition)
       │  • Calls repository method
       │
       ▼
   Repository.writeMethod()
       │  • Opens Room @Transaction { }
       │  • Upserts entity → entity table
       │  • Upserts outbox row → outbox table (same transaction!)
       │  • Optionally: upserts prerequisite outbox rows
       │  • Transaction commits atomically
       │
       ▼
   Room Database
       │  • Entity data persisted locally
       │  • Outbox row marked PENDING for future sync
       │  • Active Flow observers get automatic updates
       │
       ▼
   OutboxDrainWorker (every 6 hours)
       │  • Counts pending rows (no-op: does NOT consume)
       │  • Logs counts for monitoring
       │  • Phase 3: will push to server
```

### 7.3 Entity Relationship Diagram

```
┌──────────────────────────────────────────────────────────────────┐
│                     ORG AGGREGATE                                 │
│  COMPANY_E ─────┬──── BRANCH_E (company_id FK)                  │
│                 ├──── MEMBERSHIP_E (company_id FK)               │
│                 ├──── COMPANY_SETTING_E (company_id)              │
│                 └──── NUMBER_SERIES_E (company_id)                │
│                       └──── NUMBER_LEASE_E (series_id FK)        │
└──────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────┐
│                     MASTER AGGREGATE                              │
│  PARTY_E ──────────── RATE_CARD_E (party_id FK)                  │
│  STATION_E ───┬──── ROUTE_E (origin_station_id FK)              │
│               └──── ROUTE_E (dest_station_id FK)                 │
│  GOODS_E ────────── RATE_CARD_E (goods_id FK)                    │
│  VEHICLE_E                                                     │
│  DRIVER_E                                                      │
│  BROKER_E                                                      │
│  CHARGE_HEAD_E                                                  │
└──────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────┐
│                 CONSIGNMENT AGGREGATE (Central)                   │
│  CONSIGNMENT_E ──┬── CONSIGNMENT_ITEM_E (consignment_id FK)     │
│                  ├── CHARGE_LINE_E (consignment_id FK)           │
│                  ├── STATUS_EVENT_E (consignment_id FK)          │
│                  ├── DOC_SNAPSHOT_E (consignment_id FK)          │
│                  ├── ATTACHMENT_E (consignment_id FK)            │
│                  ├── POD_E (consignment_id FK)                   │
│                  └── TRIP_LEG_E (consignment_id FK)              │
│                                                                  │
│  References (logical, not FK):                                   │
│    consignor_id → PARTY_E                                        │
│    consignee_id → PARTY_E                                        │
│    route_id → ROUTE_E                                            │
│    from_station_id, to_station_id → STATION_E                    │
│    booking_branch_id → BRANCH_E                                  │
│    series_id → NUMBER_SERIES_E                                   │
└──────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────┐
│                     TRIP AGGREGATE                                │
│  TRIP_E ─────┬──── TRIP_LEG_E (trip_id FK)                      │
│              ├──── TRIP_COST_E (trip_id FK)                      │
│              └──── LORRY_HIRE_E (trip_id FK)                     │
│                                                                  │
│  References (logical):                                           │
│    vehicle_id → VEHICLE_E                                        │
│    driver_id → DRIVER_E                                          │
│    origin_branch_id → BRANCH_E                                   │
│    dest_station_id → STATION_E                                   │
└──────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────┐
│                     BILLING AGGREGATE                             │
│  FREIGHT_BILL_E ─── RECEIPT_ALLOCATION_E (bill_id FK)            │
│  RECEIPT_E ──────── RECEIPT_ALLOCATION_E (receipt_id FK)         │
│  RECEIPT_ALLOCATION_E ── CONSIGNMENT_E (consignment_id FK)       │
│  CREDIT_NOTE_E                                                  │
│                                                                  │
│  References (logical):                                           │
│    party_id → PARTY_E                                            │
└──────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────┐
│                     INFRASTRUCTURE                                │
│  outbox ───── outbox_prereq (outbox_id FK CASCADE)               │
│  sync_cursor                                                     │
│  seed_version                                                    │
│  PARTY_FTS (FTS4 over PARTY_E)                                   │
│  CONSIGNMENT_FTS (FTS4 over CONSIGNMENT_E)                       │
└──────────────────────────────────────────────────────────────────┘
```

### 7.4 Universal Sync Envelope

Every synced entity carries these columns:

| Column | Type | Purpose |
|--------|------|---------|
| `local_id` | String (PK) | Client-generated UUID |
| `server_id` | String? | Server-assigned ID (null until first sync) |
| `updated_at_local` | Long | Epoch millis of last local change |
| `updated_at_server` | Long? | Epoch millis of last server change |
| `sync_state` | SyncState | SYNCED / PENDING / CONFLICTED |
| `deleted_at` | Long? | Tombstone timestamp (never hard-delete) |
| `company_id` | String | Tenant scoping |

### 7.5 Entity Counts by Aggregate

| Aggregate | Entity Count | Key DAO |
|-----------|-------------|---------|
| Infrastructure | 4 (outbox, prereq, cursor, seed) | OutboxDao, SyncCursorDao, SeedVersionDao |
| Organization | 3 (company, branch, membership) | OrgDao |
| Masters | 9 (party, station, route, goods, vehicle, driver, broker, charge_head, rate_card) | MastersDao |
| Numbering | 2 (series, lease) | NumberingDao |
| Consignment | 7 (consignment, item, charge_line, status_event, doc_snapshot, attachment, pod) + 2 FTS | ConsignmentDao |
| Trip | 4 (trip, leg, cost, lorry_hire) | TripDao |
| Billing | 4 (freight_bill, credit_note, receipt, receipt_allocation) | BillingDao |
| Templates | 1 (template) | TemplateDao |
| Settings | 1 (company_setting) | SettingsDao |
| **Total** | **37 entity tables + 2 FTS** | **13 DAOs** |

---

## 8. Current Data Storage Architecture

### 8.1 Room Database

- **Name:** `transport.db`
- **Version:** 11 (schema version 15)
- **Entities:** 37 tables + 2 FTS4 virtual tables
- **Migrations:** 10 (MIGRATION_1_2 through MIGRATION_10_11)
- **TypeConverters:** SyncState, OutboxOp, OutboxEntityType, OutboxState (enum → String)

### 8.2 DataStore Preferences

| Store | File | Purpose |
|-------|------|---------|
| `SessionStore` | `session` Preferences DataStore | Signed-in user identity (userId, name, email, role, companyId, branchId) |
| `ActiveContextStore` | `active_context` Preferences DataStore | Current company/branch selection + per-branch sticky booking defaults |

### 8.3 Key Design Patterns

**Outbox Pattern (Dependency-Aware):**
```
Write to Room:
  1. upsert(entity) → entity table
  2. upsert(outbox_row) → outbox table
  3. [optional] upsert(prereqs) → outbox_prereq table
  All in one @Transaction — never partial
```

**Drain Order (Phase 3 ready):**
```sql
SELECT * FROM outbox
WHERE state = 'PENDING'
  AND next_attempt_at <= :now
  AND id NOT IN (
    SELECT outbox_id FROM outbox_prereq
    WHERE client_op_id NOT IN (
      SELECT client_op_id FROM outbox WHERE state = 'DONE'
    )
  )
ORDER BY created_at
LIMIT :limit
```

**FTS4 Full-Text Search:**
- `PARTY_FTS` — indexed on name, phone
- `CONSIGNMENT_FTS` — indexed on bilty_no, party_names
- Content-sync triggers maintain FTS automatically via Room migrations

### 8.4 Migrations History

| Migration | Schema | What Changed |
|-----------|--------|-------------|
| 1→2 | S1→S2 | Add org tables (Company, Branch, Membership) |
| 2→3 | S2→S3 | Add 9 master tables + PARTY_FTS + sync triggers |
| 3→4 | S3→S4 | ALTER ChargeHead (add default_value_paise, bearer); ALTER RateCard (add min/max freight) |
| 4→5 | S4→S5 | Add numbering (series, lease), consignment aggregate (7 tables) + CONSIGNMENT_FTS |
| 5→6 | S5→S7 | Add trip aggregate (trip, leg, cost, lorry_hire) |
| 6→7 | S7→S8 | Add attachment + POD tables |
| 7→8 | S8→S9 | Add billing aggregate (freight_bill, credit_note, receipt, allocation) |
| 8→9 | S9→S11 | Add template table |
| 9→10 | S11→S14 | Add company_setting table |
| 10→11 | S14→S15 | ALTER Consignment ADD amendment_reason |

---

## 9. Code Quality Issues

### 9.1 Hardcoded Strings (100% — Zero i18n)

The app has exactly **one** string resource in `strings.xml`:
```xml
<string name="app_name">TransportApp</string>
```

Every other user-facing string (300+) is hardcoded in Kotlin. This blocks:
- Localization (Hindi, Marathi, etc.)
- RTL layout adaptation
- Accessibility (TalkBack reads raw strings)
- Content changes without recompilation

**Categories of hardcoded strings:**
- Screen/section titles: "Register", "New challan", "The paper", "The money"
- Button labels: "Add charge", "Add article", "Clear filters", "Save receipt"
- Form field labels: "Company name", "Vehicle number", "Driver name", "GSTIN"
- Content descriptions: "Filter", "Export", "History", "More", "Back"
- Status text: "Payment due", "Unbilled freight", "Vehicles idle"
- Role labels: "Booking clerk", "Delivery clerk"

### 9.2 Debug Logging in Production (19 instances)

Most are in `pdf-android/AndroidPdfRenderer.kt` (14 Log.w calls) and `sync-android/OutboxDrainWorker.kt` (1 Log.i call).

### 9.3 Hardcoded Colors (4 instances)

| File | Color | Should Be |
|------|-------|-----------|
| `States.kt:164` | `Color(0xFF71807A)` | Theme token (rulerLine) |
| `StatusUpdateSheet.kt:289` | `android.graphics.Color.WHITE` | Theme-aware background |
| `StatusUpdateSheet.kt:293` | `android.graphics.Color.BLACK` | Theme-aware stroke |
| `AndroidPdfRenderer.kt:335` | `android.graphics.Color.WHITE` | Acceptable for PDF rendering |

### 9.4 Missing Standard Practices

| Practice | Status |
|----------|--------|
| `@Suppress` annotations | 0 (clean) |
| TODO/FIXME comments | 0 (clean — but arguably should have them for known gaps) |
| Unit tests | 0 test files found |
| Compose UI tests | 0 test files found |
| ProGuard rules | Not audited |

---

## 10. State Machines & Business Rules

### 10.1 Consignment State Machine (11 States)

```
DRAFT ──→ BOOKED ──→ LOADED ──→ IN_TRANSIT ──→ AT_HUB ──→ ARRIVED ──→ OUT_FOR_DELIVERY ──→ DELIVERED ✓
                │         │          │                          │
                │         │          ▼                          ▼
                │         │        HELD ───→ IN_TRANSIT       HELD ───→ RETURNED ✓
                │         │                 HELD ───→ ARRIVED
                │         │                 HELD ───→ RETURNED ✓
                │         ▼
                │      CANCELLED ✓
                └────→ CANCELLED ✓
```

**Terminal states (no transitions out):** DELIVERED, CANCELLED, RETURNED

**Enforced business rules:**
- Draft content is mutable; from Booked onward, corrections are amendments
- Delivered requires POD record or Manager waiver
- To Pay delivery requires collection recorded or waived
- Exceptions (Held) require reason code + remark ≥ 10 characters
- Cancelled retains bilty number permanently

### 10.2 Trip State Machine (5 States)

```
OPEN ──→ ISSUED ──→ DISPATCHED ──→ CLOSED ✓
  │         │
  │         ▼
  │      CANCELLED ✓
  └──→ CANCELLED ✓
```

**Terminal states:** CLOSED, CANCELLED

**Vehicle assignment guard:** `isOpen()` returns true for ISSUED or DISPATCHED — a vehicle on an open trip cannot be assigned to another.

---

## 11. Calculation Engine

### 11.1 ChargeCalculator

**Module:** `domain/transport`

**Capabilities:**
- **Chargeable weight** — max(actual_weight, volumetric_weight) with configurable divisor
- **Freight** — rate × quantity based on basis (PER_KG, PER_TONNE, PER_QUINTAL, PER_PACKAGE, PER_TRIP, FIXED)
- **GST** — forward charge (IGST for interstate, CGST+SGST for intrastate), reverse charge, exempt
- **Additional charges** — FLAT, PER_PACKAGE, PER_KG, PERCENT_OF_FREIGHT, PERCENT_OF_VALUE, PER_DAY

**Charge Head Bases:** `FLAT`, `PER_PACKAGE`, `PER_KG`, `PERCENT_OF_FREIGHT`, `PERCENT_OF_VALUE`, `PER_DAY`

**GST Treatments:** `INTERSTATE` (IGST), `INTRASTATE` (CGST + SGST), `EXEMPT`

### 11.2 Rate Resolver

**5-step resolution cascade:**
1. Party + Route + Goods (most specific)
2. Party + Route
3. Route + Goods
4. Route only
5. Company default

### 11.3 Money Value Class

- Indian digit grouping (lakhs/crores, not millions/billions)
- In-words conversion (e.g., "Rupees One Lakh Twenty Three Thousand Four Hundred Fifty Six and Seventy Eight Paise Only")
- Arithmetic operators (+, -, ==, compare)
- `formatted()` with ₹ symbol

---

## 12. Future Release Roadmap

### Phase 3 — Backend & Sync (Critical Path)

| Sprint | Deliverable | Effort |
|--------|-------------|--------|
| 3.1 | **API design** — REST/GraphQL schema for all 30+ entity types | 2 weeks |
| 3.2 | **Backend setup** — Firebase or custom (Supabase/Hasura); Auth (Firebase Auth + Credential Manager) | 3 weeks |
| 3.3 | **Retrofit/Ktor integration** — API service interfaces, interceptors, error handling | 2 weeks |
| 3.4 | **Outbox drain activation** — Real push in OutboxDrainWorker, conflict resolution (last-write-wins or field-level merge) | 3 weeks |
| 3.5 | **Delta sync** — Pull-based sync using SyncCursorEntity, initial full sync, incremental updates | 2 weeks |
| 3.6 | **Push notifications** — FCM for status updates, bill issuance, trip dispatch | 1 week |
| 3.7 | **Multi-device conflict handling** — Offline edits on multiple phones, merge strategy | 2 weeks |

### Phase 4 — Production Readiness

| Sprint | Deliverable | Effort |
|--------|-------------|--------|
| 4.1 | **i18n** — Extract all 300+ strings to `strings.xml`; Hindi + English | 2 weeks |
| 4.2 | **Unit tests** — ViewModel tests, Repository tests, State machine tests, Calculator tests | 3 weeks |
| 4.3 | **UI tests** — Compose test for critical flows (booking, status update, billing) | 2 weeks |
| 4.4 | **Analytics & crash reporting** — Firebase Analytics + Crashlytics | 1 week |
| 4.5 | **ProGuard/R8** — Obfuscation, optimization, seed rules | 1 week |
| 4.6 | **Release signing** — Keystore setup, Play Store config | 3 days |
| 4.7 | **Performance audit** — Database indexing, Compose recomposition profiling, memory leaks | 1 week |

### Phase 5 — Business Features

| Sprint | Deliverable | Effort |
|--------|-------------|--------|
| 5.1 | **Dead button activation** — Implement all 22 dead onClick handlers + 12 no-op events | 2 weeks |
| 5.2 | **Profile save** — Wire profile save to DataStore + outbox | 3 days |
| 5.3 | **Photo/attachment flow** — Camera integration, gallery picker, upload | 1 week |
| 5.4 | **POD (Proof of Delivery)** — Photo capture, signature pad save, GPS stamp | 1 week |
| 5.5 | **Terms & Privacy pages** — WebView or static content | 2 days |
| 5.6 | **Receipt PDF generation** — Print receipts from payments screen | 1 week |
| 5.7 | **Amendment UI** — Full amendment workflow from case file | 1 week |
| 5.8 | **Logo upload** — File picker, image compression, upload to server | 3 days |

### Phase 6 — Enhancement

| Feature | Effort |
|---------|--------|
| Barcode/QR code scanning for bilty numbers | 1 week |
| GPS tracking for trips (background location) | 2 weeks |
| E-Way Bill integration (GST portal API) | 3 weeks |
| Multi-company support (switch between companies) | 1 week |
| WhatsApp integration for bilty PDF sharing | 1 week |
| SMS integration for status updates | 3 days |
| Vehicle maintenance tracking | 1 week |
| Driver fatigue/rest compliance tracking | 1 week |
| Route optimization suggestions | 2 weeks |
| Automated TDS calculation | 1 week |

---

## 13. Path 2 — Offline-First with Minimal Backend

### 13.1 Design Philosophy

> **"Room is the truth. The server is a mirror."**

The app must work identically with zero network. The backend's role is:
1. **Backup & restore** — device replacement recovery
2. **Multi-device sync** — same data on owner's phone + accountant's phone
3. **Push notifications** — status updates from drivers/branches
4. **Compliance** — GST e-way bill generation, government portal integration

### 13.2 Recommended Minimal Backend Stack

| Component | Recommendation | Why |
|-----------|---------------|-----|
| **Auth** | Firebase Auth (Google Sign-In + phone OTP) | Zero backend auth code; handles sessions, tokens, refresh |
| **Database** | Supabase (PostgreSQL) or Cloud SQL | Supabase gives REST + real-time + Row Level Security out of the box |
| **File storage** | Firebase Storage or Supabase Storage | For attachments, POD photos, PDFs |
| **Push notifications** | Firebase Cloud Messaging (FCM) | Standard for Android; no custom server needed |
| **API** | Supabase REST/GraphQL or minimal Ktor/Express | Supabase auto-generates APIs from DB schema; alternatively 10-15 Ktor endpoints |
| **Background sync** | WorkManager (existing OutboxDrainWorker) | Already architected; just activate the drain body |

**Total estimated backend code: ~500-800 lines** if using Supabase (mostly client-side sync logic). ~3000-5000 lines if building a custom Ktor/Express API.

### 13.3 Supabase-First Architecture

```
┌──────────────────────┐          ┌─────────────────────────┐
│     TransportApp2    │          │       Supabase           │
│     (Android)        │          │                         │
│                      │   REST   │  PostgreSQL (mirror of   │
│  Room DB ←→ Outbox   │◄───────►│  Room schema)            │
│     ↓                │          │                         │
│  OutboxDrainWorker   │   FCM    │  Edge Functions          │
│     ↓                │◄─────────│  (outbox processor)      │
│  UI (Compose)        │          │                         │
│                      │  Auth    │  Auth (Google + phone)   │
│  DataStore           │◄─────────│  Storage (files/PDFs)    │
└──────────────────────┘          └─────────────────────────┘
```

### 13.4 Sync Protocol Design

#### Phase A: Initial Sync (First Login on New Device)

```
1. Auth: Google Sign-In → Firebase ID token → Supabase JWT
2. Pull: GET /sync/full?company_id=X
   → Downloads all non-deleted entities for the company
   → Response: { entities: [...], cursor: "2026-09-03T10:00:00Z" }
3. Write: Upsert all entities into local Room DB
4. Set sync_cursor to server cursor
5. All outbox rows already PENDING from local edits → will push in Phase B
```

#### Phase B: Incremental Sync (OutboxDrainWorker — every 6 hours or manual trigger)

```
PUSH (local → server):
1. GetReady(limit=100) → dependency-ordered pending outbox rows
2. For each row:
   a. PUT /entities/{entity_type}/{local_id}
      Body: payload_json + client_op_id (idempotency key)
   b. Server responds: { server_id, updated_at_server, sync_state }
   c. MarkDone(row_id) + update entity's server_id and sync_state
3. If any row returns CONFLICT:
   a. Server wins on non-conflicting fields (last-write-wins)
   b. Merge strategy for conflicting fields:
      - Consignment fields: server wins (source of truth for delivery status)
      - Party/Master fields: local wins (user's edits are authoritative)
      - Billing fields: server wins (financial integrity)
4. Retry failed rows with exponential backoff (existing backoff in OutboxEntity)

PULL (server → local):
1. Read sync_cursor for each entity family
2. GET /sync/delta?family={entity_family}&since={cursor}
3. Upsert received entities into Room
4. Apply tombstones (deleted_at from server)
5. Update sync_cursor
```

#### Phase C: Real-Time Notifications (FCM)

```
Server sends FCM when:
- Another device pushes a status update (IN_TRANSIT → AT_HUB)
- A receipt is allocated against a bill
- A trip is dispatched or closed
- A membership invitation is sent/accepted

Client handles FCM:
- Updates local Room via push payload
- Triggers recomposition via Flow observers
```

### 13.5 Minimal API Endpoints (if not using Supabase auto-gen)

| Method | Endpoint | Purpose |
|--------|----------|---------|
| `POST` | `/auth/exchange` | Exchange Firebase ID token for app JWT |
| `GET` | `/sync/full` | Full company data dump (first sync) |
| `GET` | `/sync/delta` | Delta sync since cursor |
| `PUT` | `/entities/{type}/{localId}` | Push one entity change (idempotent via client_op_id) |
| `POST` | `/entities/batch` | Push multiple entity changes (batch outbox drain) |
| `POST` | `/files/upload` | Upload attachment/POD photo |
| `GET` | `/files/{id}` | Download attachment/POD photo |
| `POST` | `/fcm/send` | Send push notification to device tokens |

**8 endpoints total.** This is achievable in 1-2 weeks with Ktor or Supabase Edge Functions.

### 13.6 Conflict Resolution Matrix

| Entity Family | Conflict Strategy | Rationale |
|---------------|-------------------|-----------|
| Party, Station, Route, Goods | **Local wins** | User's master data edits are authoritative |
| Vehicle, Driver, Broker | **Local wins** | Same reason |
| Consignment | **Hybrid** | Status fields → server wins; booking fields → local wins (if first sync) |
| Trip | **Server wins** | Trip state is managed by operations |
| Freight Bill, Receipt | **Server wins** | Financial integrity requires single source |
| Rate Card | **Last-write-wins** | Pricing can be negotiated on either device |
| Template, Settings | **Server wins** | Admin configuration |
| Membership | **Server wins** | Organizational control |

### 13.7 Implementation Steps

```
Week 1-2: Supabase setup
  ├── Create Supabase project
  ├── Mirror Room schema as Postgres tables (same column names)
  ├── Enable Row Level Security (RLS) per company_id
  ├── Set up Firebase Auth → Supabase JWT mapping
  └── Create Edge Function for outbox processing

Week 3-4: Client sync layer
  ├── Retrofit/OkHttp client for Supabase REST API
  ├── SyncManager class (orchestrates push/pull)
  ├── Activate OutboxDrainWorker.doWork() body
  ├── Initial full sync flow (after first login)
  └── Delta sync flow (periodic + manual trigger)

Week 5: File storage + FCM
  ├── Firebase Storage for attachments
  ├── FCM integration for push notifications
  └── Handle FCM data messages for real-time updates

Week 6: Testing + hardening
  ├── Offline → online transition testing
  ├── Multi-device conflict testing
  ├── Sync error handling and retry
  └── Performance benchmarking
```

### 13.8 Data Flow Diagram — Offline-First Lifecycle

```
                    ┌──────────────────┐
                    │   Device A       │
                    │   (Booking Clerk)│
                    └────────┬─────────┘
                             │
                    Local Room DB
                    ┌────────┴─────────┐
                    │  BOOKED bilty    │
                    │  + outbox row    │──── No network? No problem.
                    └────────┬─────────┘     Data saved locally.
                             │
                    ┌────────┴─────────┐
                    │  Network available│
                    │  OutboxDrainWorker│
                    │  pushes outbox row│
                    └────────┬─────────┘
                             │ REST API
                    ┌────────┴─────────┐
                    │   Supabase       │
                    │   (PostgreSQL)   │
                    └────────┬─────────┘
                             │ FCM push
                    ┌────────┴─────────┐
                    │   Device B       │
                    │   (Accountant)   │
                    │                  │
                    │  Receives push   │
                    │  → upserts to    │
                    │    local Room DB  │
                    │  → Flow observer │
                    │    → UI updates  │
                    └──────────────────┘
```

---

## Appendix A — Screen Inventory (34 Screens)

| ID | Screen | Module | Has Nav? | Has ViewModel? | Dead Buttons? |
|----|--------|--------|----------|----------------|---------------|
| T0 | SplashScreen | auth | ✅ | SplashViewModel | 2 (force update) |
| T1 | SignInScreen | auth | ✅ | SignInViewModel | 0 |
| T2 | CompanyPickerScreen | auth | ✅ | CompanyPickerViewModel | 0 |
| T3 | SetupWizardScreen | auth | ✅ | SetupWizardViewModel | 0 |
| T4 | DashboardScreen | dashboard | ✅ | DashboardViewModel | 0 |
| T5 | BookingFormScreen | booking | ✅ | BookingFormViewModel | 1 (add charge) |
| T6 | BiltyPreviewScreen | booking | ✅ | — | 1 (toolbar) |
| T7 | RegisterScreen | consignment | ✅ | RegisterViewModel | 2 (filter, export) |
| T8 | CaseFileScreen | consignment | ✅ | — | 2 (share, more) |
| T9 | StatusUpdateSheet | consignment | ✅ | StatusUpdateSheetViewModel | 0 |
| T10 | ChallanBuilderScreen | challan | ✅ | ChallanBuilderViewModel | 0 |
| T11 | ChallanDetailScreen | challan | ✅ | ChallanDetailViewModel | 0 |
| T12 | VehicleBoardScreen | challan | ✅ | VehicleBoardViewModel | 1 (filter) |
| T13 | UnbilledPoolScreen | billing | ✅ | UnbilledPoolViewModel | 1 (filter) |
| T14 | FreightBillScreen | billing | ✅ | FreightBillViewModel | 4 (more, print, share, more) |
| T15 | PaymentsScreen | billing | ✅ | PaymentsViewModel | 1 (filter) |
| T16 | StatementScreen | billing | ✅ | StatementViewModel | 0 |
| T17 | MastersHubScreen | masters | ✅ | MastersHubViewModel | 0 |
| T18 | MasterListScreen | masters | ✅ | MasterListViewModel | 0 |
| T19 | MasterEditorScreen | masters | ✅ | MasterEditorViewModel | 0 |
| T20 | RateCardEditorScreen | masters | ✅ | RateCardEditorViewModel | 0 |
| T21 | ReportsHubScreen | reports | ✅ | ReportsHubViewModel | 1 (history) |
| T22 | ReportViewerScreen | reports | ✅ | ReportViewerViewModel | 0 |
| T23 | ExportCentreScreen | reports | ✅ | ExportCentreViewModel | 2 (history, unknown) |
| T24 | SettingsHubScreen | settings | ✅ | SettingsHubViewModel | 0 |
| T25 | CompanyProfileScreen | settings | ✅ | CompanyProfileViewModel | 0 |
| T26 | BranchesScreen | settings | ✅ | BranchesViewModel | 1 (more) |
| T27 | MembersScreen | settings | ✅ | MembersViewModel | 1 (cancel) |
| T28 | NumberingScreen | settings | ✅ | NumberingViewModel | 1 (more) |
| T29 | TemplatesScreen | templates | ✅ | TemplatesViewModel | 1 (more) |
| T30 | TemplateRequestsScreen | templates | ✅ | TemplateRequestsViewModel | 0 |
| T31 | AccountDataScreen | settings | ✅ | AccountDataViewModel | 0 |
| T32 | CarouselScreen | auth | ✅ | CarouselViewModel | 0 |
| T33 | ProfileScreen | auth | ✅ | ProfileViewModel | 0 |
| — | ScreenIndexScreen | auth | ✅ | — | 0 |

---

## Appendix B — ViewModel Inventory (37 ViewModels)

All ViewModels are Hilt-injected via `@HiltViewModel` and expose `StateFlow<UiState>` via `viewModel.uiState`.

| ViewModel | Screen | Key Events | No-Op Events |
|-----------|--------|------------|--------------|
| SplashViewModel | T0 | ContinueOffline, Retry | UpdateNow |
| SignInViewModel | T1 | GoogleSignIn | Terms, Privacy |
| CompanyPickerViewModel | T2 | SelectCompany, CreateCompany | — |
| SetupWizardViewModel | T3 | SetName, SetBranch, SetGstin, Finish | — |
| DashboardViewModel | T4 | Refresh, NavigateToTile | — |
| BookingFormViewModel | T5 | SetField, Book, Amend | — |
| RegisterViewModel | T7 | ChangeSearchQuery, ToggleChip, ClickDocket | — |
| StatusUpdateSheetViewModel | T9 | AppendEvent, RecordPod | — |
| ChallanBuilderViewModel | T10 | LoadPool, CreateTrip | — |
| ChallanDetailViewModel | T11 | Dispatch, CloseTrip, StartAddCost | EditLoad, Print, Share, More |
| VehicleBoardViewModel | T12 | Filter, ClickVehicle | — |
| UnbilledPoolViewModel | T13 | Filter, SelectParty, CreateBill | — |
| FreightBillViewModel | T14 | RemoveRow, ShowPreview, Issue | — |
| PaymentsViewModel | T15 | SelectTab, OpenCollect, SaveCollect | — |
| StatementViewModel | T16 | — | SendPdf |
| MastersHubViewModel | T17 | NavigateToType | — |
| MasterListViewModel | T18 | Search, Create, ClickItem | — |
| MasterEditorViewModel | T19 | Save, Delete | — |
| RateCardEditorViewModel | T20 | SaveRate, ToggleViewAll | AddRate |
| ReportsHubViewModel | T21 | ChangePeriod, ClickReport | — |
| ReportViewerViewModel | T22 | Export, Filter | — |
| ExportCentreViewModel | T23 | SelectType, Export | — |
| SettingsHubViewModel | T24 | Navigate, ToggleSetting | — |
| CompanyProfileViewModel | T25 | Change*, Save, RequestDelete | — |
| BranchesViewModel | T26 | AddBranch | — |
| MembersViewModel | T27 | Invite, ChangeRole | — |
| NumberingViewModel | T28 | SetCounter | — |
| TemplatesViewModel | T29 | Select, Preview | — |
| TemplateRequestsViewModel | T30 | Approve, Reject | — |
| AccountDataViewModel | T31 | TrySync, Leave, RequestDelete, ConfirmDelete | — |
| CarouselViewModel | T32 | PageNext, PagePrev | GetStarted, Skip |
| ProfileViewModel | T33 | SignOut, Clear, Redraw, ChangeLanguage, Toggle* | Save |
| ScreenIndexViewModel | dev | — | — |

---

## Appendix C — Repository Inventory (20 Repositories)

| Repository | DAOs Used | Key Operations |
|-----------|-----------|----------------|
| SessionRepository | — (DataStore) | signIn, signOut, session Flow |
| CompanyRepository | OrgDao | registerCompany, selectCompanyAndBranch, acceptInvitation |
| MastersRepository | MastersDao, OrgDao | CRUD for all 9 master types, mergeParties, autoCharges |
| ConsignmentRepository | ConsignmentDao, MastersDao, NumberingDao | book, amend, cancel (transactional with outbox) |
| RegisterRepository | ConsignmentDao | pagingRegister (Paging3), registerSummary |
| CaseFileRepository | ConsignmentDao, MastersDao | caseFile (full detail view) |
| BillingRepository | BillingDao, ConsignmentDao, OrgDao, NumberingDao | unbilledPool, draftBills, createBill, issueBill, recordReceipt, allocateReceipt, statement |
| DashboardRepository | DashboardDao | dashboard (10 tiles in parallel) |
| TripRepository | TripDao, ConsignmentDao, OrgDao, MastersDao | createTrip, issueTrip, dispatchTrip, closeTrip, vehicleBoard |
| StatusRepository | ConsignmentDao, BillingDao, TripDao | appendEvent, bulkAppendByChallan, recordPod, overdueCounts |
| NumberingRepository | NumberingDao | nextNumber (lease-based) |
| RateCardRepository | MastersDao, OrgDao, SettingsDao | resolveRate (5-step cascade) |
| ReportsRepository | ReportsDao | freightRegister, outstandingTotals, revenueByRoute |
| TemplateRepository | TemplateDao | getActiveTemplate, upsertTemplate |
| DocumentRepository | ConsignmentDao, TemplateDao | PDF generation |
| AccountDataRepository | ConsignmentDao, ReportsDao, MastersDao, OutboxDao | account statistics, sync status |
| SettingsRepository | OrgDao, NumberingDao | company settings, series management |

---

## Appendix D — Key Architectural Constants

| Constant | Value | Location |
|----------|-------|----------|
| Database name | `"transport.db"` | TransportDatabase.kt |
| Database version | 11 (schema 15) | TransportDatabase.kt |
| Outbox drain interval | 6 hours | OutboxDrainWorker.kt |
| Outbox drain batch size | 100 rows | OutboxDrainWorker.kt |
| Search debounce | `SEARCH_DEBOUNCE_MS` | RegisterViewModel.kt |
| Demo seed version | 8 | SeedVersionEntity.kt |
| Demo identity | Mahesh Patistar, OWNER, Shivshakti Roadlines | SessionSnapshot.kt |
| Auth token | `"mock.offline.token"` | MockAuthTokenProvider.kt |
| FTS tables | PARTY_FTS, CONSIGNMENT_FTS | TransportDatabase.kt |

---

*End of analysis. This document should be treated as the living reference for all Phase 3+ planning and implementation.*
