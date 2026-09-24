# AGENTS.md — Toko Kasir Android POS

## 1. Project Identity

This repository contains an Android point-of-sale (POS) and basic inventory management application for a small family retail store.

Primary goals:
- Make cashier work fast and reliable.
- Centralize product prices so cashiers do not depend on remembered prices.
- Record completed transactions.
- Maintain auditable physical stock using a stock-movement ledger.
- Allow parents/admins to manage products, prices, stock, transaction history, and synchronization.
- Keep the application usable when the remote Google Sheets sync is unavailable.

The application is an operational retail system first. Avoid turning the MVP into an unnecessarily complex enterprise system.

## 2. Source of Truth

Before modifying code, read the relevant documents under `docs/`.

Document authority:
1. `docs/BUSINESS_RULES.md` — authoritative business rules.
2. `docs/ARCHITECTURE.md` — authoritative technical/data architecture.
3. `docs/PRD.md` — product requirements and scope.
4. `docs/UX_SPEC.md` — user flows and screen behavior.
5. `docs/IMPLEMENTATION_PLAN.md` — development sequence and milestones.

When documents conflict:
- Prefer the higher-authority document above.
- Do not silently invent a rule.
- Identify the conflict in the response and implement according to the higher-authority rule.
- Recommend a documentation correction when practical.

Do not treat previous chat messages as a replacement for repository documentation.

## 3. Product Scope

Supported product kinds:
- `PHYSICAL`
- `DIGITAL`

Barcode is an optional product attribute, not a product kind.

Physical products may be:
- unit-count products (`COUNT`), or
- weight-based products (`GRAM`).

Digital products are recorded as transactions but do not affect physical stock.

MVP payment methods:
- `CASH`
- `QRIS` (record/confirm payment only; no QRIS provider integration in MVP)

MVP intentionally does not integrate with the store's external digital-product provider. Record the result/reference only.

## 4. Non-Negotiable Business Rules

### Price
- Only ADMIN/PARENT users can change official product prices.
- Every price change must preserve price history.
- Customer-specific pricing and membership pricing are out of scope.
- Money uses integer Rupiah (`Long`/64-bit integer), never `Float` or `Double`.
- Existing cart lines keep their captured price. A later master-price change must not silently change an active cart.
- Historical transaction items always keep a unit-price snapshot.

### Cart and barcode
- Barcode scanning is the primary lookup path for barcoded products.
- Manual product search must always remain available as fallback.
- Repeated scans of the same product aggregate quantity instead of creating unnecessary duplicate cart lines, unless the UX specification explicitly requires a separate line.
- Unknown barcodes must offer `Daftarkan Produk Baru`.
- Unknown-barcode registration requires ADMIN authorization for the official product data/price.
- Registering an unknown barcode must preserve the active draft cart.
- After successful registration, return to the active cart and auto-add the newly registered product when technically possible.
- Scanner input must be debounced to avoid duplicate reads.

### Transactions
- Draft cart state is persistent in Room, not only in Compose memory.
- Transaction status: `DRAFT`, `COMPLETED`, `CANCELLED`.
- Payment status: `UNPAID`, `PAID`.
- Cash payment requires `amountReceived >= totalAmount`.
- Change is `amountReceived - totalAmount`.
- QRIS selection alone does not complete payment; cashier must confirm actual successful payment.
- Completing a transaction must atomically persist transaction data, transaction items, relevant stock sale movements, and sync-queue entries.
- Completed transactions must never be physically deleted.
- Cancelling a completed physical-product transaction requires ADMIN authorization, preserves the original transaction record, and creates `SALE_REVERSAL` stock movements.

### Inventory
- `StockMovement` is the source of truth for stock.
- Never use a directly editable `Product.stock` field as the operational source of truth.
- Current stock is derived from movements:
  `INITIAL_STOCK + STOCK_IN - SALE + SALE_REVERSAL ± ADJUSTMENT`.
- Internal movement between warehouse/storage and shelf is NOT recorded.
- Stock is represented in the base stock unit.
- Purchase-unit conversion is allowed and the conversion factor used at stock-in time must be preserved.
- Stock-in example: `4 DUS × 40 PCS/DUS = 160 PCS`.
- Stock adjustment uses `systemQuantity` and `physicalQuantity`; the app calculates `difference = physical - system`.
- Adjustment reason is mandatory.
- Physical-product sale must never make stock negative.

### Digital products
- Digital products do not create physical stock movements.
- A single transaction may contain physical and digital items.
- Digital transaction data is linked to its transaction item.
- Digital result status may be `SUCCESS`, `FAILED`, or `PENDING`.

### Remote sync
- Local Room DB is the operational source of truth for cashier continuity.
- Google Sheets + Apps Script is a temporary remote repository/backup/sync target, not the critical transaction database.
- Sync failure must not invalidate a locally completed transaction.
- Use stable UUIDs/idempotent identifiers to prevent duplicate remote records on retry.
- Prefer batched synchronization over one network request per entity.
- WorkManager is used for background sync.
- Sync operations should be safe to retry.
- Keep business logic in the domain/application layer, not Apps Script.
- Future migration to REST API/PostgreSQL must remain practical.

## 5. Roles and Authorization

Roles:
- `ADMIN`
- `CASHIER`

CASHIER may:
- create transactions
- scan/search products
- modify the active cart
- process cash payment
- confirm QRIS payment after verification

CASHIER may NOT:
- change master prices
- arbitrarily set official price on a newly registered product
- edit stock directly
- perform admin-only inventory adjustments
- cancel completed transactions unless an explicit business rule later permits it

ADMIN/PARENT may:
- manage products/categories
- change prices
- initialize stock
- stock in
- perform stock opname/adjustments
- review transaction history
- cancel completed transactions
- manage sync/recovery functions as specified

Authorization must be enforced in the domain/use-case layer, not only by hiding UI buttons.

## 6. Technical Stack Direction

Primary stack:
- Kotlin
- Android
- Jetpack Compose
- MVVM + Clean Architecture principles
- Repository Pattern
- Room/SQLite
- Hilt
- CameraX
- ML Kit Barcode Scanning
- WorkManager
- Google Apps Script gateway
- Google Sheets temporary remote repository

For library versions, verify current official Android/Google documentation when implementing or upgrading. Do not blindly copy stale versions from old project notes.

MVP starts as a single Gradle `app` module unless a concrete need justifies multi-module architecture.

## 7. Architecture Rules

Recommended package direction:

```text
com.harmony.tokoharmony
├── core
│   ├── common
│   ├── database
│   │   ├── entity
│   │   └── dao
│   ├── network
│   │   ├── api
│   │   └── dto
│   ├── sync
│   ├── security
│   └── di
├── data
│   ├── local
│   ├── remote
│   └── repository
├── domain
│   ├── model
│   ├── repository
│   └── usecase
├── feature
│   ├── home
│   ├── auth
│   ├── cashier
│   │   ├── transaction
│   │   ├── scanner
│   │   ├── search
│   │   ├── cart
│   │   └── payment
│   └── admin
│       ├── dashboard
│       ├── product
│       ├── price
│       ├── inventory
│       ├── transaction
│       └── sync
└── navigation
```

Rules:
- ViewModels must not call DAOs directly.
- UI must not own critical business state that must survive process/navigation changes.
- Repository interfaces belong to the domain layer where appropriate.
- Use cases own business validation/authorization.
- Database entities must not leak unnecessarily into UI.
- Prefer immutable UI state.
- Use explicit result/error models for recoverable business errors.
- Keep calculation rules deterministic and testable.

## 8. Data Modeling Rules

Important core entities:
- Category
- Product
- PriceHistory
- Transaction
- TransactionItem
- StockMovement
- StockIn
- StockAdjustment
- DigitalTransaction
- User
- SyncQueue

Use UUID/string IDs for entity identity where the architecture specifies them.

For products:
- `productKind`: `PHYSICAL | DIGITAL`
- `pricingMethod`: `PER_UNIT | PER_KG`
- `quantityType`: `COUNT | GRAM`
- `currentPrice`: integer Rupiah
- `isActive`: soft-delete/inactive semantics

For weighted products:
- Store quantity in integer grams.
- Do not use floating-point weight for persisted business calculations.
- Weight selection for MVP uses 0.5 kg increments.
- Weighted subtotal:
  `(grams * pricePerKg) / 1000`

For unit products:
- Quantity is integer count.

Never change a historical transaction by recalculating it from the current Product price.

## 9. Development Workflow

For every feature/bug/task:

1. Inspect the current repository state before editing.
2. Read the relevant docs and existing implementation.
3. Identify affected layers, entities, use cases, screens, and migrations.
4. State assumptions when a required requirement is missing.
5. Make the smallest coherent change that satisfies the requirement.
6. Do not rewrite unrelated code.
7. Update Room migrations whenever schema changes.
8. Add or update automated tests for business-critical behavior.
9. Run build/tests/lint or the most relevant available verification commands.
10. Inspect the resulting diff and summarize what changed, what was verified, and any remaining risks.

Do not implement future phases merely because they are documented. Work only on the requested milestone unless explicitly asked to continue.

## 10. Testing Priorities

Business-critical tests should cover at minimum:
- price snapshot behavior
- price history creation
- cart aggregation
- weighted quantity/subtotal calculation
- cash change calculation
- invalid cash payment rejection
- QRIS confirmation semantics
- stock movement calculations
- negative stock prevention
- stock-in conversion
- stock adjustment difference calculation
- sale reversal on cancellation
- digital transaction not affecting physical stock
- sync idempotency/retry behavior
- draft cart persistence
- authorization boundaries

Use unit tests for pure domain logic and DAO/integration tests for persistence behavior. Add UI tests where behavior cannot be meaningfully validated at lower layers.

## 11. UI/UX Guardrails

- Preserve established screen structure and interaction decisions unless the task explicitly changes UX.
- Do not redesign the app for aesthetic reasons during functional work.
- Cashier flows should minimize taps and cognitive load.
- Destructive/administrative actions need clear confirmation.
- Preserve the active cart during scanner/search/registration/admin temporary navigation.
- Use clear Indonesian labels consistent with `docs/UX_SPEC.md`.
- Do not hide important operational status behind non-obvious interactions.

## 12. Security and Data Safety

- Never hard-code production secrets, credentials, API keys, or PINs.
- Never log authentication secrets or sensitive payment/customer data.
- Store PINs as secure hashes, never plaintext.
- Avoid logging full transaction/customer payloads unless explicitly needed for development.
- Treat sync payloads as data crossing a trust boundary.
- Do not weaken authorization just to make a UI flow work.

## 13. Change Management

When changing a business rule or architecture:
- Update the relevant `docs/` file first or together with the implementation.
- Explain the impact on data/schema/UX if applicable.
- Add migration/backward-compatibility handling where required.
- Do not preserve an obsolete implementation merely because it already exists when it conflicts with the authoritative docs.

## 14. Agent Behavior

Act as an implementation partner, not an autonomous product manager.

Good behavior:
- inspect first
- reason from repository evidence
- implement incrementally
- verify changes
- surface contradictions
- minimize unnecessary edits
- preserve established business rules

Bad behavior:
- inventing business requirements
- silently changing architecture
- building the whole roadmap from one prompt
- adding libraries without justification
- replacing working code with abstractions that do not solve a current need
- treating Google Sheets as the critical transaction database
- directly editing stock numbers
- deleting historical business records
- using floating point for money

When a task is ambiguous but safe to proceed, choose the smallest behavior consistent with the docs and clearly record the assumption. Ask only when the ambiguity materially changes business behavior or data integrity.
