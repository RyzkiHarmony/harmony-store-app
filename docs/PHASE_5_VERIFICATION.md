# PHASE 5 VERIFICATION — Physical Inventory Management & Transaction Cancellation

## 1. Overview & Objectives

Phase 5 completes the physical inventory management system built strictly around the **StockMovement ledger** as the sole source of truth for inventory.

### Key Milestones Achieved:
1. **Initial Stock Setup**: Admin-only setup of product stock baselines (`INITIAL_STOCK`). Zero initial stock allowed, digital products rejected, baseline duplication prevented.
2. **Barang Masuk (Stock In)**: Admin-only recording of incoming purchases with purchase quantity, purchase unit, conversion factor, converted stock quantity, and immutable history in `stock_ins` + `STOCK_IN` movement.
3. **Stock Opname (Stock Adjustment)**: System stock loaded, physical count entered, difference calculated automatically (`physical - system`), mandatory adjustment reason selected, saved to `stock_adjustments` + `ADJUSTMENT` movement.
4. **Transaction Cancellation & Sale Reversal**: Admin-only cancellation of `COMPLETED` transactions, updating status to `CANCELLED`, recording cancellation reason/metadata, preserving historical transaction records & prices, and creating `SALE_REVERSAL` stock movements (+quantity) for physical products only (digital products create no physical stock movements).
5. **Stock Ledger Integrity**: Current stock is strictly derived from:
   $$\text{Current Stock} = \sum(\text{quantity\_delta}) = \text{INITIAL\_STOCK} + \text{STOCK\_IN} - \text{SALE} + \text{SALE\_REVERSAL} \pm \text{ADJUSTMENT}$$
   No mutable `Product.stock` field exists.
6. **Domain & Use-Case Authorization**: Strict role checking in use cases (`user.role == Role.ADMIN`), rejecting `CASHIER` attempts.

---

## 2. Database Schema Changes & Migration

### Room Version: 4 → 5

#### Tables Created:
1. `stock_ins`
   - `stock_in_id` TEXT PRIMARY KEY
   - `product_id` TEXT NOT NULL (FK -> `products.product_id` RESTRICT)
   - `purchase_quantity` INTEGER NOT NULL
   - `purchase_unit` TEXT NOT NULL
   - `conversion_factor` INTEGER NOT NULL
   - `stock_quantity` INTEGER NOT NULL
   - `note` TEXT NULL
   - `created_at` INTEGER NOT NULL
   - `created_by` TEXT NOT NULL (FK -> `users.user_id` RESTRICT)
   - Indices: `product_id`, `created_by`, `created_at`

2. `stock_adjustments`
   - `adjustment_id` TEXT PRIMARY KEY
   - `product_id` TEXT NOT NULL (FK -> `products.product_id` RESTRICT)
   - `system_quantity` INTEGER NOT NULL
   - `physical_quantity` INTEGER NOT NULL
   - `difference` INTEGER NOT NULL
   - `reason` TEXT NOT NULL
   - `note` TEXT NULL
   - `created_at` INTEGER NOT NULL
   - `created_by` TEXT NOT NULL (FK -> `users.user_id` RESTRICT)
   - Indices: `product_id`, `created_by`, `created_at`

#### Migration Implemented:
- `MIGRATION_4_5` registered in [AppDatabase.kt](file:///d:/AndroidStudio%20Data/TokoHarmony/app/src/main/java/com/harmony/tokoharmony/core/database/AppDatabase.kt) and [DatabaseModule.kt](file:///d:/AndroidStudio%20Data/TokoHarmony/app/src/main/java/com/harmony/tokoharmony/core/di/DatabaseModule.kt).
- Migration test [Migration4To5Test.kt](file:///d:/AndroidStudio%20Data/TokoHarmony/app/src/test/java/com/harmony/tokoharmony/core/database/Migration4To5Test.kt) validates non-destructive upgrade, schema accuracy, index creation, and foreign key retention.

---

## 3. Inventory Flows & Behaviors

### A. Initial Stock Setup
- **Screen**: `InitialStockSetupScreen`
- **Flow**: Admin selects physical product without initialized baseline, enters integer count / grams (kelipatan 500g for gram products), and commits.
- **Ledger Effect**: Creates `StockMovement(movementType = INITIAL_STOCK, quantityDelta = initialStock)`.
- **Validation**: Zero allowed; negative rejected; digital products rejected; duplicate initialization prevented.

### B. Barang Masuk (Stock In)
- **Screen**: `StockInScreen`
- **Flow**: Admin selects product, specifies purchase quantity (e.g. 4), purchase unit (e.g. "Dus"), conversion factor (e.g. 40), and optional note.
- **Conversion**: $4 \text{ Dus} \times 40 = 160 \text{ pcs}$.
- **Ledger Effect**: Atomically inserts `StockInEntity` and `StockMovement(movementType = STOCK_IN, quantityDelta = +160, referenceType = STOCK_IN)`.

### C. Stock Opname / Penyesuaian
- **Screen**: `StockOpnameScreen`
- **Flow**: Admin selects physical product. System displays current ledger stock (e.g. 157). Admin inputs physical count (e.g. 155). System calculates $\Delta = 155 - 157 = -2$. Admin selects mandatory reason (`Barang rusak`, `Barang hilang`, `Kedaluwarsa`, `Kesalahan pencatatan`, `Stock opname`, `Lainnya`) and submits.
- **Ledger Effect**: Atomically inserts `StockAdjustmentEntity` and `StockMovement(movementType = ADJUSTMENT, quantityDelta = -2, referenceType = STOCK_ADJUSTMENT)`.

### D. Transaction Cancellation & Sale Reversal
- **Screens**: `AdminTransactionHistoryScreen`, `AdminTransactionDetailScreen`
- **Flow**: Admin views completed transactions, opens detail, clicks `Batalkan Transaksi`, enters cancellation reason/note in confirmation dialog.
- **Safety**:
  - Only `COMPLETED` transactions can be cancelled (`DRAFT` cannot be cancelled, `CANCELLED` is idempotent/rejected).
  - Transaction record and item rows remain stored permanently (never deleted).
  - Price snapshots remain unaltered.
  - Transaction status updated to `CANCELLED` with `cancelled_at`, `cancelled_by`, and `cancellation_reason`.
  - For each physical product item, creates `StockMovement(movementType = SALE_REVERSAL, quantityDelta = +quantity, referenceType = TRANSACTION)`.
  - Digital items create no physical stock movements.

---

## 4. Verification & Testing

### A. Unit Tests
All unit tests passed cleanly (`./gradlew testDebugUnitTest`):
- `Migration4To5Test`: Database schema upgrade 4 → 5 validation.
- `InitialStockUseCaseTest`: Admin authorization, zero stock, negative rejection, digital rejection, duplicate prevention.
- `StockInUseCaseTest`: Admin authorization, purchase conversion calculations, invalid quantity/factor validation, digital rejection.
- `StockOpnameUseCaseTest`: System stock calculation, difference calculation (+, -, 0), mandatory reason, negative rejection.
- `CancelTransactionUseCaseTest`: Reversal mechanics, stock restoration, digital isolation, status transitions, admin authorization.
- `StockLedgerCalculationTest`: Verification of complete stock formula:
  $$\text{INITIAL} + \text{IN} - \text{SALE} + \text{REVERSAL} \pm \text{ADJUSTMENT}$$
  and multi-product ledger isolation.
- Total Unit Test Suites: 22 / 22 Passed.

### B. On-Device Instrumentation Tests
Connected Android instrumented test ran directly on physical device:
- **Device**: realme 9 Pro+ (`RMX3393`), Android 14 (API 34)
- **Test Class**: `Phase5InventoryInstrumentedTest`
- **Results**:
  - Initial stock creation: PASSED
  - Stock in conversion & ledger delta: PASSED
  - Cashier sale & stock deduction: PASSED
  - Stock opname difference calculation: PASSED
  - Transaction cancellation & physical stock reversal: PASSED
  - Authoritative ledger total verification: PASSED ($12 + 18 - 4 - 1 + 4 = 29$)
  - Result: 2/2 tests PASSED on `RMX3393 - 14`.

### C. Build Verification
- `./gradlew assembleDebug`: **BUILD SUCCESSFUL**

---

## 5. Known Limitations & Phase 6 Boundaries

1. **Google Sheets Sync**: SyncQueue entries and background syncing with Google Apps Script are deferred to Phase 6.
2. **WorkManager**: Background synchronization workers are not yet implemented (Phase 6 scope).
3. **Multi-device Sync**: Single-device local database is operational source of truth.
4. **Hardware Receipts / Digital Provider**: Out of scope for MVP as documented in PRD & Business Rules.

---

## 6. Readiness Status

**PHASE 5 IS COMPLETE AND READY FOR PHASE 6.**
