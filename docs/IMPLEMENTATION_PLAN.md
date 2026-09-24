# Implementation Blueprint V0.1 — Android POS

## 1. Technology Decision

### Platform

```text
Android
```

### Language

```text
Kotlin
```

### UI

```text
Jetpack Compose
```

### Architecture

```text
MVVM
+
Clean Architecture principles
+
Repository Pattern
```

### Local Database

```text
Room
SQLite
```

Room menjadi abstraction layer di atas SQLite dan direkomendasikan untuk aplikasi Android yang membutuhkan persistence data terstruktur.

### Dependency Injection

```text
Hilt
```

Hilt merupakan solusi DI Android yang direkomendasikan Google untuk mengurangi boilerplate dependency injection.

### Barcode

```text
CameraX
+
ML Kit Barcode Scanning
```

ML Kit mampu membaca berbagai format barcode umum dan proses scanning dapat dilakukan sepenuhnya pada perangkat.

### Background Sync

```text
WorkManager
```

WorkManager mendukung network constraints, retry, backoff, dan pekerjaan background yang persisten.

### Remote Storage

```text
Google Apps Script
        ↓
Google Sheets
```

---

# 2. Dependency Strategy

Gunakan Gradle Version Catalog:

```text
gradle/libs.versions.toml
```

Jangan menyebarkan nomor versi library ke seluruh file Gradle.

Untuk library yang sudah diverifikasi terhadap dokumentasi resmi saat penyusunan blueprint:

```text
Room
2.8.5

Hilt
2.57.1

ML Kit bundled barcode scanning
17.3.0
```

Room 2.8.5 tercatat sebagai stable release pada September 2026. Hilt 2.57.1 dan ML Kit Barcode Scanning 17.3.0 tercantum pada dokumentasi resmi masing-masing.

Untuk Compose, CameraX, WorkManager, Navigation, Retrofit, dan library lain, gunakan versi stable yang sesuai dengan template Android Studio saat project dibuat dan kendalikan seluruhnya melalui Version Catalog.

---

# 3. Project Module

Untuk MVP gunakan **satu Gradle module `app`**.

Jangan membuat multi-module architecture terlebih dahulu.

Struktur:

```text
root/
│
├── app/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle/
│   └── libs.versions.toml
│
└── README.md
```

Multi-module dapat dipertimbangkan ketika codebase sudah cukup besar.

---

# 4. Package Structure

Base package:

```text
com.example.tokokasir
```

Struktur:

```text
com.example.tokokasir
│
├── core
│   ├── common
│   │   ├── Result.kt
│   │   ├── Error.kt
│   │   └── DispatchersProvider.kt
│   │
│   ├── database
│   │   ├── AppDatabase.kt
│   │   │
│   │   ├── entity
│   │   │   ├── CategoryEntity.kt
│   │   │   ├── ProductEntity.kt
│   │   │   ├── PriceHistoryEntity.kt
│   │   │   ├── TransactionEntity.kt
│   │   │   ├── TransactionItemEntity.kt
│   │   │   ├── StockMovementEntity.kt
│   │   │   ├── StockInEntity.kt
│   │   │   ├── StockAdjustmentEntity.kt
│   │   │   ├── DigitalTransactionEntity.kt
│   │   │   ├── UserEntity.kt
│   │   │   └── SyncQueueEntity.kt
│   │   │
│   │   └── dao
│   │       ├── CategoryDao.kt
│   │       ├── ProductDao.kt
│   │       ├── PriceHistoryDao.kt
│   │       ├── TransactionDao.kt
│   │       ├── TransactionItemDao.kt
│   │       ├── StockMovementDao.kt
│   │       ├── StockInDao.kt
│   │       ├── StockAdjustmentDao.kt
│   │       ├── DigitalTransactionDao.kt
│   │       ├── UserDao.kt
│   │       └── SyncQueueDao.kt
│   │
│   ├── network
│   │   ├── api
│   │   │   └── RemoteSyncApi.kt
│   │   ├── dto
│   │   └── NetworkModule.kt
│   │
│   ├── sync
│   │   ├── SyncWorker.kt
│   │   ├── SyncManager.kt
│   │   └── SyncQueueProcessor.kt
│   │
│   ├── security
│   │   ├── PinManager.kt
│   │   └── SessionManager.kt
│   │
│   └── di
│       ├── DatabaseModule.kt
│       ├── NetworkModule.kt
│       └── RepositoryModule.kt
│
├── data
│   ├── local
│   │   ├── datasource
│   │   └── mapper
│   │
│   ├── remote
│   │   ├── datasource
│   │   ├── dto
│   │   └── mapper
│   │
│   └── repository
│       ├── ProductRepositoryImpl.kt
│       ├── TransactionRepositoryImpl.kt
│       ├── InventoryRepositoryImpl.kt
│       ├── PriceRepositoryImpl.kt
│       ├── UserRepositoryImpl.kt
│       └── SyncRepositoryImpl.kt
│
├── domain
│   ├── model
│   │   ├── Product.kt
│   │   ├── Transaction.kt
│   │   ├── TransactionItem.kt
│   │   ├── StockMovement.kt
│   │   └── DigitalTransaction.kt
│   │
│   ├── repository
│   │   ├── ProductRepository.kt
│   │   ├── TransactionRepository.kt
│   │   ├── InventoryRepository.kt
│   │   ├── PriceRepository.kt
│   │   ├── UserRepository.kt
│   │   └── SyncRepository.kt
│   │
│   └── usecase
│       ├── auth
│       ├── product
│       ├── cart
│       ├── transaction
│       ├── inventory
│       ├── price
│       └── sync
│
├── feature
│   ├── home
│   ├── auth
│   │
│   ├── cashier
│   │   ├── transaction
│   │   ├── scanner
│   │   ├── search
│   │   ├── cart
│   │   └── payment
│   │
│   └── admin
│       ├── dashboard
│       ├── product
│       ├── price
│       ├── inventory
│       ├── transaction
│       └── sync
│
└── navigation
    └── AppNavigation.kt
```

---

# 5. Layer Responsibilities

## Presentation

Berisi:

```text
Composable
ViewModel
UiState
UiEvent
```

Tidak boleh berisi business logic.

---

## Domain

Berisi:

```text
Use Case
Business Rule
Domain Model
Repository Interface
```

Ini adalah bagian yang paling penting untuk menjaga aturan bisnis.

---

## Data

Berisi:

```text
Repository Implementation
DAO
Remote API
DTO
Mapper
```

Domain tidak mengetahui Room atau Google Sheets.

---

# 6. Product Entity

Contoh entity:

```text
ProductEntity
```

Field:

```text
productId                  String PK
categoryId                 String FK
name                       String
barcode                    String?
productKind                String
pricingMethod              String
quantityType               String
sellingUnit                String
stockUnit                  String
purchaseUnit               String?
purchaseToStockFactor      Long?
currentPrice               Long
minimumStock               Long?
isActive                   Boolean
createdAt                  Long
updatedAt                  Long
```

## quantityType

```text
COUNT
GRAM
```

Contoh:

```text
Indomie
quantityType = COUNT
```

```text
Telur
quantityType = GRAM
```

Hal ini lebih tepat daripada membuat `WEIGHTED`, `PACKAGED_WEIGHT`, dan `BARCODED` sebagai jenis produk.

---

# 7. Price Representation

Harga menggunakan:

```text
Long
```

Contoh:

```text
4000
18000
30000
```

Satuannya:

```text
Rupiah
```

Jangan gunakan Float atau Double untuk uang.

---

# 8. Quantity Representation

### COUNT

```text
1 pcs = 1
2 pcs = 2
3 pcs = 3
```

### GRAM

```text
0,5 kg = 500
1,0 kg = 1000
1,5 kg = 1500
2,0 kg = 2000
```

Gunakan:

```text
Long
```

untuk quantity.

---

# 9. Weighted Calculation

Harga disimpan sebagai:

```text
pricePerKg
```

Quantity:

```text
grams
```

Formula:

```text
subtotal =
(grams × pricePerKg) / 1000
```

Contoh:

```text
1500 × 30000 / 1000
= 45000
```

Karena berat hanya kelipatan 500 gram pada MVP, perhitungan dapat direpresentasikan sebagai integer rupiah.

---

# 10. Product Examples

## Indomie

```text
productKind = PHYSICAL
pricingMethod = PER_UNIT
quantityType = COUNT
sellingUnit = PCS
stockUnit = PCS
purchaseUnit = DUS
purchaseToStockFactor = 40
```

## Telur

```text
productKind = PHYSICAL
pricingMethod = PER_KG
quantityType = GRAM
sellingUnit = KG
stockUnit = KG
purchaseUnit = KG
purchaseToStockFactor = 1
```

## Gula 1 kg

```text
productKind = PHYSICAL
pricingMethod = PER_UNIT
quantityType = COUNT
sellingUnit = PACK
stockUnit = PACK
```

Gula 0,5 kg dan Gula 1 kg diperlakukan sebagai produk yang berbeda jika toko memang memiliki harga dan stok terpisah.

---

# 11. Category Entity

```text
CategoryEntity

categoryId
name
isActive
createdAt
updatedAt
```

Category memiliki relasi:

```text
Category 1 ─── N Product
```

Room mendukung relationship seperti one-to-many melalui query/relational model.

---

# 12. Transaction Entity

```text
TransactionEntity

transactionId
transactionNumber
transactionStatus
paymentStatus
paymentMethod
totalAmount
amountReceived
changeAmount
createdAt
completedAt
cancelledAt
createdBy
cancelledBy
cancellationReason
```

## Transaction Status

```text
DRAFT
COMPLETED
CANCELLED
```

## Payment Status

```text
UNPAID
PAID
```

## Payment Method

```text
CASH
QRIS
```

`amountReceived` dapat `null` untuk QRIS.

`changeAmount` bernilai `0` untuk QRIS.

---

# 13. Persistent Draft Cart

Keranjang aktif disimpan sebagai:

```text
Transaction.status = DRAFT
```

dan:

```text
TransactionItems
```

bukan hanya `remember { mutableStateOf(...) }` di Compose.

Hal ini memastikan cart tetap ada ketika:

```text
Scanner dibuka
Search dibuka
Product Registration dibuka
Screen berubah
Activity direcreate
```

---

# 14. Transaction Item Entity

```text
TransactionItemEntity

itemId
transactionId
productId

productNameSnapshot

quantity
unitPrice
subtotal
sellingUnit
```

`productNameSnapshot` disimpan untuk menjaga histori transaksi.

`unitPrice` merupakan snapshot harga.

---

# 15. Stock Movement Entity

```text
StockMovementEntity

movementId
productId

movementType
quantityDelta

referenceType
referenceId

reason

createdAt
createdBy
```

Movement:

```text
INITIAL_STOCK
STOCK_IN
SALE
SALE_REVERSAL
ADJUSTMENT
```

---

# 16. Stock Calculation DAO

DAO menyediakan:

```text
getStock(productId)
```

dengan:

```text
SUM(quantityDelta)
```

Contoh:

```text
INITIAL_STOCK   +160
SALE              -3
SALE              -2
STOCK_IN          +40
ADJUSTMENT         -1
```

Current Stock:

```text
194
```

Tidak ada `UPDATE product SET stock = ...` sebagai sumber kebenaran inventory.

---

# 17. Stock In Entity

```text
StockInEntity

stockInId
productId

purchaseQuantity
purchaseUnit

conversionFactor
stockQuantity

note

createdAt
createdBy
```

Contoh:

```text
4 dus
× 40 pcs
= 160 pcs
```

Movement:

```text
STOCK_IN +160
```

---

# 18. Stock Adjustment Entity

```text
StockAdjustmentEntity

adjustmentId
productId

systemQuantity
physicalQuantity
difference

reason
note

createdAt
createdBy
```

Formula:

```text
difference =
physicalQuantity - systemQuantity
```

---

# 19. Price History Entity

```text
PriceHistoryEntity

historyId
productId

oldPrice
newPrice

changedAt
changedBy
```

Setiap perubahan harga membuat satu history record.

---

# 20. User Entity

```text
UserEntity

userId
displayName
role
pinHash

isActive

createdAt
updatedAt
```

Role:

```text
ADMIN
CASHIER
```

---

# 21. Digital Transaction Entity

```text
DigitalTransactionEntity

digitalTransactionId
transactionItemId

serviceType
customerNumber
nominal

providerReference
status

createdAt
```

Status:

```text
SUCCESS
FAILED
PENDING
```

Produk digital tidak menghasilkan StockMovement.

---

# 22. Sync Queue Entity

```text
SyncQueueEntity

queueId

entityType
entityId

operation

payload

status

retryCount
lastError

createdAt
syncedAt
```

Status:

```text
PENDING
SYNCING
SYNCED
FAILED
```

Operation:

```text
CREATE
UPDATE
CANCEL
```

---

# 23. DAO Responsibilities

## ProductDao

```text
findByBarcode()
searchByName()
getById()
getActiveProducts()
insert()
update()
```

Index:

```text
barcode
name
isActive
```

Barcode harus memiliki unique index ketika nilainya tidak null.

---

## TransactionDao

```text
getActiveDraft()
insert()
update()
getById()
getHistory()
getDetail()
```

---

## TransactionItemDao

```text
getByTransactionId()
insert()
updateQuantity()
delete()
deleteByTransactionId()
```

---

## StockMovementDao

```text
insert()
insertAll()
getStock()
getMovements()
```

---

## PriceHistoryDao

```text
insert()
getByProductId()
```

---

## StockInDao

```text
insert()
getHistory()
```

---

## StockAdjustmentDao

```text
insert()
getHistory()
```

---

## SyncQueueDao

```text
insert()
getPending()
markSyncing()
markSynced()
markFailed()
getPendingCount()
```

---

# 24. Room Database

```kotlin
@Database(
    entities = [
        CategoryEntity::class,
        ProductEntity::class,
        PriceHistoryEntity::class,
        TransactionEntity::class,
        TransactionItemEntity::class,
        StockMovementEntity::class,
        StockInEntity::class,
        StockAdjustmentEntity::class,
        DigitalTransactionEntity::class,
        UserEntity::class,
        SyncQueueEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun categoryDao(): CategoryDao
    abstract fun productDao(): ProductDao
    abstract fun transactionDao(): TransactionDao
    abstract fun transactionItemDao(): TransactionItemDao
    abstract fun stockMovementDao(): StockMovementDao
    abstract fun stockInDao(): StockInDao
    abstract fun stockAdjustmentDao(): StockAdjustmentDao
    abstract fun priceHistoryDao(): PriceHistoryDao
    abstract fun digitalTransactionDao(): DigitalTransactionDao
    abstract fun userDao(): UserDao
    abstract fun syncQueueDao(): SyncQueueDao
}
```

Gunakan database migration sejak awal dan jangan mengandalkan destructive migration untuk production data.

Room menyediakan mekanisme migration dan compile-time verification untuk query.

---

# 25. Transaction Repository

Interface domain:

```kotlin
interface TransactionRepository {

    suspend fun getActiveDraft(): Transaction?

    suspend fun addItem(
        productId: String
    )

    suspend fun updateQuantity(
        itemId: String,
        quantity: Long
    )

    suspend fun removeItem(
        itemId: String
    )

    suspend fun completeCashTransaction(
        amountReceived: Long
    ): Result<Transaction>

    suspend fun completeQrisTransaction(): Result<Transaction>

    suspend fun cancelTransaction(
        transactionId: String,
        reason: String
    ): Result<Unit>
}
```

Implementasi tidak diketahui domain.

---

# 26. Complete Transaction

Use case:

```text
CompleteTransactionUseCase
```

Proses:

```text
1. Load DRAFT
2. Validate items
3. Validate products
4. Validate stock
5. Validate payment
6. Recalculate totals
7. Update transaction
8. Insert transaction items
9. Insert SALE movements
10. Insert sync queue records
11. Commit
```

Gunakan:

```text
RoomDatabase.withTransaction { ... }
```

untuk menjaga atomicity.

---

# 27. Cash Payment Logic

Input:

```text
total = 65000
received = 100000
```

Calculation:

```text
change = 100000 - 65000
       = 35000
```

Validation:

```text
received >= total
```

Jika:

```text
received < total
```

return:

```text
INSUFFICIENT_PAYMENT
```

---

# 28. QRIS Payment Logic

Flow:

```text
Cart
 ↓
Payment QRIS
 ↓
UNPAID
 ↓
Kasir memeriksa pembayaran
 ↓
Confirm Payment
 ↓
PAID
 ↓
COMPLETED
```

Tidak ada integrasi QRIS pada MVP.

---

# 29. Barcode Scanner Architecture

Flow:

```text
CameraX
   ↓
Image Analysis
   ↓
ML Kit Barcode Scanner
   ↓
Raw Barcode Value
   ↓
ScanBarcodeUseCase
   ↓
ProductRepository
   ↓
Room
```

ML Kit menyediakan dukungan barcode EAN-13, EAN-8, Code 128, UPC, dan format umum lainnya. Scanning dapat dilakukan di perangkat.

---

# 30. Barcode Scan Throttling

Scanner tidak boleh terus menerus menambahkan item dari barcode yang sama dalam setiap frame kamera.

Gunakan:

```text
lastScannedBarcode
lastScannedTimestamp
```

Contoh:

```text
same barcode
within 800 ms
→ ignore
```

Setelah waktu debounce:

```text
scan again
→ quantity +1
```

Nilai debounce dapat diuji saat penggunaan nyata.

---

# 31. Unknown Barcode Flow

```text
SCAN
 ↓
BARCODE FOUND?
 │
 ├── YES → ADD TO CART
 │
 └── NO
       ↓
Unknown Barcode
       ↓
Daftarkan Produk Baru
       ↓
Admin Authentication
       ↓
Product Registration
       ↓
Save
       ↓
Return to Draft Cart
       ↓
Add Product
```

Draft transaction tidak boleh hilang.

---

# 32. Product Registration Use Case

```text
RegisterProductUseCase
```

Validation:

```text
name != empty

barcode unique

price valid

product kind valid

pricing method valid

unit valid
```

Untuk product physical:

```text
initial stock
```

boleh:

```text
0
```

Untuk product digital:

```text
stock
```

tidak berlaku.

---

# 33. Price Change Use Case

```text
ChangePriceUseCase
```

Proses:

```text
Validate ADMIN
 ↓
Read current price
 ↓
Update Product.currentPrice
 ↓
Create PriceHistory
 ↓
Create SyncQueue
```

Tidak ada batas perubahan harga per hari.

---

# 34. Stock In Use Case

```text
CreateStockInUseCase
```

Proses:

```text
Validate ADMIN
 ↓
Read product conversion factor
 ↓
Convert purchase quantity
 ↓
Create StockIn
 ↓
Create StockMovement(STOCK_IN)
 ↓
Create SyncQueue
```

Contoh:

```text
4 dus × 40 pcs
= 160 pcs
```

---

# 35. Stock Opname Use Case

```text
PerformStockAdjustmentUseCase
```

Proses:

```text
Validate ADMIN
 ↓
Get system stock
 ↓
Input physical stock
 ↓
Calculate difference
 ↓
Create StockAdjustment
 ↓
Create StockMovement
 ↓
Create SyncQueue
```

---

# 36. Cancellation Use Case

```text
CancelTransactionUseCase
```

Proses:

```text
Validate ADMIN
 ↓
Load transaction
 ↓
Check status == COMPLETED
 ↓
Update status = CANCELLED
 ↓
Create SALE_REVERSAL
 ↓
Create SyncQueue
```

Transaction tidak dihapus.

---

# 37. Sync Architecture

```text
Local Database
      │
      ▼
  Sync Queue
      │
      ▼
 WorkManager
      │
      ▼
Remote Sync API
      │
      ▼
Apps Script
      │
      ▼
Google Sheets
```

WorkManager dipakai karena mendukung pekerjaan background persisten, network constraints, retry, dan backoff.

---

# 38. Sync Worker

Konseptual:

```kotlin
val constraints =
    Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

val request =
    OneTimeWorkRequestBuilder<SyncWorker>()
        .setConstraints(constraints)
        .build()
```

Ketika sync gagal:

```text
Result.retry()
```

WorkManager akan menangani retry sesuai kebijakan backoff.

---

# 39. Sync Trigger

Trigger:

```text
1. Setelah transaction completed
2. Setelah price change
3. Setelah stock change
4. Setelah product change
5. Ketika network kembali
6. Saat aplikasi dibuka
```

Transaksi tidak menunggu sync.

---

# 40. Batch Synchronization

Jangan melakukan:

```text
1 entity
↓
1 HTTP request
```

Gunakan:

```text
Pending records
      ↓
Batch
      ↓
1 request
```

Contoh:

```text
10 transactions
20 transaction items
15 stock movements
5 price changes
```

dapat dikirim dalam satu batch.

Android sendiri merekomendasikan batching/defer untuk pekerjaan network agar lebih efisien; WorkManager cocok untuk pola tersebut.

---

# 41. Remote API

Apps Script API minimal:

```text
POST /sync
GET  /bootstrap
```

## POST /sync

Request:

```json
{
  "deviceId": "...",
  "operations": [
    {
      "entityType": "TRANSACTION",
      "entityId": "...",
      "operation": "CREATE",
      "payload": {}
    }
  ]
}
```

Response:

```json
{
  "success": true,
  "processed": [],
  "failed": []
}
```

---

# 42. Idempotency

Remote layer wajib mengenali:

```text
entityType
+
entityId
```

sebagai unique identity.

Retry:

```text
same entityId
```

harus menghasilkan:

```text
same record
```

bukan record baru.

---

# 43. Google Sheets

Sheet:

```text
Categories
Products
Transactions
TransactionItems
PriceHistory
StockIns
StockAdjustments
StockMovements
DigitalTransactions
Users
```

Google Sheets bukan tempat kasir beroperasi.

---

# 44. Bootstrap / Restore

Saat:

```text
new phone
```

atau:

```text
local database lost
```

sistem dapat:

```text
GET /bootstrap
      ↓
Google Sheets
      ↓
Download data
      ↓
Local Database
```

Data master dan histori dipulihkan sesuai ID.

---

# 45. Authentication

Mode:

```text
CASHIER
ADMIN
```

Mode Admin:

```text
Home
 ↓
Mode Orang Tua
 ↓
PIN
 ↓
Admin Session
```

PIN disimpan dalam bentuk hash/secure representation.

Authorization tetap dilakukan dalam Use Case.

Menyembunyikan tombol saja tidak cukup.

---

# 46. Navigation

```text
Home
│
├── Cashier
│   └── Transaction
│       ├── Search
│       ├── Scanner
│       ├── Weighted Input
│       ├── Cart
│       └── Payment
│
└── Admin
    ├── Dashboard
    ├── Products
    ├── Prices
    ├── Initial Stock
    ├── Stock In
    ├── Stock Opname
    ├── Transactions
    └── Sync
```

---

# 47. ViewModel Example

## CashierViewModel

Tanggung jawab:

```text
observe current draft
add product
update quantity
remove item
calculate total
start payment
```

Bukan:

```text
langsung insert ke DAO
```

Contoh alur:

```text
UI
 ↓
CashierViewModel
 ↓
AddProductToCartUseCase
 ↓
TransactionRepository
 ↓
Room
```

---

# 48. Recommended Use Cases

## Product

```text
CreateProductUseCase
RegisterUnknownBarcodeUseCase
UpdateProductUseCase
DeactivateProductUseCase
SearchProductUseCase
GetProductByBarcodeUseCase
```

## Cart

```text
GetActiveCartUseCase
AddProductToCartUseCase
AddWeightedProductToCartUseCase
UpdateCartQuantityUseCase
RemoveCartItemUseCase
```

## Payment

```text
CalculateChangeUseCase
CompleteCashTransactionUseCase
ConfirmQrisPaymentUseCase
CompleteQrisTransactionUseCase
```

## Transaction

```text
GetTransactionHistoryUseCase
GetTransactionDetailUseCase
CancelTransactionUseCase
```

## Price

```text
ChangeProductPriceUseCase
GetPriceHistoryUseCase
```

## Inventory

```text
CreateInitialStockUseCase
CreateStockInUseCase
GetCurrentStockUseCase
PerformStockAdjustmentUseCase
GetStockMovementHistoryUseCase
```

## Sync

```text
QueueSyncOperationUseCase
SyncPendingDataUseCase
GetSyncStatusUseCase
RestoreDataUseCase
```

---

# 49. Coding Rule

Jangan membuat class seperti:

```text
StoreManager.kt
```

yang mengurus:

```text
product
price
cart
transaction
stock
sync
```

Satu class harus mempunyai tanggung jawab yang jelas.

Contoh:

```text
PriceRepository
InventoryRepository
TransactionRepository
ProductRepository
```

lebih baik daripada:

```text
EverythingRepository
```

---

# 50. Testing Structure

```text
app/src/
├── test/
│   ├── domain/
│   ├── data/
│   └── core/
│
└── androidTest/
    ├── database/
    ├── integration/
    └── ui/
```

Test utama:

```text
PriceCalculationTest
WeightedPriceTest
CashChangeTest
StockCalculationTest
StockReversalTest
StockAdjustmentTest
TransactionCompletionTest
TransactionCancellationTest
UnknownBarcodeTest
SyncRetryTest
```

Room menyediakan dukungan untuk pengujian database dan schema/migration; pengujian database sebaiknya menjadi bagian dari development lifecycle.

---

# 51. Development Order

## Phase 1 — Foundation

```text
Project
Room
Hilt
Navigation
Theme
Base UI
```

## Phase 2 — Product

```text
Category
Product
Barcode
Search
Product Registration
Price
Initial Stock
```

## Phase 3 — Cashier

```text
Draft Cart
Scanner
Search
Weighted Product
Quantity
```

## Phase 4 — Transaction

```text
Cash
QRIS Confirmation
Transaction
Stock SALE
Success screen
```

## Phase 5 — Admin Inventory

```text
Stock In
Stock Opname
Adjustment
Cancellation
History
```

## Phase 6 — Sync

```text
Sync Queue
WorkManager
Apps Script
Google Sheets
Retry
Bootstrap
```

## Phase 7 — Digital

```text
Digital Product
Digital Transaction
SUCCESS/FAILED/PENDING
```

---

# 52. Definition of Done — MVP

MVP belum dianggap selesai hanya karena semua screen sudah muncul.

MVP dianggap selesai ketika:

```text
Create Product
        ↓
Set Price
        ↓
Initial Stock
        ↓
Scan/Search
        ↓
Cart
        ↓
Payment
        ↓
Completed Transaction
        ↓
Stock Reduced
        ↓
Data Stored Locally
        ↓
Sync to Google Sheets
```

seluruh alur dapat berjalan end-to-end tanpa intervensi manual pada database.

---

# 53. Final Architecture

```text
                          GOOGLE SHEETS
                               ▲
                               │
                         APPS SCRIPT API
                               ▲
                               │
                        ┌──────┴───────┐
                        │ Sync Worker  │
                        │ WorkManager  │
                        └──────▲───────┘
                               │
                        ┌──────┴───────┐
                        │  Sync Queue  │
                        └──────▲───────┘
                               │
╔══════════════════════════════╧══════════════════════════════╗
║                       ANDROID APP                          ║
║                                                            ║
║  ┌──────────────────────────────────────────────────────┐  ║
║  │                  PRESENTATION                        │  ║
║  │ Cashier │ Scanner │ Cart │ Payment │ Admin │ Auth   │  ║
║  └──────────────────────────┬───────────────────────────┘  ║
║                             │                              ║
║  ┌──────────────────────────▼───────────────────────────┐  ║
║  │                     DOMAIN                           │  ║
║  │ Use Cases │ Business Rules │ Repository Interfaces   │  ║
║  └──────────────────────────┬───────────────────────────┘  ║
║                             │                              ║
║  ┌──────────────────────────▼───────────────────────────┐  ║
║  │                      DATA                            │  ║
║  │ Repository │ Local DS │ Remote DS │ Mapper            │  ║
║  └───────────────┬───────────────────────┬───────────────┘  ║
║                  │                       │                  ║
║                  ▼                       ▼                  ║
║              ┌───────┐             Remote API              ║
║              │ Room  │                                  ║
║              │SQLite │                                  ║
║              └───────┘                                  ║
╚════════════════════════════════════════════════════════════╝

External:
CameraX → ML Kit Barcode Scanner
```



# Implementation Plan — Toko Kasir Android POS (Toko Harmony)

## Document Overview & Authority

This document defines the authoritative, phased development roadmap for the Toko Kasir Android POS application (`Toko Harmony`).
It is derived from the repository documentation hierarchy specified in `AGENTS.md`:

1. `docs/BUSINESS_RULES.md` (authoritative business rules)
2. `docs/ARCHITECTURE.md` (authoritative technical/data architecture)
3. `docs/PRD.md` (product requirements & scope)
4. `docs/UX_SPEC.md` (user flows & screen behavior)
5. `docs/IMPLEMENTATION_PLAN.md` (this document)

---

## 1. Repository State & Initial Assessment

- **Current Repository Status**: Android Project Foundation Established (Phase 1 Completed & Verified).
- **Project Structure**:
  - Root Gradle files (`build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`, wrapper 8.7).
  - Version Catalog (`gradle/libs.versions.toml`) with stable versions for Compose BOM, Hilt, Room, and Navigation.
  - Single application module (`app/`) configured with `namespace = "com.harmony.tokoharmony"`, `compileSdk = 34`, `minSdk = 26`, `targetSdk = 34`.
  - Phase 1 Foundation implemented: `TokoHarmonyApp`, `MainActivity`, `AppNavigation`, `HomeScreen`, `AppDatabase` (v1 with schema export), `PinManager` (PBKDF2/SHA-256), `Result`, `DispatcherProvider`, Hilt modules, and unit test suite.
- **Verification Status**:
  - `.\gradlew testDebugUnitTest`: BUILD SUCCESSFUL (all unit tests passed).
  - `.\gradlew assembleDebug`: BUILD SUCCESSFUL (debug APK packaged).
  - Room schema export: `app/schemas/com.harmony.tokoharmony.core.database.AppDatabase/1.json` verified.

---

## 2. Documentation Conflicts & Priority Resolutions

In accordance with Section 2 of `AGENTS.md`, conflicts are resolved using the precedence `BUSINESS_RULES > ARCHITECTURE > PRD > UX_SPEC`:

| Area | Conflict / Discrepancy | Higher-Authority Resolution |
| :--- | :--- | :--- |
| **Weight Representation** | PRD V0.1 §12 mentions decimal quantity (`quantity = 1.5`). | **Integer Grams (`Long`)**: BUSINESS_RULES BR-018/BR-019 and ARCHITECTURE mandate integer grams (`500`, `1000`, `1500` grams) and integer Rupiah (`Long`). No `Float` or `Double` for money or persisted weight. |
| **Product Classification** | PRD V0.1 §4 lists product types as `BARCODED`, `NON_BARCODE`, `WEIGHTED`, `PACKAGED_WEIGHT`, `DIGITAL`. | **Normalized Domain Model**: ARCHITECTURE / PRD V0.2 §4 define `productKind` (`PHYSICAL \| DIGITAL`), `pricingMethod` (`PER_UNIT \| PER_KG`), and `quantityType` (`COUNT \| GRAM`). Barcode is an optional nullable attribute on products. |
| **Authoritative Stock** | PRD V0.1 §4 lists a mutable `stock` field in `Product`. | **Movement Ledger Truth**: BUSINESS_RULES BR-008/BR-009 and ARCHITECTURE mandate `StockMovement` as the sole operational source of truth. Stock is derived: `INITIAL_STOCK + STOCK_IN - SALE + SALE_REVERSAL ± ADJUSTMENT`. |
| **Package Identity** | Architecture blueprints reference `com.example.tokokasir`. | **Align with Workspace**: Use `com.harmony.tokoharmony` as the root package namespace across all architectural layers. |

---

## 3. Non-Negotiable Project Invariants

1. **Room Database** is the local operational source of truth. Google Sheets is never the primary cashier database.
2. **StockMovement** is the sole source of stock history. Never use an editable `Product.stock` field.
3. **Money Representation**: Strictly integer `Long` Rupiah, never `Float` or `Double`.
4. **Weighted Quantities**: Strictly integer `Long` grams (MVP supported increments: 500g).
5. **Snapshotting**: `TransactionItem` must snapshot `product_name` and `unit_price` at the moment of addition.
6. **Cart Persistence**: Draft carts persist in Room (`transaction_status = DRAFT`, `payment_status = UNPAID`).
7. **Cart Preservation**: Registering an unknown barcode or navigating to Admin must not clear the active draft cart.
8. **Role Authorization**: Only `ADMIN` can alter master prices, perform stock adjustments, stock in, or cancel transactions. Enforced in use cases, not only in UI.
9. **Transaction Immutability**: Completed transactions are never physically deleted. Cancellation marks them `CANCELLED` and issues `SALE_REVERSAL` movements.
10. **Negative Stock Prevention**: Physical sales must not drive stock below zero.
11. **Explicit QRIS Confirmation**: Selecting QRIS does not complete payment; cashier must explicitly confirm receipt.
12. **Digital Separation**: Digital transactions never alter physical inventory.
13. **Sync Idempotency**: Google Sheets synchronization is asynchronous, batched, retry-safe, and keyed on immutable UUIDs.

---

## 4. Phased Implementation Roadmap

```
Phase 1: Foundation (Build, Room, Hilt, Compose, Navigation, Theme)
   │
Phase 2: Product & Category Management (Master data, Price history, Admin auth)
   │
Phase 3: Cashier Core (Draft cart in Room, Barcode scan, Search, Weight handling)
   │
Phase 4: Payment & Transaction Completion (Cash calculation, QRIS confirm, Atomic sale)
   │
Phase 5: Inventory & Stock Ledger (Initial stock, Stock In, Stock Opname, Reversal)
   │
Phase 6: Remote Synchronization (SyncQueue, WorkManager, Apps Script, Sheets)
   │
Phase 7: Digital Transactions & Hardening (Digital record, History filters, Backup)
```

---

### PHASE 1 — FOUNDATION

#### Objective
Establish a clean, modern Android foundation with verified build configuration, Version Catalog, Jetpack Compose, Hilt Dependency Injection, Navigation Compose, Room Database with schema export, base package structure, and unit/integration testing harnesses.

#### Implementation Order
1. **Fix Version Catalog & Gradle Configuration**:
   - Correct invalid `coreKtx = "1.19.0"` to stable `1.13.1`.
   - Configure Kotlin (1.9.24) and Compose Compiler (1.5.14) or Kotlin 2.0+ with Compose Compiler plugin.
   - Add dependencies for Jetpack Compose BOM, Hilt, Room, Navigation Compose, and Lifecycle ViewModel Compose.
   - Configure Room KSP compiler and schema export directory (`schemas/`).
2. **Setup Base Packages under `com.harmony.tokoharmony`**:
   - `core/common`: `Result`, `Error`, `DispatchersProvider`.
   - `core/database`: `AppDatabase`, type converters.
   - `core/security`: `PinManager` (PIN hashing using PBKDF2/SHA-256).
   - `core/di`: `DatabaseModule`, `CoroutinesModule`.
   - `ui/theme`: Color palette, typography, shapes, `TokoHarmonyTheme`.
   - `navigation`: `AppNavigation`, route definitions (`Home`, `Cashier`, `Admin`, `Auth`).
3. **Setup Entry Point**:
   - `TokoHarmonyApp`: `@HiltAndroidApp` Application class.
   - `MainActivity`: `@AndroidEntryPoint` hosting Compose `AppNavigation`.
   - Home Screen with navigation to Mode Kasir and Mode Orang Tua (PIN dialog placeholder).
4. **Verification & Test Setup**:
   - Verify Gradle build (`.\gradlew assembleDebug` and `.\gradlew testDebugUnitTest`).
   - Setup unit test harness for common utilities and Room database test runner.

#### Files & Packages Affected
- `gradle/libs.versions.toml`
- `build.gradle.kts`
- `app/build.gradle.kts`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/harmony/tokoharmony/TokoHarmonyApp.kt`
- `app/src/main/java/com/harmony/tokoharmony/MainActivity.kt`
- `app/src/main/java/com/harmony/tokoharmony/core/common/Result.kt`
- `app/src/main/java/com/harmony/tokoharmony/core/common/DispatcherProvider.kt`
- `app/src/main/java/com/harmony/tokoharmony/core/database/AppDatabase.kt`
- `app/src/main/java/com/harmony/tokoharmony/core/security/PinManager.kt`
- `app/src/main/java/com/harmony/tokoharmony/core/di/DatabaseModule.kt`
- `app/src/main/java/com/harmony/tokoharmony/core/di/CoroutinesModule.kt`
- `app/src/main/java/com/harmony/tokoharmony/ui/theme/*`
- `app/src/main/java/com/harmony/tokoharmony/navigation/AppNavigation.kt`
- `app/src/main/java/com/harmony/tokoharmony/navigation/Screen.kt`
- `app/src/main/java/com/harmony/tokoharmony/feature/home/HomeScreen.kt`

#### Database Changes
- Initial `AppDatabase` setup (Version 1) with schema export enabled.
- Temporary baseline entity or initial entities (`UserEntity`) to validate Room compile-time verification.

#### Dependencies
- AndroidX Core KTX (`1.13.1`), Lifecycle Runtime KTX (`2.8.5`), Activity Compose (`1.9.2`).
- Jetpack Compose BOM (`2024.09.00`), Material 3, Material Icons Extended.
- Hilt Android & Compiler (`2.51.1`).
- Room Runtime, KTX & Compiler (`2.6.1`).
- Navigation Compose (`2.8.0`).
- JUnit 4, Truth / MockK / Kotlinx Coroutines Test for unit tests.

#### Tests
- `DispatcherProviderTest`: verify dispatcher abstraction.
- `PinManagerTest`: verify PBKDF2/SHA-256 PIN hashing and comparison.
- Baseline Room Database test to verify in-memory database creation.

#### Acceptance Criteria
- `.\gradlew assembleDebug` compiles with 0 errors.
- `.\gradlew test` passes all unit tests.
- App launches into Home Screen showing "Mode Kasir" and "Mode Orang Tua" buttons.
- No business logic or architectural leakage in presentation components.

#### Risks & Mitigations
- *Risk*: Dependency resolution conflicts between Kotlin, AGP, and Compose compiler.
- *Mitigation*: Pin exact compatible versions in `libs.versions.toml` verified against Google AndroidX release matrix.

---

### PHASE 2 — PRODUCT & MASTER PRICE MANAGEMENT

#### Objective
Implement category and product master entities, Room DAOs, repositories, and use cases for product creation, editing, soft deletion (`is_active = false`), price updates with mandatory `PriceHistory` logging, and Admin authentication.

#### Implementation Order
1. **Entities & DAOs**:
   - `CategoryEntity`, `ProductEntity`, `PriceHistoryEntity`, `UserEntity`.
   - `CategoryDao`, `ProductDao`, `PriceHistoryDao`, `UserDao`.
2. **Domain Models & Repositories**:
   - Models: `Product`, `Category`, `PriceHistory`, `User`, `Role` (`ADMIN`, `CASHIER`), `ProductKind`, `PricingMethod`, `QuantityType`.
   - Repository interfaces: `ProductRepository`, `PriceRepository`, `AuthRepository`.
   - Data implementations & mappers.
3. **Use Cases**:
   - `GetProductsUseCase`, `SearchProductsUseCase`, `GetProductByBarcodeUseCase`.
   - `CreateProductUseCase` (requires ADMIN role).
   - `UpdateProductUseCase` (requires ADMIN role).
   - `ChangeProductPriceUseCase` (requires ADMIN role; logs `PriceHistory` atomically).
   - `AuthenticateAdminUseCase`, `ValidateAdminPinUseCase`.
4. **Admin UI (Mode Orang Tua)**:
   - Product list with search and filter.
   - Add/Edit product form (supporting barcode input, category selection, price, unit, initial stock input placeholder).
   - Price change screen showing current price and history.

#### Database Changes
- Tables: `categories`, `products`, `price_history`, `users`.
- Indexes: `products.barcode` (unique where not null), `products.name`, `price_history.product_id`.

#### Tests
- Unit: `ChangeProductPriceUseCaseTest` (verifies price update and automatic `PriceHistory` creation; verifies CASHIER rejection).
- Integration: `ProductDaoTest` (uniqueness of barcode, soft deletion filtering, case-insensitive search).

#### Acceptance Criteria
- Admin can create, edit, and deactivate products.
- Barcode must be unique among active products; null barcodes permitted for non-barcoded items.
- Changing product master price creates a `PriceHistory` record with timestamp and admin user ID.
- Cashier role cannot trigger master price changes.

---

### PHASE 3 — CASHIER & PERSISTENT DRAFT CART

#### Objective
Implement the cashier transaction workflow: CameraX + ML Kit debounced barcode scanning, manual search fallback, weighted product selection (0.5 kg increments), persistent Room draft cart (`DRAFT`), and seamless unknown barcode registration without losing cart contents.

#### Implementation Order
1. **Entities & DAOs**:
   - `TransactionEntity` (with `transaction_status = DRAFT`, `payment_status = UNPAID`).
   - `TransactionItemEntity`.
   - `TransactionDao`, `TransactionItemDao`.
2. **Domain Models & Repositories**:
   - `Cart`, `CartItem`, `TransactionItemSnapshot`.
   - `TransactionRepository` (draft cart operations).
3. **Use Cases**:
   - `GetActiveDraftCartUseCase`, `AddProductToCartUseCase`, `UpdateCartItemQuantityUseCase`, `RemoveCartItemUseCase`.
   - `ScanBarcodeUseCase` (debounces camera input by 800ms; auto-aggregates existing cart items).
   - `AddWeightedProductToCartUseCase` (validates 500g increments; calculates subtotal in integer Rupiah: `(grams * pricePerKg) / 1000`).
4. **Presentation & UI**:
   - Cashier Screen: Cart item list, aggregated quantities, item delete, live total calculation.
   - Barcode Scanner Composable (CameraX PreviewView + ML Kit barcode analyzer).
   - Manual Product Search Composable.
   - Weighted Product Dialog (0.5 kg interval buttons).
   - Unknown Barcode Dialog: Prompt "Produk belum terdaftar" with [Daftarkan Produk Baru] navigating to Admin registration, preserving draft cart in Room.

#### Database Changes
- Tables: `transactions`, `transaction_items`.
- Foreign keys: `transaction_items.transaction_id -> transactions.transaction_id`, `transaction_items.product_id -> products.product_id`.

#### Tests
- Unit: `AddProductToCartUseCaseTest` (aggregates quantity on duplicate scan; preserves snapshot price).
- Unit: `WeightedCalculationTest` (verifies integer arithmetic `(grams * pricePerKg) / 1000` for 500g, 1000g, 1500g).
- Integration: Draft cart survives process recreation and navigation to registration and back.

#### Acceptance Criteria
- Scanning an existing barcode adds product to cart or increments quantity.
- Manual search quickly returns matches from local Room DB without network latency.
- Unknown barcode prompts registration; after admin registration, returns directly to draft cart with item auto-added.
- Master price change while product is in cart does NOT alter the captured `unit_price` in the active cart.

---

### PHASE 4 — PAYMENT & TRANSACTION COMPLETION

#### Objective
Implement checkout, cash change calculation, explicit QRIS payment confirmation, atomic transaction finalization, and completed transaction immutability.

#### Implementation Order
1. **Domain Use Cases**:
   - `CalculateCashPaymentUseCase`: validates `amountReceived >= totalAmount`; calculates `change = amountReceived - totalAmount`.
   - `ConfirmQrisPaymentUseCase`: transitions `payment_status` from `UNPAID` to `PAID` after explicit cashier confirmation.
   - `CompleteTransactionUseCase`: atomic Room transaction validating stock, setting `transaction_status = COMPLETED`, snapshotting items, generating `SALE` stock movements, and enqueuing sync records.
2. **Repository Implementation**:
   - Use `RoomDatabase.withTransaction` to guarantee all-or-nothing execution.
3. **Presentation & UI**:
   - Payment Selection Screen (`TUNAI`, `QRIS`).
   - Cash Payment Screen: Cash input, quick nominal chips, live change calculation, disable button if insufficient.
   - QRIS Payment Screen: Waiting state with explicit "PEMBAYARAN BERHASIL" confirmation button.
   - Transaction Success Screen: Displays transaction number, total, change, and [TRANSAKSI BARU] button.

#### Database Changes
- Status columns in `transactions`: `transaction_status` (`DRAFT`, `COMPLETED`, `CANCELLED`), `payment_status` (`UNPAID`, `PAID`).

#### Tests
- Unit: `CalculateCashPaymentTest` (rejects `amountReceived < totalAmount`, exact change calculation).
- Unit: `CompleteTransactionUseCaseTest` (atomically commits transaction, items, stock movements, and sync entries).
- UI/ViewModel: Cashier payment flow validation.

#### Acceptance Criteria
- Cash transaction cannot complete if `amountReceived < totalAmount`.
- QRIS cannot complete without explicit cashier tap on "PEMBAYARAN BERHASIL".
- On completion, local database atomically records transaction, items, `SALE` stock movement, and sync queue entry.
- Completed transactions cannot be physically deleted.

---

### PHASE 5 — INVENTORY & STOCK LEDGER

#### Objective
Implement the authoritative stock ledger via `StockMovement`, including Initial Stock Setup, Stock In with purchase-unit conversion, Stock Opname/Adjustment, and transaction cancellation with `SALE_REVERSAL`.

#### Implementation Order
1. **Entities & DAOs**:
   - `StockMovementEntity`, `StockInEntity`, `StockAdjustmentEntity`.
   - `StockMovementDao` (`SUM(quantity_delta)` query), `StockInDao`, `StockAdjustmentDao`.
2. **Domain Models & Repositories**:
   - `StockMovement` (`INITIAL_STOCK`, `STOCK_IN`, `SALE`, `SALE_REVERSAL`, `ADJUSTMENT`).
   - `InventoryRepository`.
3. **Use Cases**:
   - `GetCurrentStockUseCase`: derives stock from ledger movements.
   - `CreateInitialStockUseCase`: creates `INITIAL_STOCK` movement.
   - `CreateStockInUseCase`: applies `conversion_factor` (e.g. 4 dus * 40 = +160 pcs) and persists factor.
   - `PerformStockAdjustmentUseCase`: takes `systemQuantity` and `physicalQuantity`, calculates `difference = physical - system`, requires mandatory reason.
   - `CancelTransactionUseCase` (Admin only): sets transaction to `CANCELLED`, creates `SALE_REVERSAL` movements for all physical items.
4. **Admin Inventory UI**:
   - Initial Stock Setup Screen.
   - Stock In Screen (purchase quantity, purchase unit, conversion factor, resulting units).
   - Stock Opname Screen (shows system stock, input physical stock, auto-calculated diff, reason selector).
   - Transaction History & Cancellation Screen (detail view with cancellation reason dialog).

#### Database Changes
- Tables: `stock_movements`, `stock_ins`, `stock_adjustments`.
- Indexes: `stock_movements.product_id`, `stock_movements.created_at`.

#### Tests
- Unit: `StockCalculationTest` (derives stock correctly from mixed movement types).
- Unit: `StockInConversionTest` (verifies conversion math and audit record).
- Unit: `StockAdjustmentTest` (verifies `difference = physical - system` and mandatory reason).
- Unit: `CancelTransactionTest` (verifies `SALE_REVERSAL` movement generation).
- Unit: `NegativeStockPreventionTest` (prevents sale if requested quantity > current stock).

#### Acceptance Criteria
- No field `Product.stock` is used as authoritative operational state.
- Stock in correctly converts multi-unit purchases and retains conversion factor.
- Stock opname automatically calculates difference and mandates reason.
- Admin cancellation preserves transaction record and restores stock via `SALE_REVERSAL`.

---

### PHASE 6 — REMOTE SYNCHRONIZATION (GOOGLE SHEETS & APPS SCRIPT)

#### Objective
Implement resilient, asynchronous, offline-first remote backup and sync to Google Sheets via an Apps Script gateway using `SyncQueue`, `WorkManager`, batching, and idempotent UUIDs.

#### Implementation Order
1. **Entities & DAOs**:
   - `SyncQueueEntity` (`queue_id`, `entity_type`, `entity_id`, `operation`, `payload`, `status`, `retry_count`, `last_error`).
   - `SyncQueueDao`.
2. **Network Layer**:
   - Retrofit / OkHttp client for Apps Script Web App HTTPS gateway.
   - DTOs for batch operations: `POST /sync` and `GET /bootstrap`.
3. **WorkManager & Sync Processor**:
   - `SyncWorker` with `NetworkType.CONNECTED` constraint and exponential backoff.
   - `SyncQueueProcessor`: batches pending sync items, transmits to Apps Script, marks items `SYNCED` or increments retry on failure.
4. **Admin Sync UI**:
   - Sync status indicator (synced, pending count, last sync timestamp).
   - Manual [SINKRONKAN SEKARANG] button.
   - Recovery / bootstrap definitions.

#### Database Changes
- Table: `sync_queue`.
- Indexes: `sync_queue.status`, `sync_queue.created_at`.

#### Dependencies
- WorkManager KTX (`2.9.1`).
- Retrofit 2 (`2.11.0`) & Kotlinx Serialization Converter.
- OkHttp 3 / Logging Interceptor.

#### Tests
- Unit: `SyncQueueProcessorTest` (batch payload generation, state transitions).
- Unit: `IdempotencyTest` (verifies retries use identical stable UUIDs).
- Integration: Local transaction remains valid and completed even when remote network sync fails.

#### Acceptance Criteria
- Transaction completion is completely decoupled from network availability.
- Network sync batches operations and respects exponential backoff on errors.
- Sync operations are idempotent; retries do not create duplicate rows on Google Sheets.

---

### PHASE 7 — DIGITAL TRANSACTIONS & SYSTEM HARDENING

#### Objective
Implement digital product transaction recording (pulsa, PLN, PDAM) without affecting physical stock, transaction history filtering, low-stock warnings, and end-to-end regression hardening.

#### Implementation Order
1. **Entities & DAOs**:
   - `DigitalTransactionEntity` (`digital_transaction_id`, `transaction_item_id`, `service_type`, `customer_number`, `nominal`, `provider_reference`, `status`).
   - `DigitalTransactionDao`.
2. **Domain Logic & Use Cases**:
   - `RecordDigitalTransactionUseCase`: records digital item linked to transaction item; asserts zero physical stock movement.
   - Status management: `SUCCESS`, `FAILED`, `PENDING`.
3. **UI & Hardening**:
   - Digital transaction recording dialog in cashier flow.
   - Filterable Transaction History in Admin mode (filter by date, status, payment method).
   - Low-stock badge / indicator on product list based on `minimum_stock`.
   - Comprehensive end-to-end testing across all user flows.

#### Database Changes
- Table: `digital_transactions`.

#### Tests
- Unit: Digital transaction does not produce `StockMovement`.
- Integration: Combined physical + digital cart checkout verification.
- End-to-end cashier workflow verification.

#### Acceptance Criteria
- Digital transactions record external reference and customer number without generating physical inventory movements.
- Transaction history filter correctly partitions transactions by status and date.
- Entire cashier and inventory cycle operates reliably offline.

---

## 5. Phase 1 Detailed Specification & Immediate Next Step

### Phase 1 Exact Deliverables & Files to Create/Modify:

1. **Gradle Build & Dependency Catalog**:
   - `gradle/libs.versions.toml`:
     - Fix `coreKtx` to `1.13.1`.
     - Add Jetpack Compose BOM, Compose UI, Foundation, Material 3, Icons.
     - Add Hilt (`2.51.1`) plugin and dependencies.
     - Add Room (`2.6.1`) runtime, ktx, and compiler with KSP.
     - Add Navigation Compose (`2.8.0`).
     - Add Coroutines Test and Room Testing libraries.
   - `build.gradle.kts`: Add Hilt and KSP plugin declarations.
   - `app/build.gradle.kts`:
     - Apply Hilt, KSP, and Compose compiler plugins.
     - Enable Compose feature (`buildFeatures { compose = true }`).
     - Set Java/JVM target to 17 or 1.8 cleanly.
     - Add Room schema location argument for KSP.
2. **Base Architecture & DI**:
   - `app/src/main/java/com/harmony/tokoharmony/TokoHarmonyApp.kt`: Application class annotated with `@HiltAndroidApp`.
   - `app/src/main/java/com/harmony/tokoharmony/core/common/Result.kt`: Standardized domain result wrapper.
   - `app/src/main/java/com/harmony/tokoharmony/core/common/DispatcherProvider.kt`: Coroutine dispatcher abstraction.
   - `app/src/main/java/com/harmony/tokoharmony/core/di/CoroutinesModule.kt`: Hilt module providing dispatchers.
   - `app/src/main/java/com/harmony/tokoharmony/core/database/AppDatabase.kt`: Room database abstract class with version 1 and schema export.
   - `app/src/main/java/com/harmony/tokoharmony/core/database/entity/UserEntity.kt`: Initial entity for admin/cashier authentication.
   - `app/src/main/java/com/harmony/tokoharmony/core/database/dao/UserDao.kt`: User DAO interface.
   - `app/src/main/java/com/harmony/tokoharmony/core/security/PinManager.kt`: PBKDF2/SHA-256 PIN hashing implementation.
   - `app/src/main/java/com/harmony/tokoharmony/core/di/DatabaseModule.kt`: Room database and DAO Hilt bindings.
3. **Presentation & Navigation**:
   - `app/src/main/java/com/harmony/tokoharmony/ui/theme/Color.kt`, `Theme.kt`, `Type.kt`: App design system tokens.
   - `app/src/main/java/com/harmony/tokoharmony/navigation/Screen.kt`: Navigation routes.
   - `app/src/main/java/com/harmony/tokoharmony/navigation/AppNavigation.kt`: NavHost definition.
   - `app/src/main/java/com/harmony/tokoharmony/feature/home/HomeScreen.kt`: Home mode selection UI.
   - `app/src/main/java/com/harmony/tokoharmony/MainActivity.kt`: Main Android entry point.
4. **Verification**:
   - `app/src/test/java/com/harmony/tokoharmony/core/security/PinManagerTest.kt`: Unit test for PIN hash validation.
   - Run `.\gradlew testDebugUnitTest` and `.\gradlew assembleDebug` to verify clean build.

### Exact Next Implementation Step:
Upon user approval of this implementation plan, begin **Phase 1: Foundation**, starting with updating `gradle/libs.versions.toml`, `build.gradle.kts`, and `app/build.gradle.kts` to resolve the `coreKtx` build blocker and install Compose, Hilt, Room, and Navigation.
