# PHASE 4 VERIFICATION & ATOMICITY AUDIT REPORT

**Date:** 2026-09-24  
**Project:** Toko Kasir Android POS (`TokoHarmony`)  
**Scope:** Phase 4 — Payment Flow, Stock Movements, & Atomic Transaction Completion  
**Status:** **PASSED / READY FOR PHASE 5**

---

## 1. Executive Summary

Phase 4 implements the payment and atomic transaction-completion flow:
$$\text{DRAFT} \longrightarrow \text{PAYMENT (CASH / QRIS)} \longrightarrow \text{PAID} \longrightarrow \text{COMPLETED} \longrightarrow \text{SALE STOCK MOVEMENTS} \longrightarrow \text{SUCCESS SCREEN}$$

All non-negotiable business rules and architectural invariants were strictly implemented and verified:
- **Atomic Transaction Completion:** Encapsulated in `RoomDatabase.withTransaction { ... }`. Prevents any partial state where a transaction is marked `COMPLETED` without corresponding `SALE` stock movements, or stock is decremented without a completed transaction.
- **Strict Integer Rupiah Calculations:** All monetary figures (`totalAmount`, `amountReceived`, `changeAmount`, `subtotal`, `unitPrice`) use 64-bit `Long` integers. Never uses `Float` or `Double`.
- **Cash Payment Rules:** Enforces `amountReceived >= totalAmount`. Calculates exact change `amountReceived - totalAmount`. Rejects underpayment and displays the exact shortage.
- **Manual QRIS Confirmation Flow:** Selecting QRIS maintains `UNPAID` state until the cashier manually taps `"PEMBAYARAN BERHASIL"`. No external QRIS API calls are made. Back navigation preserves the active `DRAFT` cart intact.
- **Distinct Status Enums:** Decouples `TransactionStatus` (`DRAFT`, `COMPLETED`, `CANCELLED`) and `PaymentStatus` (`UNPAID`, `PAID`).
- **Physical Stock Ledger (`StockMovement`):** Generates negative `SALE` stock movements for `PHYSICAL` products only. `DIGITAL` products record zero physical stock movements.
- **Negative Stock Prevention:** Queries the movement ledger (`sum(quantityDelta)`) before completing physical sales. If physical stock is insufficient, completion is rejected with an explicit error, the transaction remains `DRAFT`, and zero stock movements are committed.
- **Authoritative Total Recalculation:** Re-reads items directly from the database and recalculates subtotals (`COUNT`: `quantity * unitPrice`, `GRAM`: `(grams * pricePerKg) / 1000`) before final persistence.
- **Price Snapshot Preservation:** Existing cart items retain their captured `unitPrice` regardless of subsequent master price updates.
- **Single-Active-Draft Invariant:** Once completed, the previous transaction is permanently detached from the active draft session.

---

## 2. Environment & Devices

| Component | Specification / Value |
| :--- | :--- |
| **Connected Physical Device** | `realme 9 Pro+ / RMX3393` (Android 14 / API 34), Device ID: `Q4XGHI5H55CUEMWS` |
| **Android Build SDK** | Compile SDK: 34, Min SDK: 26, Target SDK: 34 |
| **Kotlin / Compose** | Kotlin 1.9.24, Compose BOM 2024.04.01, Material3 |
| **Local Database** | Room 2.6.1 (SQLite Schema V4 with `MIGRATION_3_4`) |
| **Test Engine** | JUnit 4 + Kotlinx Coroutines Test + Room SQLite In-Memory Database |

---

## 3. Files Created & Modified

### 3.1 Files Created
- **Database & Entities:**
  - `app/src/main/java/com/harmony/tokoharmony/core/database/entity/StockMovementEntity.kt` — Room entity for `stock_movements` table with foreign key indices.
  - `app/src/main/java/com/harmony/tokoharmony/core/database/dao/StockMovementDao.kt` — DAO for inserting stock movements and calculating current stock balance via `SUM(quantity_delta)`.
- **Domain Models & Repositories:**
  - `app/src/main/java/com/harmony/tokoharmony/domain/model/StockMovement.kt` — Domain model for stock movement records.
  - `app/src/main/java/com/harmony/tokoharmony/domain/model/StockMovementEnums.kt` — Enum definitions for `StockMovementType` (`INITIAL_STOCK`, `STOCK_IN`, `SALE`, `SALE_REVERSAL`, `ADJUSTMENT_IN`, `ADJUSTMENT_OUT`, `TRANSFER_IN`, `TRANSFER_OUT`) and `StockMovementReferenceType` (`TRANSACTION`, `STOCK_IN`, `STOCK_ADJUSTMENT`, `INITIALIZATION`, `MANUAL`).
  - `app/src/main/java/com/harmony/tokoharmony/data/local/mapper/StockMovementMapper.kt` — Entity <-> Domain mappers for stock movements.
  - `app/src/main/java/com/harmony/tokoharmony/domain/repository/StockRepository.kt` — Repository interface for stock operations and stock balance lookups.
  - `app/src/main/java/com/harmony/tokoharmony/data/repository/StockRepositoryImpl.kt` — Implementation of `StockRepository`.
- **Use Cases:**
  - `app/src/main/java/com/harmony/tokoharmony/domain/usecase/payment/CalculateCashChangeUseCase.kt` — Validates cash received, returns change or shortage.
  - `app/src/main/java/com/harmony/tokoharmony/domain/usecase/payment/CompleteCashTransactionUseCase.kt` — Use case for completing cash payments.
  - `app/src/main/java/com/harmony/tokoharmony/domain/usecase/payment/CompleteQrisTransactionUseCase.kt` — Use case for completing confirmed QRIS transactions.
  - `app/src/main/java/com/harmony/tokoharmony/domain/usecase/transaction/GetTransactionByIdUseCase.kt` — Lookup transaction by UUID.
  - `app/src/main/java/com/harmony/tokoharmony/domain/usecase/inventory/GetCurrentStockUseCase.kt` — Query current stock for a product from the movement ledger.
- **Presentation & UI:**
  - `app/src/main/java/com/harmony/tokoharmony/feature/cashier/payment/CashierPaymentUiState.kt` — UI state and event definitions for checkout/payment screen.
  - `app/src/main/java/com/harmony/tokoharmony/feature/cashier/payment/CashierPaymentViewModel.kt` — ViewModel handling cash amount input, quick denomination chips, change calculation, and payment execution.
  - `app/src/main/java/com/harmony/tokoharmony/feature/cashier/payment/CashierPaymentScreen.kt` — Jetpack Compose checkout screen supporting Cash and QRIS modes.
  - `app/src/main/java/com/harmony/tokoharmony/feature/cashier/success/TransactionSuccessUiState.kt` — UI state for completion screen.
  - `app/src/main/java/com/harmony/tokoharmony/feature/cashier/success/TransactionSuccessViewModel.kt` — ViewModel loading completed transaction details.
  - `app/src/main/java/com/harmony/tokoharmony/feature/cashier/success/TransactionSuccessScreen.kt` — Jetpack Compose success screen showing transaction summary, change, and "TRANSAKSI BARU" action.
- **Tests:**
  - `app/src/test/java/com/harmony/tokoharmony/core/database/Migration3To4Test.kt` — Verifies schema upgrade from V3 to V4.
  - `app/src/test/java/com/harmony/tokoharmony/domain/usecase/CompleteTransactionAtomicityTest.kt` — 11 comprehensive automated tests covering all 16 atomicity and business rule scenarios.
  - `app/src/test/java/com/harmony/tokoharmony/feature/cashier/CashierPaymentViewModelTest.kt` — Verifies payment ViewModel state management and change computation.

### 3.2 Files Modified
- `app/src/main/java/com/harmony/tokoharmony/core/database/AppDatabase.kt` — Incremented database version to 4, added `StockMovementEntity`, registered `stockMovementDao()`, and defined `MIGRATION_3_4`.
- `app/src/main/java/com/harmony/tokoharmony/core/di/DatabaseModule.kt` — Added `MIGRATION_3_4` to database builder and provided `StockMovementDao`.
- `app/src/main/java/com/harmony/tokoharmony/core/di/RepositoryModule.kt` — Bound `StockRepositoryImpl` to `StockRepository`.
- `app/src/main/java/com/harmony/tokoharmony/domain/repository/TransactionRepository.kt` — Added `completeCashTransaction` and `completeQrisTransaction` contracts.
- `app/src/main/java/com/harmony/tokoharmony/data/repository/TransactionRepositoryImpl.kt` — Implemented atomic transaction completion with `RoomDatabase.withTransaction`, stock validation, subtotal recalculation, and stock movement generation.
- `app/src/main/java/com/harmony/tokoharmony/feature/cashier/cart/CashierCartScreen.kt` — Added `[ PROSES PEMBAYARAN ]` button navigating to `CashierPayment` route.
- `app/src/main/java/com/harmony/tokoharmony/navigation/Screen.kt` — Added `CashierPayment` and `TransactionSuccess` route definitions.
- `app/src/main/java/com/harmony/tokoharmony/navigation/AppNavigation.kt` — Registered navigation destinations for checkout and success screens.
- `app/src/test/java/com/harmony/tokoharmony/domain/usecase/DraftCartUseCaseTest.kt` — Updated fake repository to conform to the extended `TransactionRepository` interface.

---

## 4. Database Schema Changes & Migration Details

### 4.1 Schema Version
- **Old Version:** `3`
- **New Version:** `4`
- **Schema Export File:** `app/schemas/com.harmony.tokoharmony.core.database.AppDatabase/4.json`

### 4.2 New Table: `stock_movements`
```sql
CREATE TABLE IF NOT EXISTS `stock_movements` (
    `movement_id` TEXT NOT NULL,
    `product_id` TEXT NOT NULL,
    `movement_type` TEXT NOT NULL,
    `quantity_delta` INTEGER NOT NULL,
    `balance_after` INTEGER NOT NULL,
    `reference_id` TEXT,
    `reference_type` TEXT NOT NULL,
    `notes` TEXT,
    `created_at` INTEGER NOT NULL,
    `created_by` TEXT NOT NULL,
    PRIMARY KEY(`movement_id`),
    FOREIGN KEY(`product_id`) REFERENCES `products`(`product_id`) ON UPDATE NO ACTION ON DELETE RESTRICT
);
CREATE INDEX IF NOT EXISTS `index_stock_movements_product_id` ON `stock_movements` (`product_id`);
CREATE INDEX IF NOT EXISTS `index_stock_movements_reference_id` ON `stock_movements` (`reference_id`);
CREATE INDEX IF NOT EXISTS `index_stock_movements_created_at` ON `stock_movements` (`created_at`);
```

### 4.3 Migration Verification
- `MIGRATION_3_4` executed without data loss or table recreation.
- Verified by automated unit test `Migration3To4Test` against SQLite database instance.

---

## 5. Implementation Verification

### 5.1 Atomicity & Transaction Invariants
Every completion execution in `TransactionRepositoryImpl` runs within `appDatabase.withTransaction`:
1. Validates that the transaction exists and has status `DRAFT`.
2. Validates that the draft cart has at least 1 item.
3. Validates that all referenced products still exist and have `isActive == true`.
4. Re-reads all items from database and recalculates subtotals deterministically:
   - For `COUNT`: `quantity * unitPrice`
   - For `GRAM`: `(grams * pricePerKg) / 1000`
5. Validates physical stock balance for each `PHYSICAL` item:
   $$\text{Current Stock} = \sum \text{quantity\_delta} \ge \text{Requested Quantity}$$
   If insufficient, throws validation error and triggers database rollback.
6. Validates payment requirements (`amountReceived >= totalAmount` for CASH; confirmed payment for QRIS).
7. Generates a stable human-readable transaction number (`TRX-YYYYMMDD-HHMMSS-XXXX`) if not already generated.
8. Writes `StockMovement` records with `movementType = SALE` and `quantityDelta = -quantity` for physical products.
9. Updates `TransactionEntity` with `transactionStatus = COMPLETED`, `paymentStatus = PAID`, `completedAt = timestamp`.
10. Releases the active draft session so a new `DRAFT` transaction will be initialized upon the next cashier entry.

### 5.2 Cash Payment Flow
- Displays total amount, quick denomination selection chips (Uang Pas, 10k/20k/50k/100k/200k denominations, next round-up 10k/50k).
- Real-time calculation of change or shortage banner.
- Rejecting checkout when `amountReceived < totalAmount`.

### 5.3 QRIS Payment Flow
- Clear visual indicator for QRIS checkout.
- Cashier prompt to inspect incoming payment on store account before confirmation.
- Completion button labeled `"PEMBAYARAN BERHASIL"`.
- Back action returns to draft cart with zero stock movements recorded.

---

## 6. Automated Test Results

### 6.1 Summary of Unit Test Suites
Executing `./gradlew.bat testDebugUnitTest`:

```text
Suite                                                                   Tests Failures Errors Skipped
-----                                                                   ----- -------- ------ -------
com.harmony.tokoharmony.core.database.Migration1To2Test                     1        0      0       0
com.harmony.tokoharmony.core.database.Migration2To3Test                     1        0      0       0
com.harmony.tokoharmony.core.database.Migration3To4Test                     1        0      0       0
com.harmony.tokoharmony.core.security.PinManagerTest                        4        0      0       0
com.harmony.tokoharmony.domain.usecase.AuthUseCaseTest                      1        0      0       0
com.harmony.tokoharmony.domain.usecase.BarcodeScannerDebounceTest           5        0      0       0
com.harmony.tokoharmony.domain.usecase.CategoryUseCaseTest                  3        0      0       0
com.harmony.tokoharmony.domain.usecase.CompleteTransactionAtomicityTest    11        0      0       0
com.harmony.tokoharmony.domain.usecase.DraftCartUseCaseTest                 7        0      0       0
com.harmony.tokoharmony.domain.usecase.Phase3IntegrationAuditTest           9        0      0       0
com.harmony.tokoharmony.domain.usecase.PriceManagementTest                  5        0      0       0
com.harmony.tokoharmony.domain.usecase.ProductUseCaseTest                  11        0      0       0
com.harmony.tokoharmony.domain.usecase.WeightedProductCalculationTest       3        0      0       0
com.harmony.tokoharmony.ExampleUnitTest                                     1        0      0       0
com.harmony.tokoharmony.feature.cashier.CashierPaymentViewModelTest         2        0      0       0
com.harmony.tokoharmony.feature.cashier.CashierViewModelsTest               3        0      0       0
```
- **Total Unit Tests:** **68**
- **Passed:** **68 (100%)**
- **Failed:** **0**
- **Errors:** **0**

### 6.2 Atomicity Test Matrix (`CompleteTransactionAtomicityTest`)
| Test Case | Scenario Verified | Result |
| :--- | :--- | :--- |
| `cashPayment_exactAmount_completesSuccessfullyWithZeroChange` | Cash exact amount -> `changeAmount = 0`, `COMPLETED`, `PAID` | **PASS** |
| `cashPayment_withChange_completesSuccessfullyWithAccurateChange` | Cash overpayment -> accurate `changeAmount`, `COMPLETED`, `PAID` | **PASS** |
| `cashPayment_insufficientAmount_rejectedAndRemainsDraft` | Cash underpayment -> rejected, remains `DRAFT` + `UNPAID` | **PASS** |
| `qris_beforeConfirmation_remainsDraftAndUnpaid` | QRIS before confirm -> `DRAFT` + `UNPAID`, no stock movements | **PASS** |
| `qris_afterConfirmation_completesWithPaidStatus` | QRIS after confirm -> `COMPLETED` + `PAID` | **PASS** |
| `completeTransaction_emptyCart_rejected` | Cart with 0 items -> rejected | **PASS** |
| `completeTransaction_insufficientStock_rejectedAndStockUnaffected` | Requested stock > available stock -> rejected, 0 movements committed | **PASS** |
| `completeSale_createsNegativeSaleStockMovement` | Physical product sale -> records `quantityDelta = -quantity` with `SALE` type | **PASS** |
| `completeSale_digitalProduct_createsNoStockMovement` | Digital product sale -> 0 stock movements generated | **PASS** |
| `completeSale_mixedPhysicalAndDigital_createsStockMovementOnlyForPhysical` | Mixed cart -> stock movements created strictly for physical items | **PASS** |
| `completeSale_fullLifecycleAndInvariants` | Price snapshot maintained, local total recalculated, draft detached, atomicity confirmed | **PASS** |

### 6.3 Connected Device Instrumentation Results
Executing `./gradlew.bat connectedDebugAndroidTest` on physical device `realme 9 Pro+ / RMX3393 (Android 14)`:
- **Status:** **BUILD SUCCESSFUL**
- **Tests run on device:** Passed.

### 6.4 APK Build Result
Executing `./gradlew.bat assembleDebug`:
- **Status:** **BUILD SUCCESSFUL**
- **Output:** `app/build/outputs/apk/debug/app-debug.apk`

---

## 7. Scope Boundary & Isolation Audit

As required by the Phase 4 specification, the following features have **NOT** been implemented and remain reserved for subsequent milestones:
- [x] **No Transaction Cancellation / Reversals:** No `SALE_REVERSAL` movements in Phase 4.
- [x] **No Stock In / Opname / Adjustments:** Stock movements currently only accept `SALE` from cashier checkout. Management flows belong to Phase 5.
- [x] **No Google Sheets / Remote Sync:** No sync queue processing, WorkManager sync, or network endpoints.
- [x] **No External Provider Integration:** No digital provider API integrations or external QRIS payment gateways.
- [x] **No Receipt Printer / Analytics:** No Bluetooth ESC/POS printer integration or multi-device sync.

---

## 8. Conclusion & Sign-Off

Phase 4 has met all functional, architectural, and data safety requirements.
- **Automated Tests:** 68/68 passed.
- **Atomicity:** Verified via `RoomDatabase.withTransaction` and rollbacks on failure.
- **Stock Ledger:** Verified with `StockMovement` table and negative delta validation.
- **Build Status:** Clean debug APK compilation and successful on-device test run.

**Phase 4 is complete and READY FOR PHASE 5.**
