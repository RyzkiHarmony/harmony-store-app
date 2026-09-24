# Software & Data Architecture — Toko Kasir Android POS (Toko Harmony)

## Overview & Authority

Dokumen ini merupakan spesifikasi arsitektur kanonikal untuk aplikasi Android POS Toko Kasir (`Toko Harmony`).
Dokumen ini menggabungkan dan merekonsiliasi:
- **Software Architecture V0.1**: Pola desain sistem, Clean Architecture, MVVM, use cases, alur data, error boundaries, dan strategi sinkronisasi.
- **Data Architecture & Persistence (PRD V0.2)**: ERD, skema tabel Room / SQLite, pemetaan Google Sheets, atomisitas transaksi, dan data integrity rules.

Hirarki otoritas:
1. `docs/BUSINESS_RULES.md`
2. `docs/ARCHITECTURE.md` (dokumen ini)
3. `docs/PRD.md`
4. `docs/UX_SPEC.md`
5. `docs/IMPLEMENTATION_PLAN.md`

Root package namespace proyek: `com.harmony.tokoharmony`.

---

# BAGIAN I: SOFTWARE ARCHITECTURE

## 1. Architectural Goals

Arsitektur aplikasi harus:

1. Memprioritaskan kecepatan transaksi kasir.
2. Tetap dapat digunakan secara penuh ketika internet tidak tersedia (offline-first).
3. Memisahkan UI dari business logic secara tegas (Clean Architecture).
4. Menjaga integritas transaksi dan stok (atomic commit & ledger-based stock).
5. Memungkinkan Google Sheets diganti dengan backend REST API / PostgreSQL di masa depan tanpa mengubah domain logic.
6. Memudahkan pengujian otomatis setiap bagian sistem.
7. Mendukung pengembangan fitur secara bertahap tanpa membuat satu module/class menjadi terlalu besar.

---

## 2. Technology Stack

### Android

```text
Language        : Kotlin (1.9.24)
Platform        : Android (minSdk 26, compileSdk 34, targetSdk 34)
Architecture    : MVVM + Clean Architecture principles
UI              : Jetpack Compose + Material 3
Dependency DI   : Hilt (2.51.1)
Local Database  : Room (2.6.1) / SQLite via KSP
Background Task : WorkManager (2.9.1)
Barcode         : CameraX + ML Kit Barcode Scanning
Networking      : Retrofit 2 + OkHttp 3
Serialization   : Kotlinx Serialization
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

## 3. High-Level Architecture

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

## 4. Architectural Principle

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

## 5. Package Structure

Base package: `com.harmony.tokoharmony`

```text
com.harmony.tokoharmony
│
├── core
│   ├── common
│   │   ├── Result.kt
│   │   ├── Error.kt
│   │   └── DispatcherProvider.kt
│   │
│   ├── database
│   │   ├── AppDatabase.kt
│   │   ├── dao/
│   │   └── entity/
│   │
│   ├── network
│   │   ├── api/
│   │   ├── dto/
│   │   └── NetworkModule.kt
│   │
│   ├── security
│   │   ├── PinManager.kt
│   │   └── SessionManager.kt
│   │
│   ├── sync
│   │   ├── SyncManager.kt
│   │   ├── SyncWorker.kt
│   │   └── SyncQueueProcessor.kt
│   │
│   └── di
│       ├── DatabaseModule.kt
│       ├── NetworkModule.kt
│       ├── CoroutinesModule.kt
│       └── RepositoryModule.kt
│
├── data
│   ├── local
│   │   ├── datasource/
│   │   └── mapper/
│   │
│   ├── remote
│   │   ├── datasource/
│   │   ├── dto/
│   │   └── mapper/
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
│       ├── auth/
│       ├── product/
│       ├── cart/
│       ├── transaction/
│       ├── inventory/
│       ├── price/
│       └── sync/
│
├── feature
│   ├── home/
│   ├── auth/
│   ├── cashier
│   │   ├── transaction/
│   │   ├── scanner/
│   │   ├── search/
│   │   ├── cart/
│   │   └── payment/
│   │
│   └── admin
│       ├── dashboard/
│       ├── product/
│       ├── price/
│       ├── inventory/
│       ├── transaction/
│       └── sync/
│
├── ui
│   └── theme/
│
└── navigation
    ├── Screen.kt
    └── AppNavigation.kt
```

---

## 6. Presentation Layer

Presentation hanya menangani:
- UI Composable.
- User interaction.
- UI state (immutable).
- Navigation.
- Menampilkan feedback loading/error/success.

Presentation tidak boleh:
- menghitung stok.
- mengubah harga master.
- membuat `StockMovement`.
- menulis langsung ke Room DAO atau Google Sheets.

---

## 7. Domain Layer

Domain merupakan pusat business logic murni yang tidak memiliki dependensi terhadap Android Framework atau UI:
- Use Cases: Memvalidasi aturan bisnis dan otorisasi sebelum memanggil repository.
- Domain Models: Representasi bisnis independen.
- Repository Interfaces: Abstraksi data layer.

---

## 8. Alur Pencarian Produk

```text
SearchProduct
      ↓
ProductRepository
      ↓
Local Product DAO
      ↓
Room
```

Tidak ada network call pada pencarian kasir.

---

## 9. Alur Barcode Scanning & Debouncing

```text
CameraX
   ↓
ML Kit Barcode Scanning
   ↓
Barcode String (Debounce 800ms)
   ↓
ScanBarcodeUseCase
   ↓
ProductRepository
   ↓
Local DB
```

- Jika barcode ditemukan: Produk dimasukkan ke cart. Jika sudah ada di cart, kuantitas diagregasi (`quantity += 1`).
- Jika barcode tidak ditemukan: Sistem mengarahkan ke alur Unknown Barcode.

---

## 10. Alur Unknown Barcode

```text
Scanner
   ↓
Barcode belum terdaftar
   ↓
Dialog Unknown Barcode
   ↓
[ Daftarkan Produk Baru ]
   ↓
Admin Authentication (PIN)
   ↓
RegisterProductUseCase
   ↓
ProductRepository -> Room
   ↓
Return ke Draft Cart yang Sama
   ↓
Auto-add produk baru ke cart
```

Draft cart yang sedang berjalan tersimpan persisten di Room dan tidak boleh hilang saat membuka pendaftaran produk baru.

---

## 11. Arsitektur Draft Cart Persisten

Keranjang kasir disimpan persisten di database Room:
- Tabel `transactions` dengan `transaction_status = DRAFT`, `payment_status = UNPAID`.
- Tabel `transaction_items` berisi snapshot harga dan kuantitas.

Keuntungan:
1. Cart tidak hilang saat perpindahan screen atau pergantian mode.
2. Cart tidak hilang jika terjadi configuration change atau aplikasi restart.
3. Alur checkout memiliki status transaksi yang deterministik.

---

## 12. Alur Kuantitas & Harga di Keranjang

Saat item dimasukkan ke cart:
- `Product.current_price` diambil dan disalin ke `TransactionItem.unit_price`.
- `TransactionItem.unit_price` menjadi harga snapshot item tersebut.
- Perubahan harga master oleh Admin setelah item masuk cart tidak mengubah harga item di draft cart yang sedang aktif.

---

## 13. Representasi Kuantitas & Perhitungan Timbang

### Produk Satuan (`COUNT`)
- Kuantitas berupa bilangan bulat unit (1, 2, 3 pcs).
- Subtotal: `quantity * unit_price`.

### Produk Timbang (`GRAM`)
- Disimpan sebagai **integer gram** (`Long`: 500, 1000, 1500, 2000 gram).
- UI menampilkan pilihan increment 0,5 kg.
- Harga master disimpan sebagai harga per kilogram (`price_per_kg`).
- Subtotal dihitung dengan integer arithmetic:
  ```text
  subtotal = (quantity_gram * price_per_kg) / 1000
  ```
- Tidak ada floating-point dalam perhitungan uang atau kuantitas tersimpan.

---

## 14. Arsitektur Pembayaran

### Pembayaran Tunai (`CASH`)
- Validasi: `amount_received >= total_amount`.
- Kembalian dihitung otomatis: `change = amount_received - total_amount`.
- Jika uang kurang, tombol penyelesaian dinonaktifkan.

### Pembayaran QRIS (`QRIS`)
- Pemilihan QRIS menempatkan transaksi pada status `payment_status = UNPAID`.
- Kasir wajib menekan konfirmasi "PEMBAYARAN BERHASIL" setelah memverifikasi pembayaran pada rekening toko.
- Hanya setelah konfirmasi kasir, `payment_status` berubah menjadi `PAID` dan transaksi dapat diselesaikan.

---

## 15. Atomisitas Penyelesaian Transaksi (`CompleteTransactionUseCase`)

Penyelesaian transaksi wajib dilakukan dalam satu Room Database Transaction (`RoomDatabase.withTransaction`):

```text
BEGIN TRANSACTION
1. Load & validasi draft transaction
2. Validasi status keaktifan produk
3. Validasi kecukupan stok fisik (quantity <= current_stock)
4. Validasi pembayaran (Cash cukup / QRIS terkonfirmasi)
5. Recalculate total
6. Update Transaction (status = COMPLETED, completed_at = now)
7. Insert TransactionItems
8. Insert StockMovements (movement_type = SALE, delta = -quantity)
9. Insert SyncQueue records
COMMIT
```

Jika terjadi error, seluruh perubahan di-rollback secara otomatis.

---

## 16. Arsitektur Manajemen Stok & Harga

### Harga Master
- Hanya Admin yang dapat mengubah harga master.
- Setiap perubahan harga mengupdate `Product.current_price` dan mencatat satu record di `PriceHistory`.
- Tidak ada batasan frekuensi perubahan harga dalam satu hari.

### Ledger Stok (`StockMovement`)
- Tidak ada field `Product.stock` yang diedit bebas.
- Seluruh mutasi stok dicatat sebagai `StockMovement`:
  - `INITIAL_STOCK`
  - `STOCK_IN`
  - `SALE`
  - `SALE_REVERSAL`
  - `ADJUSTMENT`
- Stok saat ini dihitung:
  ```text
  Current Stock = SUM(quantity_delta)
  = INITIAL_STOCK + STOCK_IN - SALE + SALE_REVERSAL ± ADJUSTMENT
  ```
- Produk digital tidak pernah menghasilkan `StockMovement`.

### Stock In
- Mendukung satuan beli berbeda dari satuan stok dasar.
- Konversi: `stock_quantity = purchase_quantity * conversion_factor`.
- Nilai `conversion_factor` yang digunakan dicatat pada record `StockIn` untuk menjaga auditabilitas historis.

### Stock Opname / Adjustment
- Admin memasukkan `system_quantity` dan `physical_quantity`.
- Sistem menghitung: `difference = physical_quantity - system_quantity`.
- Alasan penyesuaian wajib diisi (rusak, hilang, kedaluwarsa, salah catat, opname, lainnya).

### Pembatalan Transaksi
- Transaksi selesai tidak dihapus secara fisik.
- Status transaksi diubah menjadi `CANCELLED`.
- Sistem membuat `StockMovement` bertipe `SALE_REVERSAL` (+quantity) untuk memulihkan stok fisik.
- Pembatalan hanya dapat dilakukan oleh Admin dan wajib menyertakan alasan.

---

## 17. Remote Synchronization & Google Sheets Gateway

```text
Room DB -> SyncQueue -> WorkManager -> Apps Script -> Google Sheets
```

1. **Local-First**: Transaksi kasir selesai sepenuhnya secara lokal. Kegagalan internet tidak membatalkan transaksi.
2. **Idempotency**: Setiap entitas menggunakan UUID yang dibuat di perangkat. Retry sinkronisasi menggunakan ID yang sama (upsert by ID).
3. **Batching**: Sinkronisasi mengirimkan sekumpulan mutasi sekaligus dalam satu payload batch ke Apps Script.
4. **WorkManager**: Mengelola background worker dengan network constraint `NetworkType.CONNECTED` dan exponential backoff.

---

## 18. Keamanan & Otorisasi

- Hak akses dibagi menjadi `ADMIN` (Orang Tua) dan `CASHIER` (Kasir).
- Mode Orang Tua dilindungi PIN. PIN disimpan dalam bentuk salted PBKDF2WithHmacSHA256 hash, bukan plaintext.
- Otorisasi diverifikasi di **Use Case**, bukan hanya menyembunyikan tombol UI.

---

# BAGIAN II: DATA ARCHITECTURE, ERD & PERSISTENCE

## 19. Entity Relationship Diagram (ERD)

```text
┌──────────────┐
│   Category   │
├──────────────┤
│ PK category_id
│ name         │
│ is_active    │
└──────┬───────┘
       │ 1
       │
       │ N
┌──────▼───────┐
│   Product    │
├──────────────┤
│ PK product_id
│ FK category_id
│ name         │
│ barcode      │
│ product_kind │ (PHYSICAL | DIGITAL)
│ pricing_method (PER_UNIT | PER_KG)
│ quantity_type│ (COUNT | GRAM)
│ selling_unit │
│ stock_unit   │
│ purchase_unit│
│ purchase_conversion_factor
│ current_price│ (Long integer Rupiah)
│ minimum_stock│
│ is_active    │
└──┬─────┬─────┘
   │     │
   │     ├──────────────────────┐
   │     │                      │
   │     ▼                      ▼
   │ ┌───────────────┐   ┌───────────────┐
   │ │ PriceHistory  │   │ StockMovement │
   │ └───────────────┘   └───────┬───────┘
   │                             │
   │     ┌───────────────────────┘
   │     │
   ▼     ▼
┌──────────────────────┐
│   TransactionItem    │
├──────────────────────┤
│ PK item_id           │
│ FK transaction_id    │
│ FK product_id        │
│ product_name_snapshot
│ quantity             │ (Long: unit count atau gram)
│ unit_price           │ (Long: snapshot harga)
│ subtotal             │ (Long: subtotal integer Rupiah)
│ selling_unit         │
└──────────┬───────────┘
           │ N
           │
           │ 1
    ┌──────▼───────────┐
    │    Transaction   │
    ├──────────────────┤
    │ PK transaction_id│ (UUID)
    │ transaction_number (TRX-xxxx)
    │ payment_method   │ (CASH | QRIS)
    │ payment_status   │ (UNPAID | PAID)
    │ transaction_status (DRAFT | COMPLETED | CANCELLED)
    │ total_amount     │ (Long)
    │ amount_received  │ (Long?)
    │ change_amount    │ (Long)
    │ created_at       │
    │ completed_at     │
    │ cancelled_at     │
    │ created_by       │
    │ cancelled_by     │
    │ cancellation_reason
    └──────────────────┘

TransactionItem 1 ─── 0..1 DigitalTransaction

User 1 ─── N Transaction
User 1 ─── N PriceHistory
User 1 ─── N StockMovement
User 1 ─── N StockIn
User 1 ─── N StockAdjustment
```

---

## 20. Skema Tabel Room Database

### Table: `categories`
- `category_id`: String (PK)
- `name`: String
- `is_active`: Boolean
- `created_at`: Long
- `updated_at`: Long

### Table: `products`
- `product_id`: String (PK, UUID)
- `category_id`: String (FK)
- `name`: String
- `barcode`: String? (Unique Index where NOT NULL)
- `product_kind`: String (`PHYSICAL`, `DIGITAL`)
- `pricing_method`: String (`PER_UNIT`, `PER_KG`)
- `quantity_type`: String (`COUNT`, `GRAM`)
- `selling_unit`: String
- `stock_unit`: String
- `purchase_unit`: String?
- `purchase_conversion_factor`: Long?
- `current_price`: Long (Rupiah)
- `minimum_stock`: Long?
- `is_active`: Boolean
- `created_at`: Long
- `updated_at`: Long

### Table: `price_history`
- `history_id`: String (PK, UUID)
- `product_id`: String (FK)
- `old_price`: Long
- `new_price`: Long
- `changed_at`: Long
- `changed_by`: String (FK -> users.user_id)

### Table: `transactions`
- `transaction_id`: String (PK, UUID)
- `transaction_number`: String (Unique)
- `transaction_status`: String (`DRAFT`, `COMPLETED`, `CANCELLED`)
- `payment_status`: String (`UNPAID`, `PAID`)
- `payment_method`: String (`CASH`, `QRIS`)
- `total_amount`: Long
- `amount_received`: Long?
- `change_amount`: Long
- `created_at`: Long
- `completed_at`: Long?
- `cancelled_at`: Long?
- `created_by`: String (FK -> users.user_id)
- `cancelled_by`: String? (FK -> users.user_id)
- `cancellation_reason`: String?

### Table: `transaction_items`
- `item_id`: String (PK, UUID)
- `transaction_id`: String (FK -> transactions.transaction_id)
- `product_id`: String (FK -> products.product_id)
- `product_name_snapshot`: String
- `quantity`: Long (Count unit atau grams)
- `unit_price`: Long (Snapshot harga saat masuk cart)
- `subtotal`: Long (Integer Rupiah)
- `selling_unit`: String

### Table: `stock_movements`
- `movement_id`: String (PK, UUID)
- `product_id`: String (FK -> products.product_id)
- `movement_type`: String (`INITIAL_STOCK`, `STOCK_IN`, `SALE`, `SALE_REVERSAL`, `ADJUSTMENT`)
- `quantity_delta`: Long (Positif untuk penambahan, negatif untuk pengurangan)
- `reference_type`: String? (`TRANSACTION`, `STOCK_IN`, `STOCK_ADJUSTMENT`)
- `reference_id`: String?
- `reason`: String?
- `created_at`: Long
- `created_by`: String (FK -> users.user_id)

### Table: `stock_ins`
- `stock_in_id`: String (PK, UUID)
- `product_id`: String (FK)
- `purchase_quantity`: Long
- `purchase_unit`: String
- `conversion_factor`: Long
- `stock_quantity`: Long
- `note`: String?
- `created_at`: Long
- `created_by`: String

### Table: `stock_adjustments`
- `adjustment_id`: String (PK, UUID)
- `product_id`: String (FK)
- `system_quantity`: Long
- `physical_quantity`: Long
- `difference`: Long
- `reason`: String
- `note`: String?
- `created_at`: Long
- `created_by`: String

### Table: `digital_transactions`
- `digital_transaction_id`: String (PK, UUID)
- `transaction_item_id`: String (FK -> transaction_items.item_id)
- `service_type`: String (Pulsa, Paket Data, Token PLN, PDAM)
- `customer_number`: String
- `nominal`: Long
- `provider_reference`: String?
- `status`: String (`SUCCESS`, `FAILED`, `PENDING`)
- `created_at`: Long

### Table: `users`
- `user_id`: String (PK)
- `display_name`: String
- `role`: String (`ADMIN`, `CASHIER`)
- `pin_hash`: String? (PBKDF2 salted hash)
- `is_active`: Boolean
- `created_at`: Long
- `updated_at`: Long

### Table: `sync_queue`
- `queue_id`: String (PK, UUID)
- `entity_type`: String (`TRANSACTION`, `PRODUCT`, `PRICE_HISTORY`, `STOCK_MOVEMENT`, dll.)
- `entity_id`: String
- `operation`: String (`CREATE`, `UPDATE`, `CANCEL`)
- `payload`: String (JSON)
- `status`: String (`PENDING`, `SYNCING`, `SYNCED`, `FAILED`)
- `retry_count`: Int
- `last_error`: String?
- `created_at`: Long
- `synced_at`: Long?

---

## 21. Pemetaan Google Sheets (Remote Backup)

Google Sheets mencerminkan struktur tabel Room:
1. `Categories`
2. `Products`
3. `Transactions`
4. `TransactionItems`
5. `PriceHistory`
6. `StockMovements`
7. `StockIns`
8. `StockAdjustments`
9. `DigitalTransactions`
10. `Users`

Aturan Sinkronisasi:
- Data Master (`Products`, `Categories`, `Users`) menggunakan pendekatan **Upsert by ID**.
- Data Historis (`Transactions`, `TransactionItems`, `StockMovements`, `PriceHistory`) bersifat **Append-only** dengan identitas UUID unik untuk mencegah duplikasi saat retry.

---

## 22. Data Integrity Rules

1. `Product.barcode` wajib unik di antara produk aktif jika tidak NULL.
2. `Transaction.transaction_id` (UUID) dan `Transaction.transaction_number` wajib unik.
3. `TransactionItem.unit_price` wajib snapshot harga saat dimasukkan, kebal terhadap perubahan harga master selanjutnya.
4. `TransactionItem.product_name_snapshot` wajib mencatat nama produk saat transaksi.
5. Mutasi stok penjualan hanya dibuat saat transaksi berstatus `COMPLETED`.
6. Pembatalan transaksi wajib membuat mutasi `SALE_REVERSAL` yang mengembalikan stok fisik secara persis.
7. Produk digital tidak boleh membuat mutasi stok fisik.
8. Sinkronisasi retry wajib menggunakan UUID entitas yang sama tanpa membuat ID baru.