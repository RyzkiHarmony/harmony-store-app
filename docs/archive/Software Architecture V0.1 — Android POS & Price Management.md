# Software Architecture V0.1 — Android POS & Price Management

## 1. Architectural Goals

Arsitektur aplikasi harus:

1. Memprioritaskan kecepatan transaksi kasir.
2. Tetap dapat digunakan ketika internet tidak tersedia.
3. Memisahkan UI dari business logic.
4. Menjaga integritas transaksi dan stok.
5. Memungkinkan Google Sheets diganti dengan backend database di masa depan.
6. Memudahkan pengujian setiap bagian sistem.
7. Mendukung pengembangan fitur secara bertahap tanpa membuat satu module/class menjadi terlalu besar.

---

# 2. Technology Stack

## Android

```text
Language        : Kotlin
Platform        : Android
Architecture    : MVVM + Clean Architecture principles
UI              : Jetpack Compose
Dependency DI   : Hilt
Local Database  : Room / SQLite
Background Task : WorkManager
Barcode         : CameraX + ML Kit Barcode Scanning
Networking      : Retrofit / OkHttp
Serialization   : Kotlin Serialization
```

Google Sheets tidak diakses langsung dari setiap fitur aplikasi.

Arsitektur remote:

```text
Android
   ↓
Remote Data Source
   ↓
Google Apps Script Web App
   ↓
Google Sheets
```

Apps Script berfungsi sebagai lightweight gateway/API untuk Google Sheets.

---

# 3. High-Level Architecture

```text
┌───────────────────────────────────────────┐
│                 PRESENTATION              │
│                                           │
│  Cashier UI     Admin UI     Auth UI      │
│       │              │          │         │
│       ▼              ▼          ▼         │
│  ViewModel / State Management             │
└──────────────────────┬────────────────────┘
                       │
                       ▼
┌───────────────────────────────────────────┐
│                  DOMAIN                   │
│                                           │
│  Use Cases                                │
│  - AddProductToCart                       │
│  - ScanBarcode                            │
│  - CompleteTransaction                    │
│  - ChangePrice                            │
│  - AddStock                               │
│  - AdjustStock                            │
│  - CancelTransaction                      │
│                                           │
│  Domain Models                            │
│  Repository Interfaces                    │
└──────────────────────┬────────────────────┘
                       │
                       ▼
┌───────────────────────────────────────────┐
│                    DATA                   │
│                                           │
│  Repository Implementations               │
│       │                                   │
│       ├───────────────┐                   │
│       ▼               ▼                   │
│  Local Data       Remote Data             │
│  Room/SQLite      Apps Script API         │
└──────────┬───────────────┬────────────────┘
           │               │
           ▼               ▼
      Local DB       Google Sheets
```

---

# 4. Architectural Principle

Aliran utama aplikasi:

```text
UI
 ↓
ViewModel
 ↓
Use Case
 ↓
Repository
 ↓
Local Database
```

Remote synchronization adalah proses terpisah:

```text
Local Database
 ↓
Sync Queue
 ↓
WorkManager
 ↓
Remote API
 ↓
Google Sheets
```

Kasir tidak pernah bergantung pada remote API untuk menjalankan transaksi normal.

---

# 5. Package Structure

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
│   ├── database
│   │   ├── dao
│   │   ├── entity
│   │   └── AppDatabase.kt
│   │
│   ├── network
│   │   ├── api
│   │   ├── model
│   │   └── NetworkModule.kt
│   │
│   ├── security
│   ├── sync
│   │   ├── SyncManager.kt
│   │   ├── SyncWorker.kt
│   │   └── SyncQueueProcessor.kt
│   │
│   └── di
│       └── AppModule.kt
│
├── data
│   ├── local
│   │   ├── datasource
│   │   ├── mapper
│   │   └── dao
│   │
│   ├── remote
│   │   ├── datasource
│   │   ├── api
│   │   ├── mapper
│   │   └── dto
│   │
│   └── repository
│
├── domain
│   ├── model
│   ├── repository
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
│   ├── cashier
│   │   ├── transaction
│   │   ├── scanner
│   │   ├── search
│   │   ├── cart
│   │   └── payment
│   │
│   ├── admin
│   │   ├── dashboard
│   │   ├── product
│   │   ├── price
│   │   ├── inventory
│   │   └── transaction
│   │
│   └── auth
│
└── navigation
    └── AppNavigation.kt
```

---

# 6. Presentation Layer

Presentation hanya menangani:

- UI.
- User interaction.
- UI state.
- Navigation.
- Menampilkan loading/error/success.

Presentation tidak boleh:

```text
menghitung stok
mengubah harga
membuat StockMovement
menulis Google Sheets
```

Contoh:

```text
CashierViewModel
```

tidak langsung:

```text
room.productDao().insert(...)
```

Tetapi:

```text
CashierViewModel
       ↓
CompleteTransactionUseCase
       ↓
TransactionRepository
```

---

# 7. Domain Layer

Domain merupakan pusat business logic.

Contoh use cases:

```text
AddProductToCartUseCase
UpdateCartQuantityUseCase
RemoveCartItemUseCase
CompleteCashTransactionUseCase
CompleteQrisTransactionUseCase
CancelTransactionUseCase
ChangeProductPriceUseCase
RegisterProductUseCase
CreateInitialStockUseCase
CreateStockInUseCase
PerformStockAdjustmentUseCase
GetCurrentStockUseCase
SyncPendingDataUseCase
```

Setiap use case memiliki satu tanggung jawab utama.

---

# 8. Product Flow

Ketika kasir mencari produk:

```text
SearchProduct
      ↓
ProductRepository
      ↓
Local Product DAO
      ↓
Room
```

Tidak ada network call.

---

# 9. Barcode Flow

```text
CameraX
   ↓
ML Kit
   ↓
Barcode String
   ↓
ScanBarcodeUseCase
   ↓
ProductRepository
   ↓
Local DB
```

Jika ditemukan:

```text
Product
 ↓
AddProductToCartUseCase
 ↓
Draft Transaction Item
```

Jika tidak ditemukan:

```text
UNKNOWN_BARCODE
 ↓
Registration Flow
```

---

# 10. Unknown Barcode Flow

```text
Scanner
   ↓
Barcode = 899...
   ↓
Product not found
   ↓
Show Unknown Barcode UI
   ↓
Admin Authentication
   ↓
RegisterProductUseCase
   ↓
ProductRepository
   ↓
Local DB
```

Setelah product berhasil dibuat:

```text
Product Created
      ↓
Return to Existing Draft Transaction
      ↓
Add Product to Cart
```

Draft transaction tidak boleh hilang.

---

# 11. Draft Transaction Architecture

Keranjang tidak hanya disimpan sebagai state UI.

Draft transaction disimpan di Room.

```text
transactions
transaction_items
```

dengan:

```text
transaction_status = DRAFT
payment_status = UNPAID
```

Keuntungannya:

- cart tetap ada ketika berpindah screen.
- cart tetap ada setelah configuration change.
- cart dapat bertahan jika aplikasi mati.
- unknown barcode registration tidak kehilangan transaksi.
- payment flow memiliki state yang jelas.

---

# 12. Cart Flow

```text
Add Product
     ↓
Get current price
     ↓
Create/Update Draft Transaction Item
     ↓
Save to Room
```

Saat item pertama kali masuk cart:

```text
Product.current_price
       ↓
TransactionItem.unit_price
```

Harga tersebut menjadi snapshot.

---

# 13. Quantity Flow

## Unit Product

```text
quantity = 1
quantity = 2
quantity = 3
```

## Weighted Product

Database menggunakan gram:

```text
500
1000
1500
2000
```

UI menampilkan:

```text
0,5 kg
1,0 kg
1,5 kg
2,0 kg
```

Use case menghitung:

```text
subtotal =
(quantity_gram × price_per_kg) / 1000
```

---

# 14. Payment Architecture

Payment UI mengirim:

```text
PaymentMethod
AmountReceived
```

ke use case.

## Cash

```text
Total = 65.000
Received = 100.000

Change = 35.000
```

Validation:

```text
Received >= Total
```

## QRIS

```text
PaymentMethod = QRIS
PaymentStatus = UNPAID
```

Setelah kasir memastikan pembayaran:

```text
ConfirmQrisPaymentUseCase
```

mengubah:

```text
PaymentStatus
UNPAID → PAID
```

kemudian transaksi dapat diselesaikan.

---

# 15. Complete Transaction Use Case

Ini adalah use case paling kritis dalam aplikasi.

```text
CompleteTransactionUseCase
```

Proses:

```text
1. Load Draft Transaction
2. Validate Cart
3. Validate Product Status
4. Validate Stock
5. Validate Payment
6. Recalculate Total
7. Create/Update Transaction
8. Create Transaction Items
9. Create SALE Stock Movements
10. Change Transaction Status → COMPLETED
11. Change Payment Status → PAID
12. Create Sync Queue
13. Commit
```

Semua perubahan database lokal dilakukan sebagai satu Room transaction.

---

# 16. Transaction Atomicity

Conceptual:

```text
BEGIN DB TRANSACTION

Validate
Create Transaction
Create Items
Create Stock Movements
Create Sync Queue

COMMIT
```

Jika terjadi error:

```text
ROLLBACK
```

Tidak boleh terjadi:

```text
Transaction = COMPLETED
Stock Movement = missing
```

atau:

```text
Stock reduced
Transaction = failed
```

---

# 17. Price Management Architecture

Admin:

```text
ChangePriceUseCase
        ↓
Validate Admin Role
        ↓
Read Current Price
        ↓
Update Product.current_price
        ↓
Create PriceHistory
        ↓
Create Sync Queue
```

Harga dapat diubah berkali-kali dalam sehari.

---

# 18. Stock Architecture

Stock tidak diedit secara langsung.

## Initial Stock

```text
CreateInitialStockUseCase
       ↓
StockMovement(INITIAL_STOCK)
```

## Stock In

```text
CreateStockInUseCase
       ↓
StockIn record
       ↓
StockMovement(STOCK_IN)
```

## Sale

```text
CompleteTransactionUseCase
       ↓
StockMovement(SALE)
```

## Cancellation

```text
CancelTransactionUseCase
       ↓
StockMovement(SALE_REVERSAL)
```

## Physical Adjustment

```text
PerformStockAdjustmentUseCase
       ↓
Calculate difference
       ↓
StockAdjustment record
       ↓
StockMovement(ADJUSTMENT)
```

---

# 19. Stock Calculation

Source of truth:

```text
StockMovement
```

Query:

```text
SUM(quantity_delta)
```

Contoh:

```text
INITIAL_STOCK +160
SALE            -3
SALE            -2
STOCK_IN        +40
ADJUSTMENT       -1
SALE_REVERSAL    +2
--------------------
CURRENT STOCK   196
```

---

# 20. Repository Layer

Repository menjadi abstraction antara domain dan data source.

Contoh:

```text
ProductRepository
TransactionRepository
InventoryRepository
PriceRepository
UserRepository
SyncRepository
```

Domain hanya mengetahui interface:

```text
interface ProductRepository
```

bukan:

```text
RoomProductDao
GoogleSheetsApi
```

Implementasi berada di Data Layer.

---

# 21. Product Repository

```text
ProductRepository
       │
       ├── LocalProductDataSource
       │        ↓
       │      Room
       │
       └── RemoteProductDataSource
                ↓
           Apps Script API
```

Untuk pencarian kasir:

```text
ProductRepository
       ↓
Local only
```

Untuk sinkronisasi:

```text
Sync Manager
       ↓
Remote Data Source
```

---

# 22. Sync Architecture

Sync tidak boleh berada di ViewModel.

Gunakan WorkManager:

```text
Transaction Completed
        ↓
Sync Queue created
        ↓
WorkManager
        ↓
SyncWorker
        ↓
SyncQueueProcessor
        ↓
Remote API
        ↓
Google Sheets
```

WorkManager bertugas menjalankan pekerjaan background yang dapat ditunda sampai kondisi yang sesuai tersedia.

---

# 23. Sync Trigger

Sync dapat dipicu:

```text
1. Setelah transaksi selesai
2. Setelah perubahan data admin
3. Saat koneksi internet kembali
4. Saat aplikasi dibuka
5. Secara berkala
```

Tidak perlu dilakukan pada setiap barcode scan.

---

# 24. Sync Queue

Contoh:

```text
queue_id = Q-001
entity_type = TRANSACTION
entity_id = TRX-001
operation = CREATE
status = PENDING
retry_count = 0
```

Setelah berhasil:

```text
status = SYNCED
```

Jika gagal:

```text
status = FAILED
retry_count++
last_error = ...
```

---

# 25. Idempotent Sync

Setiap entity memiliki UUID.

Contoh:

```text
transaction_id =
550e8400-e29b-41d4-a716-446655440000
```

Retry menggunakan ID yang sama.

Server/gateway harus melakukan:

```text
UPSERT by ID
```

atau memeriksa ID sebelum append.

Tujuannya mencegah:

```text
TRX-001
TRX-001
TRX-001
```

akibat retry.

---

# 26. Remote Architecture

Saya merekomendasikan:

```text
Android
   │
   │ HTTPS
   ▼
Google Apps Script Web App
   │
   ▼
Google Sheets
```

Android tidak menggunakan credential Google Sheets secara langsung dalam operasi kasir.

Apps Script menyediakan endpoint ringan seperti:

```text
POST /sync
GET  /bootstrap
```

### POST /sync

Menerima batch:

```text
{
    products: [...],
    transactions: [...],
    transactionItems: [...],
    stockMovements: [...],
    priceHistory: [...]
}
```

### GET /bootstrap

Digunakan untuk:

- restore.
- initial synchronization.
- recovery device.

---

# 27. Why Apps Script Gateway

Direct:

```text
Android → Google Sheets API
```

membuat aplikasi harus menangani autentikasi Google dan permission Google Sheets secara langsung.

Dengan gateway:

```text
Android → Apps Script → Sheets
```

aplikasi memiliki remote interface yang lebih sederhana.

Selain itu, nanti ketika Google Sheets diganti:

```text
Apps Script → PostgreSQL
```

atau:

```text
REST API → PostgreSQL
```

Android tidak perlu mengubah seluruh domain/business logic.

---

# 28. Sync Direction

Normal operation:

```text
LOCAL → GOOGLE SHEETS
```

menjadi jalur utama.

Recovery:

```text
GOOGLE SHEETS
      ↓
BOOTSTRAP
      ↓
LOCAL DATABASE
```

Google Sheets tidak digunakan sebagai sumber data realtime untuk operasi kasir.

---

# 29. Initial App Setup Flow

Saat aplikasi pertama kali dijalankan:

```text
App Start
   ↓
Check Local Database
   ↓
No Product Data?
   ↓
Admin Setup
   ↓
Create Initial Products
   ↓
Set Price
   ↓
Set Initial Stock
   ↓
Sync
   ↓
Ready for Cashier
```

---

# 30. Initial Stock Setup Flow

Per produk:

```text
Create/Edit Product
       ↓
Current Price
       ↓
Initial Stock
       ↓
Save
```

Contoh:

```text
Indomie Goreng
Price = Rp4.000
Initial Stock = 160 pcs
```

membuat:

```text
INITIAL_STOCK +160
```

---

# 31. Admin Authentication

Mode Orang Tua:

```text
Home
 ↓
Mode Orang Tua
 ↓
PIN
 ↓
Admin Session
```

PIN tidak disimpan plaintext.

Database menyimpan hash/secure representation.

Admin session dapat tetap aktif selama periode tertentu agar tidak mengganggu operasional.

---

# 32. Role Enforcement

Authorization harus dilakukan di **domain/use case**, bukan hanya dengan menyembunyikan tombol UI.

Contoh:

```text
ChangePriceUseCase
```

harus memeriksa:

```text
currentUser.role == ADMIN
```

Walaupun UI kasir secara tidak sengaja memanggil use case tersebut, operasi tetap ditolak.

Hal yang sama berlaku untuk:

```text
CancelTransaction
AdjustStock
RegisterProduct
ChangePrice
```

---

# 33. Navigation

```text
Home
│
├── Cashier
│   └── Transaction
│       ├── Scanner
│       ├── Search
│       ├── Cart
│       └── Payment
│
└── Admin
    ├── Dashboard
    ├── Products
    ├── Price
    ├── Initial Stock
    ├── Stock In
    ├── Stock Opname
    ├── Transactions
    └── Sync
```

---

# 34. Important Data Flow — Normal Sale

```text
Cashier
   ↓
Scan/Search
   ↓
Product
   ↓
Draft Transaction
   ↓
Cart
   ↓
Payment
   ↓
CompleteTransactionUseCase
   ↓
Room Transaction
   ├── Transaction
   ├── TransactionItems
   └── StockMovement(SALE)
   ↓
SyncQueue
   ↓
WorkManager
   ↓
Apps Script
   ↓
Google Sheets
```

---

# 35. Important Data Flow — Unknown Barcode

```text
Camera
   ↓
Barcode
   ↓
Product Not Found
   ↓
Admin Authentication
   ↓
Product Registration
   ↓
Price
   ↓
Initial Stock
   ↓
Product Created
   ↓
Existing Draft Transaction
   ↓
Add New Item
   ↓
Continue Checkout
```

---

# 36. Important Data Flow — Price Change

```text
Admin
   ↓
Change Price
   ↓
ChangePriceUseCase
   ↓
Update Product
   ↓
Create PriceHistory
   ↓
Create SyncQueue
   ↓
Local price immediately updated
   ↓
Sync later
```

---

# 37. Important Data Flow — Stock Opname

```text
Admin
   ↓
Select Product
   ↓
System Stock = 157
   ↓
Input Physical Stock = 155
   ↓
Calculate Difference = -2
   ↓
StockAdjustment
   ↓
StockMovement -2
   ↓
Current Stock = 155
```

---

# 38. Important Data Flow — Cancellation

```text
Admin
   ↓
Transaction Detail
   ↓
Cancel
   ↓
Confirmation + Reason
   ↓
CancelTransactionUseCase
   ↓
Transaction = CANCELLED
   ↓
SALE_REVERSAL
   ↓
Sync Queue
```

---

# 39. Error Boundaries

Error pada setiap layer harus memiliki tanggung jawab yang jelas.

### UI

Menampilkan:

```text
"Stok tidak mencukupi"
"Pembayaran belum dikonfirmasi"
"Produk belum terdaftar"
```

### Domain

Menentukan:

```text
apa yang valid/tidak valid
```

### Data

Menangani:

```text
database error
network error
serialization error
```

### Sync

Menangani:

```text
retry
duplicate detection
pending state
```

---

# 40. Critical Reliability Rules

### Rule 1

Local transaction lebih penting daripada remote sync.

### Rule 2

Remote sync tidak boleh mengubah transaction menjadi gagal.

### Rule 3

Retry sync tidak membuat ID baru.

### Rule 4

Stock calculation berasal dari StockMovement.

### Rule 5

TransactionItem menyimpan snapshot harga.

### Rule 6

Draft transaction disimpan lokal.

### Rule 7

Unknown barcode tidak menghapus draft transaction.

### Rule 8

Permission diperiksa di use case.

### Rule 9

Penyelesaian transaksi menggunakan database transaction.

### Rule 10

Google Sheets tidak menjadi dependency langsung pada alur kasir.

---

# 41. Development Order

Implementasi sebaiknya dilakukan dalam urutan:

```text
Phase 1
Project setup
Room
Hilt
Navigation
Basic UI
```

```text
Phase 2
Product
Category
Admin
Product Registration
Price Management
```

```text
Phase 3
Cashier
Search
Draft Cart
Barcode Scanner
Weighted Product
```

```text
Phase 4
Payment
Transaction
Stock Movement
Cancellation
```

```text
Phase 5
Initial Stock
Stock In
Stock Opname
Stock Adjustment
```

```text
Phase 6
Sync Queue
WorkManager
Apps Script
Google Sheets
```

```text
Phase 7
Digital Transaction Recording
Transaction History
Low Stock
Recovery
```

---

# 42. Testing Strategy

## Unit Test

Test:

```text
price calculation
weighted calculation
change calculation
stock calculation
stock reversal
stock adjustment
permission rules
```

## Repository Test

Test:

```text
Product CRUD
Transaction persistence
Stock Movement
Sync Queue
```

## Integration Test

Test:

```text
Complete transaction
Transaction cancellation
Unknown barcode registration
Initial stock
Stock opname
Sync retry
```

## UI Test

Test:

```text
Scan → Cart → Payment
Search → Cart → Payment
Unknown Barcode → Register → Cart
Cash Payment
QRIS Confirmation
Admin Authentication
```

---

# 43. Architectural Boundary

MVP tidak boleh mempunyai dependency seperti:

```text
Cashier UI → Google Sheets
Cashier UI → Room DAO
ViewModel → Google API
UI → stock calculation
```

Yang diperbolehkan:

```text
UI
 ↓
ViewModel
 ↓
Use Case
 ↓
Repository
 ↓
Data Source
```

---

# 44. Migration Strategy

Saat nanti Google Sheets diganti database:

Saat ini:

```text
RemoteDataSource
     ↓
Apps Script
     ↓
Google Sheets
```

Masa depan:

```text
RemoteDataSource
     ↓
REST API
     ↓
PostgreSQL
```

Domain tetap:

```text
Use Cases
Repositories
Business Rules
```

Sebisa mungkin tetap tidak berubah.

---

# 45. Target Architecture

```text
                         ┌──────────────────┐
                         │   GOOGLE SHEETS  │
                         └────────▲─────────┘
                                  │
                             Apps Script
                                  │
                           ┌──────┴──────┐
                           │ Sync Worker │
                           └──────▲──────┘
                                  │
                         ┌────────┴────────┐
                         │   SYNC QUEUE    │
                         └────────▲────────┘
                                  │
┌─────────────────────────────────┴─────────────────────────┐
│                        ANDROID APP                         │
│                                                           │
│  ┌─────────────────────────────────────────────────────┐  │
│  │                  PRESENTATION                       │  │
│  │                                                     │  │
│  │ Cashier │ Scanner │ Payment │ Admin │ Inventory     │  │
│  └───────────────────────┬─────────────────────────────┘  │
│                          │                                │
│  ┌───────────────────────▼─────────────────────────────┐  │
│  │                     DOMAIN                          │  │
│  │                                                     │  │
│  │ Use Cases │ Rules │ Repository Interfaces           │  │
│  └───────────────────────┬─────────────────────────────┘  │
│                          │                                │
│  ┌───────────────────────▼─────────────────────────────┐  │
│  │                      DATA                           │  │
│  │                                                     │  │
│  │ Repository │ Local Data │ Remote Data               │  │
│  └──────────────┬──────────────────────────────────────┘  │
│                 │                                         │
│            ┌────▼────┐                                    │
│            │ Room DB │                                    │
│            └─────────┘                                    │
└───────────────────────────────────────────────────────────┘
```

# 46. Architectural Summary

Aplikasi menggunakan prinsip:

```text
Local-first
+
MVVM
+
Domain-driven business logic
+
Room persistence
+
WorkManager synchronization
+
Google Sheets as temporary remote repository
```

Kasir bekerja sepenuhnya terhadap Local Database.

Google Sheets berada di luar jalur kritis transaksi.

Business logic tidak ditempatkan di UI.

Stock menggunakan ledger.

Historical price disimpan pada transaction item.

Sync menggunakan UUID dan idempotency.

Struktur ini memungkinkan aplikasi berkembang dari aplikasi toko sederhana menjadi POS yang lebih lengkap tanpa membongkar fondasi.