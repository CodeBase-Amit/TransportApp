# Project Analysis: TransportApp2

This analysis provides a detailed overview of the current state of the TransportApp2 project, highlighting completed work, architectural strengths, and existing flaws or technical debt.

## 1. Executive Summary

The project is a native Android application for transport management, built with Kotlin, Jetpack Compose, and a strictly enforced multi-module MVVM/UDF architecture. The UI implementation phase (Phase 1) is largely complete, with all 34 screens defined in the design specification implemented as stateless Compose components. The project is currently transitioning through Phase 2, which involves integrating a Room-based offline data layer and Hilt dependency injection.

## 2. Work Done

### 2.1 UI & Theming (Sprint 0-7)
- **Design System**: A robust design system is implemented in `:core:designsystem`, featuring specialized palettes (Day Shift / Night Haul), a 14-step typography scale, and custom components (e.g., `JourneyChip`, `PaymentStamp`, `RouteLine`).
- **Screen Implementation**: All 34 screens (T0 to T33) have been implemented across feature modules. These screens follow the `Spec.md` contract:
    - **Stateless Content**: Composables render `UiState` and emit `Events`.
    - **ViewModels**: Pure Kotlin/Coroutine-based logic with `StateFlow`.
- **Material 3 Audit**: A consistency pass has been completed to ensure compliance with Material 3 and to remove unauthorized hardcoded colors or emojis (Sprint 7).

### 2.2 Architecture & Infrastructure
- **Module Isolation**: The project uses 20+ Gradle modules to enforce dependency boundaries. Feature modules are isolated and depend only on core/domain modules.
- **Core Common**: Critical domain types like `Money` (paise-based `Long`) and `Weight` (gram-based `Long`) are implemented to prevent precision issues, adhering to strict financial/operational rules.
- **Navigation**: A centralized `Routes.kt` and `AppNavHost` handle navigation, including `Uri` encoding for complex document numbers.

### 2.3 Domain & Data Layer (Phase 2 Progress)
- **Rate Resolution**: The core `RateResolver` and `MinQty` parsing logic are implemented in `:domain:transport`, following a 5-step resolution matrix.
- **Repositories**: Initial repository structures for `Company`, `Documents`, and `Masters` are present in `:data:transport`.
- **Mappers**: Entity-to-Domain mappers are being established to keep the domain layer clean of database concerns.

---

## 3. Project Flaws & Technical Debt

### 3.1 Documentation & Process Inconsistencies
- **Sprint Tracking**: `Sprints.md` contains contradictions. It marks Sprints 0, 1, 6, and 7 as "DONE," but leaves Sprints 2-5 unmarked, even though the summary states all 34 screens are finished. This creates ambiguity regarding the completion status of the Dashboard, Money, and Operations features.
- **Version Specs**: `Spec.md` refers to Kotlin 2.2.10 and AGP 9.3.2. These appear to be futuristic placeholders or typos, which could cause confusion for new developers or automated tools.

### 3.2 Architectural Violations
- **Platform Type Leakage**: Several repository interfaces in `:data:transport` (e.g., `DocumentRepository`, `PdfPort`) return `android.net.Uri`. This introduces a platform dependency into layers that should ideally remain platform-agnostic, potentially complicating future multi-platform efforts or unit testing.
- **Forbidden Pattern (Money/Weight)**: `RateResolver.kt` uses `toDoubleOrNull()` for parsing `MinQty`. While it rounds to `Long` immediately, `Spec.md §14.1` strictly forbids `Double`/`Float` for money or weight. The parsing logic should ideally use a more robust `BigDecimal` or string-based integer parsing to avoid any floating-point jitter.

### 3.3 Implementation Gaps
- **Test Coverage**: While the architecture is designed for testability, actual test coverage is sparse. Only a few unit tests exist (e.g., `MoneyInWordsTest`, `RoleRankTest`). Critical paths like the `RateResolver` and `BookingForm` validation lack comprehensive test suites.
- **Phase 2 Migration**: Many screens likely still depend on `SampleData` rather than real repositories. The "no-data-in-screens rule" (§5) is a high-priority goal that remains partially unverified across all 34 screens.
- **Error Handling**: `Spec.md §9` requires centralized error copy. While `ErrorCode` exists, the integration with `UiState` across all features is inconsistently applied, with many ViewModels still using placeholder or empty error states.

### 3.4 Code Quality
- **Placeholder Folder Names**: Some source directories use dotted names (e.g., `feature.auth`) instead of standard nested directory structures, which may conflict with certain IDE features or build tools.
- **Incomplete Logic**: Some repository implementations (e.g., `DocumentRepositoryTest`) contain overrides that return `null`, indicating unfinished infrastructure work.

---

## 4. Recommendations

1.  **Standardize Sprint Progress**: Synchronize `Sprints.md` with actual code status to provide a clear roadmap for Phase 2.
2.  **Refine Domain Boundaries**: Replace `android.net.Uri` in repository interfaces with a domain-specific `DocumentId` or `Path` string to fully decouple from Android.
3.  **Enhance Test Coverage**: Prioritize unit tests for `RateResolver`, `Money` calculations, and the `Outbox` synchronization logic.
4.  **Enforce Precision Rules**: Audit `MinQty.parse` to remove `Double` usage, ensuring all calculations start and end as `Long`.
5.  **Audit Version Catalog**: Correct the Kotlin and AGP versions in `Spec.md` to match the actual project configuration.
