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