# Part 2 — Files, Screens, UI Components, Buttons, Cards

## 3. Every File Documentation

The tables below cover **every source file** in the app's ~150-file surface. Columns are
condensed: **Purpose · Exports · Called-by · Calls · Owns · Risks**. Files whose full
behaviour is documented in later parts are cross-referenced instead of repeated.

### 3.1 `:app` module

| File | Purpose | Exports | Called by | Calls | Risks |
|---|---|---|---|---|---|
| `MainActivity.kt` | sole activity; `@AndroidEntryPoint`; registers the resumed activity into pdf-android's `CurrentActivity` registry (D49) so `PrintManager` can drive from repository depth | nothing | launcher, AppNavHost | CurrentActivity.register, setContent { TransportAppTheme { AppNavHost } } | if another activity ever takes focus during a render, print falls back gracefully |
| `TransportApp.kt` | `@HiltAndroidApp`; on create: debug-gated `DemoSeeder.seedIfNeeded()` (FLAG_DEBUGGABLE only, A1), `OutboxDrainWorker.enqueue` | nothing | system | Hilt, DemoSeeder, WorkManager | seeding is synchronous in debug by design (picker/dashboard race) — never enable in release |
| `AppNavHost.kt` | the `NavHost`; `startDestination = Routes.SPLASH` always (D53); declares every feature graph + the debug screen-map route | `AppNavHost` | MainActivity | every feature NavGraph | adding a screen = add a `composable` here or in a feature graph; debug screen-map route stays registered for T31 long-press |
| `ScreenIndexScreen.kt` | dev map: every screen one tap away; reached only via debug long-press on T31 diagnostics card (D53) | `ScreenIndexScreen`, `ScreenEntry`, `screenGroups` | AppNavGraph, T31 | navController.navigate(route) | never link from release UI |

### 3.2 `:core:common`

| File | Exports | Notes |
|---|---|---|
| `Money.kt` | `@JvmInline value class Money(val paise: Long)` with `+ -` ops, `formatted()` (₹, Indian grouping), `inWords()` ("Rupees … Only"), `inWordsLedger()` | the ONLY money type; Double/Float forbidden |
| `Weight.kt` | gram-based Long helpers (`formatIndianGrouping` lives here too: lakh/crore) | |
| `ErrorCode.kt` | 20 typed codes: `AUTH_EXPIRED, AUTH_NO_ACCESS, TENANT_MISMATCH, LEASE_EXHAUSTED, LEASE_INVALID, DUP_CLIENT_OP, CONSIGNMENT_IMMUTABLE, ALREADY_BILLED, TOPAY_UNCOLLECTED, POD_REQUIRED, CAPACITY_EXCEEDED, MASTER_IN_USE, TEMPLATE_VERSION_MISSING, TEMPLATE_FIELD_UNKNOWN, PHOTO_QUALITY, EXPORT_TOO_LARGE, BILL_MIXED_TREATMENT, TRIP_VEHICLE_BUSY, SYNC_RESYNC_REQUIRED, OFFLINE_UNAVAILABLE` | UI copy is centralised in `core/ui/ErrorCopy.kt` |
| `Result.kt` | sealed `Result<T>` = Success(value)/Failure(code, message?, cause?) + `failure()/success()` helpers + `isSuccess()/isFailure()` | every repo write returns this |
| `SeedIds.kt` | `SeedIds.COMPANY_SHIVSHAKTI/BRANCH_INDORE/…` constants used by DemoSeeder AND the demo preselect path | never reference in production logic except the S18 demo-party convenience |

### 3.3 `:core:database` — entities (33 tables)

Every entity carries the sync envelope unless noted: `local_id` PK (UUID), `server_id?`,
`updated_at_local`, `updated_at_server?`, `sync_state` (SYNCED/PENDING/CONFLICTED),
`deleted_at?` (tombstone), `company_id`. **Never hard-delete a synced row.**

| Entity (table) | Key columns beyond envelope | Notes |
|---|---|---|
| `CompanyEntity` (COMPANY_E) | name, legal_name, address, gstin, pan, transporter_id, gst_treatment, display_bilty_series, **logo_ref** (S22) | index on name |
| `BranchEntity` (BRANCH_E) | name, code, address?, is_head_office | FK → COMPANY_E CASCADE |
| `MembershipEntity` (MEMBERSHIP_E) | user_name (denormalised), user_email, role (5), branch_scope (ALL/branch id), status (ACTIVE/INVITED/DECLINED), invited_by, invited_expires_at, display_expires | index (company,user,email,status); unique (company,user) |
| `PartyEntity` (PARTY_E) | name, phone, email?, type (CONSIGNOR/CONSIGNEE/BOTH), street_address?, station?, pincode?, gstin?, usual_route_id?, usual_payment_mode?, display_bilty_count | S24 refresher upserts keyed by server_id |
| `StationEntity` (STATION_E) | name, state? | |
| `RouteEntity` (ROUTE_E) | origin_station_id, dest_station_id, distance_km, transit_days | |
| `GoodsEntity` (GOODS_E) | name | |
| `VehicleEntity` (VEHICLE_E) | number, type, capacity_g, ownership (OWN/ATTACHED/MARKET) | |
| `DriverEntity` (DRIVER_E) | name, licence, phone | |
| `BrokerEntity` (BROKER_E) | name, phone | |
| `ChargeHeadEntity` (CHARGE_HEAD_E) | code, label, basis (FLAT/PER_PACKAGE/PER_KG/PERCENT_OF_FREIGHT/PERCENT_OF_VALUE/PER_DAY), display_value?, default_value_paise, bearer, taxable, auto_apply, sort_order | drives §10.4 charge lines |
| `RateCardEntity` (RATE_CARD_E) | party_id?, route_id?, goods_id?, basis, rate_paise, min_qty_label?, min_freight_paise?, max_freight_paise?, note?, sort_order | 5-step resolution reads this |
| `CompanySettingEntity` (COMPANY_SETTING_E) | effective_from, gst_rate_bp, weight_step_g, volumetric_divisor_g?, gst_treatment, rounding, created_by_name | **dated** settings (§10.5) — booking reads the governing row |
| `NumberSeriesEntity` (NUMBER_SERIES_E) | branch_id, doc_type (BILTY/CHALLAN/FREIGHT_BILL/RECEIPT), prefix, fy_part, digits, last_issued, reset_rule | unique (company,branch,doc_type) |
| `NumberLeaseEntity` (NUMBER_LEASE_E) | series_id, device_id, range_start, range_end, next_value, expires_at | no-overlap guard in repo |
| `ConsignmentEntity` (CONSIGNMENT_E) | series_id, bilty_no?, provisional_no?, status_projection (derived!), booking_branch_id, dest_branch_id?, consignor_id, consignee_id, route_id, from/to_station_id, payment_mode, risk, delivery_type, place_of_supply_state?, eway_bill_no?, private_mark?, packages, actual_weight_g, chargeable_weight_g, declared_value_paise?, freight_paise, gst_paise, total_paise, booked_at, booked_by_name, expected_arrival, party_names, freight_bill_id? (partial unique: one live bill), amends_id?, amendment_reason? | the central aggregate |
| `ConsignmentItemEntity` | description, quantity, weight | multi-article (S15) |
| `ChargeLineEntity` | head_code, label, basis, input_value?, computed_paise, taxable, sort_order | per-consignment §10.4 lines |
| `StatusEventEntity` (STATUS_EVENT_E) | client_event_id (unique per company), event_type, location?, remark?, reason_code?, occurred_at, actor_name, photo_ref?, signature_ref? | append-only; source of status truth |
| `DocSnapshotEntity` (DOC_SNAPSHOT_E) | payload_json (full print payload), template_key, template_version, copy_count, html hash | reprint = re-render the snapshot |
| `AttachmentEntity` (ATTACHMENT_E) | kind, file_ref (relative app-files path), size_bytes, caption? | POD photos, case-file photos |
| `PodEntity` (POD_E) | consignee_name, signature_ref?, photo_ref?, pod_date, remarks? | satisfies the DELIVERED gate |
| `TripEntity` (TRIP_E) | challan_no?, vehicle_id, driver_id, origin_branch_id, dest_station_id, via_stations?, state (OPEN/ISSUED/DISPATCHED/CLOSED/CANCELLED), hire_paise, advance_paise, balance_paise, created_by_name, dispatched_at?, closed_at? | §11.1 |
| `TripLegEntity` (TRIP_LEG_E) | trip_id, consignment_id, leg_no, loaded_at | unique (trip,consignment) |
| `TripCostEntity` (TRIP_COST_E) | head, incurred_on, amount_paise, payment_mode, remark | §11 margin inputs |
| `LorryHireEntity` (LORRY_HIRE_E) | trip-level hire record | |
| `FreightBillEntity` (FREIGHT_BILL_E) | bill_no?, state (DRAFT/ISSUED/PARTIALLY_PAID/PAID), party_id, period_start/end, due_at?, freight/taxable/other/gst/total paise | partial unique: one consignment on one live bill |
| `ReceiptEntity` (RECEIPT_E) | series_id, receipt_no?, party_id, amount_paise, instrument, instrument_ref?, received_at, branch, received_by_name, notes? | |
| `ReceiptAllocationEntity` | receipt_id, bill_id, amount_paise | allocations ≤ receipt ≤ outstanding |
| `CreditNoteEntity` | party_id, amount, reason | creation flow = S20 leftover |
| `TemplateEntity` (TEMPLATE_E) | template_key, version, content_json (§9 JSON), status, is_default | versions-as-rows (S11) |
| `CompanySettingEntity` — see above | | |
| `PartyFtsEntity`/`ConsignmentFtsEntity` | FTS4 virtual tables maintained by triggers | LIKE search actually used (D7) |

Infra tables: `outbox` (+ `outbox_prereq` edges), `sync_cursor`, `seed_version`.

### 3.4 `:core:database` — DAOs (10 + 3 infra)

| DAO | Surface highlights |
|---|---|
| `OrgDao` | upsert/get Company/Branch/Membership, observeBranches/Memberships, getBranchesForCompany |
| `MastersDao` | CRUD + upserts for the 9 masters, `getRateCandidates(company, party?, route?, goods?)`, `updateRatePaise`, `getAutoChargeHeads`, FTS search for parties, `countX()` for the hub, duplicate queries |
| `NumberingDao` | upsertSeries/Lease, `getSeries(company,branch,docType)`, `getActiveLeases(seriesId, now)`, `getLeasesForSeries` |
| `ConsignmentDao` | `pagingRegister` (PagingSource, filter args), `summaryRegister`, get-by-bilty/provisional/local, upsert consignment/item/charge-line, `insertStatusEvent`, `getEventsOrdered`, snapshot get/upsert, attachment/pod, `countOverdue`, `getRecentHeldEvents`, FTS |
| `TripDao` | create/issue/dispatch/close support, `getTripByChallanNo`, `getOpenTripForVehicle`, `getLiveTrips`, `getLegRows`, `getCosts`, `getLegsFreightPaise` (S24 money), `observeBoard` |
| `BillingDao` | unbilled query, bill CRUD, `getBillWithParty`, `getBillConsignments`, issue, receipt CRUD, allocations, `getIssuedBillsForParty`, `getReceiptsForParty`, credit notes, `getPartyNameGstin`, outstanding totals |
| `DashboardDao` | ten parallel §13 queries (running trips, in transit, booked today, to-pay awaiting, unbilled, receivable, exception reasons, overdue buckets, idle vehicles, month money) |
| `ReportsDao` | freight register (+totals), outstanding, topay pending, lorry hire, number gaps, revenue by route/party/branch, sheet counts |
| `TemplateDao` | `getActiveTemplate(key)`, `getTemplateVersion(key, version)`, upsert |
| `SettingsDao` | dated company settings (`getGoverning`), picker lists (`routeOptions` join, party options) |
| `OutboxDao` | `upsertRow`, `upsertPrereqs`, `getReady(now, limit)` (prereq-ordered), `getPendingCount`, `observePendingCount`, `markDone(ids)`, `markRetriable(ids, nextAttemptAt, errorCode)` |
| `SyncCursorDao` / `SeedVersionDao` | cursors per family; seed version gate |

TypeConverters convert the four enums. `TransportDatabase` (v12) wires all DAOs +
`MIGRATION_1_2 … MIGRATION_11_12` (S22 added `COMPANY_E.logo_ref`).

### 3.5 `:core:datastore`

- `SessionStore.kt` — identity + JWT + SIGNED_OUT flag + `signIn(snapshot)`,
  `setActiveContext`, `updateDisplayName`, `saveToken(token)`, `token()`,
  `clear()`. Three-state machine (D54): signed-out flag → stored identity/company →
  debug-only DEMO fallback.
- `ActiveContextStore.kt` — active company/branch + sticky booking defaults.

### 3.6 `:core:designsystem`

Theme files (S20 tokens, D57):

- `Color.kt` — LightColors/DarkColors (green-cast, sunrise #E85D3D, shadowTint) + `PaperColors` (never inverts).
- `Type.kt` — Anek (display), Plex Sans (body), Plex Mono (data); `displayHeroMoney` 34sp; M3 mapping.
- `Shape.kt` — paper 2dp … contentCard 24dp, sheets 32dp, pill.
- `Dimens.kt` — 4dp grid, row ladder (40/48/56/72/88), app bars, chips, route line, tile sizes.
- `Motion.kt` — `HaulMotion` springs (bouncy/snappy/press) + easing tweens + `rememberReducedMotion()`.
- `Theme.kt` — `TransportAppTheme`, `LocalTransportColors` (haulAmber, delivered, sunrise, shadowTint, paperShadow, paper colors, stamps), never dynamic color.

Components (`component/`):

| File | Components |
|---|---|
| `AppBars.kt` | `TransportTopAppBar` (title, navigationIcon+Desc, trailingIcons), `NavDestination`, `TransportBottomNavBar` (80dp, pill), `StickyActionBar`, `TransportExtendedFab`, `OfflineBar`, `ErrorBanner` |
| `Buttons.kt` | `AppPrimaryButton` (press-scale + tinted glow + `celebrate` sunrise variant), `AppTonalButton`, `AppOutlinedButton`, `AppTextButton`, `AppDestructiveButton`, `AppListTextButton`, `Modifier.softGlow` |
| `Cards.kt` | `ContentCard` (`elevated` tinted-shadow variant), `NestedCard`, `PaperSheet` (warmed paper shadow) |
| `Chips.kt` | `JourneyChip`, `PaymentStamp` (spring-landing at −3°), `SyncChip`, `FilterChip`, `SegmentedControl`, `GroupHeading`, `FilterSheet` (S21 shared bottom sheet) |
| `DocketRow.kt` | `DocketRow` — 88dp, status rail, pressed fill, hero-ready |
| `Fields.kt` | `TransportTextField`, `SearchField`, `SummaryStrip`, `InfoRow`-style rows |
| `RouteLine.kt` | `RouteLine` (H/V; truck **placed** + drives + bobs — B1 closed), `CompactRouteLine` (animated, board) |
| `SignaturePad.kt` | canvas signature capture with `clearSignal`, `onPathChange` |
| `States.kt` | `LoadingBlock` (pulse), `LoadingList`, `EmptyState`, `EmptyStateIllustrated` (route-line motif), `ErrorState`, `InitialsAvatar` |

### 3.7 `:core:ui`

- `Routes.kt` — every route constant + `Uri.encode` helpers (`caseFile`, `statusSheet`, `challanDetail`, `freightBill`, `statement`, `masterList`, `masterEditor`, `rateCardEditor`, `reportViewer`, `legalDoc`); `SCREEN_INDEX` (debug-only, D53).
- `AppNavDrawer.kt` — `DrawerDestination` enum (8), `AppNavDrawer` (header + Work/Business/Admin groups).
- `NavTab.kt` — `navigateTab` (popUpTo(0)+saveState+singleTop+restoreState), `navigateDrawerDestination`.
- `ErrorCopy.kt` — all 20 ErrorCodes → plain copy.
- `PrintStatus.kt` — `Idle / Rendering(msg) / Error(msg)` shared by print flows.
- `sample/` — residual UiState row types (Party, RegisterRow, CaseEvent…) still used by feature screens; deleted per-screen samples live only in git history.

### 3.8 `:data:transport`

Repos (interface + impl per file, see Part 3 §9): SessionRepository, CompanyRepository,
MastersRepository, RateCardRepository, ConsignmentRepository, RegisterRepository,
CaseFileRepository, StatusRepository, NumberingRepository, TripRepository, BillingRepository,
DashboardRepository, DocumentRepository (+PdfPort/PdfActions/OperationalDocuments),
TemplateRepository, ReportsRepository, SettingsRepository, AccountDataRepository,
MastersRefresher (S24), OutboxPush (S25).

DI: `DataModule` (18 @Binds), `DatabaseModule`, `NetworkModule` (base URL 10.0.2.2:3000,
TokenProvider from SessionStore), `DeviceModule` (device short id), `DispatcherModule` +
`Qualifiers` (@IoDispatcher).

### 3.9 `:domain:transport`

- `PaymentMode.kt` — PAID/TOPAY/TBB with stampText.
- `RoleRank.kt` — ranked role gate `atLeast(role, minimum)`.
- `TransportEnums.kt` — `ConsignmentStatus` (11), `TripState` (5), `MembershipScope/Status`, `Role`, `PartyType`, `FreightBillStatus`.
- `calc/ChargeableWeight.kt` — max(actual, volumetric) with divisor + ceil, weight-step rounding.
- `calc/ChargeCalculator.kt` — `CalculationInput` (packages, weight, dims, divisor, rate, heads, removedHeadCodes, manualCharges, gst) → charges/taxable/gst/total; exact paise.
- `calc/RateResolver.kt` — 5-step cascade (party+route+goods → … → company default), `MinQty` grammar ("500 kg", "1.5 t"), no Double (S18 fix).
- `consignment/ConsignmentStateMachine.kt` — transition table (verified above).
- `trip/TripStateMachine.kt` — OPEN→ISSUED→DISPATCHED→CLOSED (+CANCELLED).
- `masters/MasterModels.kt` — PartyDetail, PartyListRow, RateRow, RateCardResolutionStep, MasterCounts…
- `org/OrgModels.kt` — CompanySummary, BranchSummary, MembershipSummary, RegisterCompanyRequest, MembershipScope/Status.
- `tracking/Ageing.kt` — overdue buckets + DEFAULT_GRACE_DAYS, DAY_MS.

### 3.10 `:export-engine`

- `BiltyRegisterRow.kt` — the CSV row model (headers + Indian-grouped cells).
- `CsvWriter.kt` — deterministic CRLF writer (UTC dates); consumed by ReportsRepository.

### 3.11 `:pdf-android`

- `AndroidPdfRenderer.kt` — headless WebView render loop → `PdfDocument` → bytes; 11-item checklist in comments (fonts, pagination, await layout); runs off-main.
- `PdfCallbackBridge.kt` — `android.print.PrintManager` adapter ("print" + "share" file hand-off).
- `CurrentActivity.kt` — registry the resumed activity registers into (D49) so repo-depth code can reach an Activity.

### 3.12 `:sync-android`

- `OutboxDrainWorker.kt` — @HiltWorker; `OutboxPush.drain(100)`; logs pushed/failed/pending; periodic 6 h + CONNECTED; manual trigger via T31.

### 3.13 Feature modules — file inventory

Every feature follows the same file grammar; listed with deltas only:

- `auth/`: SplashScreen(+VM), SignInScreen(+VM), CompanyPickerScreen(+VM), SetupWizardScreen(+VM+UiState w/ SetupField enum), CarouselScreen(+VM+UiState), ProfileScreen(+VM+UiState), LegalDocScreen (S21 static), navigation/AuthNavGraph.
- `dashboard/`: DashboardScreen(+VM+UiState with DashTile/DashException/isEmpty), navigation/DashboardNavGraph.
- `booking/`: BookingFormScreen(+VM 540 lines: draft persistence, manual charges, volumetric, multi-article, amend, submit) + BookingFormUiState + BiltyPreviewScreen(+UiState w/ BiltyPaperData) + navigation/BookingNavGraph.
- `consignment/`: RegisterScreen(+VM w/ Paging3, chips, debounce) , CaseFileScreen(+VM: print/share/cancel/photo), StatusUpdateSheet(+VM: legal continuations, POD gate, signature), ConsignmentNavGraph.
- `challan/`: ChallanBuilderScreen(+VM: pool, load meter, overload gate), ChallanDetailScreen(+VM: money card, add-cost, print/share renders), VehicleBoardScreen(+VM: drawer, filter sheet), ChallanNavGraph.
- `billing/`: UnbilledPoolScreen(+VM: party groups, build bill), FreightBillScreen(+VM: draft→preview→issue), PaymentsScreen(+VM: To Pay collect + waiver, receipts + allocation), StatementScreen(+VM), BillingNavGraph.
- `masters/`: MastersHubScreen(+VM), MasterListScreen(+VM: search, chips, duplicates merge), MasterEditorScreen(+VM: draft persistence), RateCardEditorScreen(+VM: resolution steps, add-rate), MastersNavGraph.
- `reports/`: ReportsHubScreen(+VM), ReportViewerScreen(+VM), ExportCentreScreen(+VM: sheet picker, pack builder, requestRegisterCsv), ReportsNavGraph.
- `settings/`: SettingsHubScreen(+VM), CompanyProfileScreen(+VM: drafts, logo), BranchesScreen(+VM: add-branch), MembersScreen(+VM: invite dialog, cancel), NumberingScreen(+VM: counter edit), AccountDataScreen(+VM: queue, sync-now), SettingsNavGraph, SettingsUiStates.
- `templates/`: TemplatesScreen(+VM: filter, install), TemplateRequestsScreen(+UiState), TemplatesNavGraph.

### 3.14 `:core:network`

- `ApiClient.kt` — the boundary (see Part 3 §11); `TokenProvider`; `OFFLINE_MESSAGE`.
- `AuthApi.kt` — login/devLogin → `AuthResponse`.
- `MastersApi.kt` — 6 list endpoints → `RemoteMaster`; `NumberingApi` — lease/peek.

### 3.15 `Backend/` (separate Next.js service, not part of the Android build)

Routes: auth (login, dev-login), org (companies/branches), masters CRUD ×9,
consignments (+cancel/status/pod), trips (+dispatch/close/costs), billing (unbilled/bills/
issue/receipts/allocate), numbering (list/lease/next), health. Business rules mirror the
app (state machines, atomic lease, billing invariants, paise). **No delta-feed, no
idempotency, no uploads** — those gate full §16.2 sync (see Part 4 §19).

---

## 4. Screen Documentation

All 34 production screens + the dev map. Conventions shared by every screen (stated once,
not repeated): MVVM-UDF shape per Spec §3; drawer on the three roots (Home/Register/
Vehicles — D53); `TransportAppTheme` throughout; errors as `ErrorBanner`/inline copy from
`ErrorCopy.kt`; loading as `LoadingBlock`/`LinearProgressIndicator`; empty as
`EmptyState`/`EmptyStateIllustrated`.

### T0 Splash — `feature.auth/screen/SplashScreen.kt` · route `splash`
- **Purpose:** session resolver; 4-step route-line animation (session → memberships →
  company → open) driven by `SplashViewModel.resolve()` which reads
  `sessionRepository.session.first()` at step 0 and sets `destination` =
  SIGN_IN (signed-out) or COMPANY_PICKER.
- **Nav:** `onResolved(destination)` → AuthNavGraph navigates SIGN_IN or COMPANY_PICKER
  with `popUpTo(SPLASH, inclusive)`.
- **States:** RESOLVING (RouteLine + steps), FORCED_UPDATE (buttons inert offline —
  documented), RESOLVE_FAILED (Continue offline → re-resolve).
- **Pitfall fixed (S18):** the per-step state update must `copy`, not replace — the old
  replace reset `destination` and sent signed-out users to the picker.

### T1 Sign in — `SignInScreen.kt` · route `sign_in`
- **Events:** `ContinueWithGoogle` (→ `SessionRepository.signIn()` — mock identity, D62),
  Terms/Privacy (→ `Routes.legalDoc(...)` static pages).
- **States:** loading on button; `signedIn` one-shot → nav to picker.
- **Offline:** no backend needed; sign-in always succeeds.

### T2 Company & branch picker — `CompanyPickerScreen.kt` · route `company_picker`
- Reads memberships/companies/branches from Room via `CompanyRepository.observe*`.
- Selection persists active context (`selectCompanyAndBranch`) → Dashboard.
- **Sign out** icon → `SessionRepository.signOut()` → fresh Splash (popUpTo(0)).
- Empty state: "Register a new company" + invitations (accept/decline → repo + outbox).
- S24: entry triggers `MastersRefresher.refreshAll()` in the background.

### T3 Setup wizard — `SetupWizardScreen.kt` · route `setup_wizard`
- 4 steps (Company/Tax/Branch/Vehicle) + done frame; every field writes through
  `SetupWizardEvent.EditField(SetupField, value)` (S18 — was dead).
- Finish → `SetupWizardEvent.Finish` → `companyRepository.registerCompany(...)` (one
  transaction: COMPANY + BRANCH + OWNER MEMBERSHIP + outbox prereqs) →
  `numberingRepository.ensureSeries(BILTY)` → done frame ("«company» is ready") →
  Dashboard. Validation: company name + branch code required; GSTIN/branch code uppercased.

### T32 Carousel — `CarouselScreen.kt` · route `carousel`
- Pager with three panels; SelectPage/Next events; GetStarted/Skip navigate via callbacks
  (dead VM events removed in S21).

### T33 Profile — `ProfileScreen.kt` · route `profile`
- Identity from session; Save → `updateDisplayName` (S21); sign-out; language/notifications
  toggles are UI-state (i18n deferred).

### T4 Dashboard — `DashboardScreen.kt` · route `dashboard` (drawer root, bottom-nav Home)
- **App bar:** hamburger → drawer; identity (initials/company/branch); Person → Settings.
- **Hero money strip** (S20): THIS MONTH delta chip + freight/hire/margin at dataLarge +
  `AnimatedSparkline` (draws itself).
- **Exception strip:** error cards, dismissible (`DismissException`), tap → case file
  (`onException(biltyNo)`).
- **Tiles:** 2-col grid, role-gated by `RoleRank` (hidden, not greyed); tap targets:
  Unbilled freight → T13, Vehicles idle → T12, Exceptions → T7.
- **Empty:** `EmptyStateIllustrated` "Nothing booked yet" → Book the first bilty.
- **States:** isLoading skeletons; pull data via `DashboardRepository.load` (10 parallel
  queries) + `statusRepository.exceptions`; offline: tiles render from Room, staleness
  stamp in UiState.
- **FAB:** "New bilty" → T5. **Bottom nav:** Home/Register/Vehicles via `navigateTab`.

### T5 Booking form — `BookingFormScreen.kt` · route `booking_form?amends={amends}`
The most important screen (§3). Fully documented behaviour:
- **Parties:** consignor/consignee cards start "Tap to add"; tap → search field with
  150 ms debounce → `searchPartiesOnce` (LIKE, benchmarked ≤120 ms @5k rows) → results
  select → party object into state; `Clear*` resets. Saved draft parties re-hydrate by id
  from masters (S19).
- **Route/goods:** inline picker sheets fed by `RateCardRepository.routeOptions/goodsOptions`.
- **Weight/volumetric:** ChangeWeight/ChangeLength|Breadth|Height →
  `ChargeCalculator.calculate` recomputes **per keystroke**; volumetric branch when dims
  present and divisor set (settings-driven); over-9,000 kg error copy.
- **Charges:** auto-apply heads from CHARGE_HEAD_E + clerk **Add charge** dialog (S21) →
  `ManualCharge(label, amount, taxable=true)` lines; RemoveCharge re-prices.
- **Payment/risk/delivery:** SegmentedControl; payment mode drives the stamp (which
  re-lands with a spring on change).
- **Sticky bar:** GRAND TOTAL + amount-in-words + **celebrate** "Book and print" (sunrise).
- **Reserved number:** peeked from the series (`peekNext`), never typed; provisional
  warning banner when on PROV numbers.
- **Amend mode:** `?amends=` prefill from the original's scope; reason required; original
  immutable.
- **Submit** → `ConsignmentRepository.book/amend`: validation → withTransaction { number
  lease (server-first, S24) → consignment + items + charge lines + status event + snapshot
  + outbox } → `bookedBiltyNo` one-shot → T6.

### T6 Bilty preview — `BiltyPreviewScreen.kt` · route `bilty_preview/{biltyNo}`
- 4-copy paper stack (white/pink/yellow/green), warmed paper shadows; RouteLine pager;
  `DocumentRepository.renderBilty` (pinned template version) → Print (spooler), Share
  (FileProvider sheet), Save & New, Done → case file. Copy menu (S21) jumps between copies.

### T7 Register — `RegisterScreen.kt` · route `register` (drawer root, bottom-nav Register)
- Paging 3 (`pagingRegister` with filter args) + day headers; summary strip
  (MATCHING/PACKAGES/FREIGHT); chips + search (150 ms debounce) + filter sheet (S21);
  **Export CSV** (S21) → `ReportsRepository.registerCsvForPeriod` + `buildCsvExport`.
- Docket rows carry status rails; tap → T8.

### T8 Case file — `CaseFileScreen.kt` · route `case_file/{biltyNo}`
- Summary strip; RouteLine timeline from the event log; money card; documents; actions:
  **Print** (reprint from snapshot, §9.12), **Share** (S21, same render → share sheet),
  Add photo (S19 Photo Picker → ATTACHMENT_E), Hold (T9), Amend (`booking_form?amends=`),
  Cancel (Manager dialog → repo.cancel), Raise bill (→ T13), Full history (→ T9).
- Refreshes on resume (`LifecycleResumeEffect`).

### T9 Status update sheet — `StatusUpdateSheet.kt` · route `status_sheet/{biltyNo}`
- Only §7.1-legal continuations listed; HOLD requires reason + remark ≥ 10 chars; DELIVERED
  requires consignee name + **signature** (SignaturePad PNG → files → `SaveWithSignature`);
  optional photo (S19 Photo Picker/TakePicture → importer). Append → projection advances.

### T10 Challan builder — `ChallanBuilderScreen.kt` · route `challan_builder`
- Loadable pool (Booked-here + At-hub, never on a live trip); multi-select with load meter
  toward vehicle capacity; **overload needs Manager override (§11.2)**; vehicle/driver
  auto-picked (first available — picker is a documented leftover); create → trip + issue →
  T11.

### T11 Challan detail — `ChallanDetailScreen.kt` · route `challan_detail/{challanNo}`
- Paper preview, loaded groups, **THE MONEY card** (S19: freight/hire/costs/**margin**),
  Add-cost dialog (head chips + mandatory remark → TRIP_COST_E), Dispatch/Close actions,
  **Print/Share** (S22 → `renderChallan` fixed-format A4 → spooler/share sheet).

### T12 Vehicle board — `VehicleBoardScreen.kt` · route `vehicle_board` (drawer root, bottom-nav Vehicles)
- Filter sheet mirrors chips (S21); summary (RUNNING/IDLE/LATE); vehicle cards with
  `CompactRouteLine` (truck rides the route, S20); FAB New challan.

### T13 Unbilled pool — `UnbilledPoolScreen.kt` · route `unbilled_pool`
- TBB consignments grouped by party with period/branch/age filters (sheet mirrors chips);
  select parties/consignments → **Build the bill** → T14 with the draft.

### T14 Freight bill — `FreightBillScreen.kt` · route `freight_bill/{billId}`
- Draft body (remove rows), preview, **Issue** (server-side in online tier; offline honest
  copy — numbers can't be reserved offline, §9/§12.1) → issued body with print/share
  (S22 render) + Receipt.

### T15 Payments — `PaymentsScreen.kt` · route `payments`
- Tab 1 To-Pay collections (Manager waiver on Held), Tab 2 bill receipts + explicit
  allocation (≤ receipt, ≤ outstanding); status flips ISSUED → PARTIALLY_PAID → PAID.

### T16 Statement — `StatementScreen.kt` · route `statement/{partyId}`
- FY-to-date ledger: opening, rows (bills debit / receipts credit), closing Dr/Cr, 90-day
  ageing callout; **Send statement as PDF** (S22 → `renderStatement` → share).

### T17–T20 Masters — hub (counts), list (search, chips, duplicates banner → merge),
editor (draft persistence S19, save/delete with MASTER_IN_USE guard), rate card editor
(resolution steps, editable rows, Add-rate dialog S21).

### T21–T23 Reports — hub (grouped by question, period), viewer (freight register table),
export centre (sheet picker → CSV pack zip → Recent exports; XLSX/Tally answer
OFFLINE_UNAVAILABLE).

### T24–T31 Settings — hub (identity + counts + wired rows, S16/S21), company profile
(drafts, logo S22), branches (add dialog S21), members (invite dialog S19, cancel S21),
numbering (counter change dialog §9, S19), templates (installed list), template requests
(queued-only), account & data (storage facts, real queue, Try-now drain, leave/delete
visual).

### Screen-index (dev) — `ScreenIndexScreen.kt` · route `screen_index`
Debug-only long-press entry from T31 (D53).

---

## 5–7. UI Components, Buttons, Cards (the design-system inventory)

### 5.1 Shared components (all in `:core:designsystem/component/`)

| Component | Purpose | Key API | Behaviour notes |
|---|---|---|---|
| `TransportTopAppBar` | 64dp bar | title, navigationIcon (+Desc S17), trailingIcons slot | used by every non-root screen; roots use Menu + drawer |
| `TransportBottomNavBar` | 80dp M3 bar, 3 roots | destinations, activeIndex, onSelect | pill indicator slides; only on T4/T7/T12 |
| `AppNavDrawer` | modal drawer w/ company header | header data, active, onSelect | Work/BUSINESS/ADMIN groups (D53) |
| `AppPrimaryButton` | 56dp pill CTA | text, icon, enabled, celebrate | spring press-scale (0.96) + tinted glow; sunrise when `celebrate` |
| `AppTonalButton/AppOutlinedButton/AppTextButton/AppDestructiveButton` | secondary CTAs | | |
| `ContentCard` | 24dp card | fill, border, padding, `elevated` | elevated = tinted shadow (money cards) |
| `NestedCard` | 12dp inner card | fill, border, onClick | |
| `PaperSheet` | 2dp paper card | fill, border | warmed paper shadow; document frames only |
| `TransportTextField` | all inputs | value, label, monospace, maxLines | |
| `SearchField` | register/masters search | value, placeholder | debounce lives in the VM |
| `FilterChip` / `SegmentedControl` | filters | label, selected, onClick | shape-morph spring |
| `JourneyChip` / `PaymentStamp` / `SyncChip` | status glyphs | | stamp springs on mode change |
| `DocketRow` | 88dp bilty row | doc/amount/route/consignee/status/payment/packages/exception | status rail + pressed fill; hero-ready |
| `RouteLine` / `CompactRouteLine` | the signature primitive | steps/stopCount/current | truck placed + drives + bobs (D58); draws travelled segment |
| `SignaturePad` | POD capture | clearSignal, onPathChange | exports PNG via canvas (paper-white/ink-black is intentional) |
| `LoadingBlock/LoadingList` | pulse skeletons | | no shimmer, no spinner |
| `EmptyState / EmptyStateIllustrated / ErrorState` | states | title/body/CTA | illustrated uses route-line motif |
| `InitialsAvatar` | 40/48/72dp | initials | |
| `SummaryStrip` | 3-figure bar | pairs | |
| `GroupHeading` | section eyebrow | title, trailing | |
| `FilterSheet` | shared filter bottom sheet (S21) | title, options slot | M3 ModalBottomSheet, 32dp |
| `OfflineBar` / `ErrorBanner` | connectivity/error strips | | |
| `TransportExtendedFab` | 56dp pill FAB | text, icon | |

### 6. Buttons — complete click chains (representative, all others analogous)

**"Book and print" (T5 sticky bar, celebrate variant)**
1. Tap → press-scale spring; `onBookAndPrint` → `viewModel.onEvent(BookingFormEvent.Submit)`
2. VM: validation (parties present, weight > 0, amend reason if amending) →
   `ConsignmentRepository.book/amend`
3. Repo: `NumberingRepository.issueNext` (S24: server lease → fallback local block grant
   → provisional) → `database.withTransaction { upsert consignment + items + charge lines +
   status event + doc snapshot + outbox row }`
4. Result: `bookedBiltyNo` one-shot → Screen `LaunchedEffect` → `onBooked(no)` → nav to T6.
5. Failure: typed code → `state.error` → ErrorCopy inline.

**"Sync now" (T31)**
Tap → `AccountDataEvent.TrySync` → VM: `OutboxPush.drain()` (ready rows → REST; DONE/
retriable) → `MastersRefresher.refreshAll()` → re-read queue → label "Sync now · N sent".

**"Book the bill" (T13)** → `UnbilledPoolEvent.BuildBill` → `BillingRepository.buildBill`
(draft with selected consignments; one-party-per-bill invariant) → `onBillBuilt(billId)` →
T14.

**"Issue this bill" (T14)** → `FreightBillEvent.Issue` →
`BillingRepository.issueBill` (assigns number — server-side in online tier; locks rows) →
stage ISSUED → print/share enabled (S22 renders).

**"Record cost" (T11 dialog)** → `ChallanDetailEvent.SaveCost` →
`TripRepository.addCost` (remark mandatory §11.4) → reload money card (margin updates).

**"Add branch" (T26 dialog)** → `BranchesEvent.SaveBranch` →
`SettingsRepository.addBranch` (Manager gate, duplicate refusal) → BRANCH_E + outbox.

**"Send invite" (T27 dialog)** → `MembersEvent.SendInvite` → `inviteMember` (Owner gate,
INVITED + 5-day expiry + outbox).

**"Set counter" (T28 dialog)** → `NumberingEvent.ConfirmCounter` →
`changeSeriesCounter` (Owner-only, forward-only, outbox UPDATE).

**"Save" (T25 profile)** → `CompanyProfileEvent.Save` → `saveCompanyProfile` (COMPANY_E +
COMPANY outbox) / logo picker → `saveLogo` (import + logo_ref + outbox).

**"Book the first bilty" (T4 empty state)** → `onNewBilty` → T5.

**Sign-out (T27 hub / T31)** → confirm → `SessionRepository.signOut()` → `store.clear()` →
rewind to Splash (popUpTo(0)) → resolver → sign-in.

### 7. Cards — data sources at a glance

| Card | Screen | Data path |
|---|---|---|
| Hero money strip | T4 | `DashboardRepository.load` → month figures (Room aggregates) |
| §13 tiles ×9 | T4 | same load; role-gated |
| Docket rows | T7 | `RegisterRepository.pagingRegister` (Paging 3) |
| Case-file timeline | T8 | `CaseFileRepository.caseFile` → event log |
| Money card | T11 | `TripRepository.tripDetail` (freight/hire/costs/margin) |
| Vehicle cards | T12 | `TripRepository.observeBoard` (Flow) |
| Unbilled party cards | T13 | `BillingRepository.unbilledPool` |
| Bill rows | T14 | `BillingRepository.observeBill` |
| Statement ledger | T16 | `BillingRepository.statement` |
| Rate rows | T20 | `MastersRepository.rateRowsForParty` |
| Bilty paper stack | T6 | `renderBilty` snapshot → HTML |
