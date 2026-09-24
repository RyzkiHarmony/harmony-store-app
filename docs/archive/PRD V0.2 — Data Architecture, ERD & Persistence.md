# PRD V0.2 — Data Architecture, ERD & Persistence

## 1. Tujuan Data Architecture

Data architecture dirancang untuk:

1. Mendukung transaksi kasir secara offline.
2. Menjaga histori transaksi dan perubahan harga.
3. Menghitung stok berdasarkan stock movement.
4. Mencegah duplikasi data saat sinkronisasi.
5. Memisahkan data master dari data historis.
6. Memungkinkan migrasi dari Google Sheets ke database backend pada masa mendatang.
7. Memungkinkan pendaftaran produk baru ketika barcode belum tersedia di database.

Arsitektur:

```text
Android Application
        │
        ▼
   Room / SQLite
        │
        ▼
   Sync Queue
        │
        ▼
  Google Sheets
```

Google Sheets berfungsi sebagai **remote repository sementara**, bukan sebagai database operasional utama.

---

# 2. Entity Relationship Diagram

Model relasional utama:

```text
┌──────────────┐
│   Category   │
├──────────────┤
│ PK category_id
│ name         │
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
│ product_kind │
│ pricing_method
│ selling_unit │
│ stock_unit   │
│ purchase_unit
│ conversion_factor
│ current_price
│ minimum_stock
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
│ quantity             │
│ unit_price           │
│ subtotal             │
│ selling_unit         │
└──────────┬───────────┘
           │ N
           │
           │ 1
    ┌──────▼───────────┐
    │    Transaction   │
    ├──────────────────┤
    │ PK transaction_id│
    │ transaction_no   │
    │ payment_method   │
    │ payment_status   │
    │ transaction_status
    │ total_amount     │
    │ amount_received  │
    │ change_amount    │
    │ created_at       │
    │ created_by       │
    └──────────────────┘

TransactionItem 1 ─── 0..1 DigitalTransaction

User 1 ─── N Transaction
User 1 ─── N PriceHistory
User 1 ─── N StockMovement
User 1 ─── N StockIn
User 1 ─── N StockAdjustment
```

---

# 3. Category

## Table: categories

```text
category_id
name
created_at
updated_at
is_active
```

Kategori dapat digunakan kembali oleh berbagai produk.

Contoh:

```text
Makanan
Minuman
Sembako
Perawatan
Lainnya
```

---

# 4. Product

## Table: products

```text
product_id
category_id
name
barcode
product_kind
pricing_method
selling_unit
stock_unit
purchase_unit
purchase_conversion_factor
current_price
minimum_stock
is_active
created_at
updated_at
```

## product_kind

```text
PHYSICAL
DIGITAL
```

## pricing_method

```text
PER_UNIT
PER_KG
```

Barcode bukan jenis produk. Barcode adalah atribut identifikasi yang bersifat opsional.

Contoh:

```text
Indomie Goreng
product_kind = PHYSICAL
pricing_method = PER_UNIT
selling_unit = PCS
stock_unit = PCS
barcode = 899...
```

Telur:

```text
Telur
product_kind = PHYSICAL
pricing_method = PER_KG
selling_unit = KG
stock_unit = KG
barcode = NULL
```

Gula 1 kg:

```text
Gula 1 kg
product_kind = PHYSICAL
pricing_method = PER_UNIT
selling_unit = PACK
stock_unit = PACK
barcode = NULL
```

---

# 5. Product Registration

Produk baru dapat didaftarkan melalui Mode Orang Tua/Admin.

Data minimal:

```text
Nama Produk
Kategori
Barcode (opsional)
Jenis Produk
Metode Harga
Satuan Penjualan
Satuan Stok
Satuan Pembelian
Conversion Factor
Harga
Stok Awal
```

Untuk produk barcode:

```text
barcode
```

harus unik.

Jika produk tidak memiliki barcode:

```text
barcode = NULL
```

Produk tetap dapat digunakan melalui pencarian manual.

---

# 6. Unknown Barcode Flow

Ketika kasir melakukan scanning barcode yang tidak ditemukan:

```text
Camera
   ↓
Barcode detected
   ↓
Search local Product
   ↓
NOT FOUND
```

Aplikasi menampilkan:

```text
Produk belum terdaftar

Barcode:
899xxxxxxxxxxx

[ DAFTARKAN PRODUK BARU ]
[ BATAL ]
```

## Admin Authorization

Pendaftaran produk baru yang menghasilkan master data harus memerlukan otorisasi Admin.

Kasir tidak dapat membuat produk resmi tanpa otorisasi Admin.

### Registration Flow

```text
Unknown Barcode
       ↓
Daftarkan Produk Baru
       ↓
Admin Authentication
       ↓
Product Form
       ↓
Input Product Data
       ↓
Set Price
       ↓
Set Initial Stock
       ↓
Save
       ↓
Product Created
       ↓
Barcode sekarang terdaftar
```

Setelah berhasil disimpan, produk dapat langsung tersedia untuk transaksi yang sedang berlangsung.

### Business Rule

Kasir tidak boleh menetapkan harga master sendiri ketika menemukan barcode baru.

Harga harus ditentukan oleh Admin.

Jika barcode ditemukan saat transaksi tetapi produk belum terdaftar, sistem tidak boleh memperlakukannya sebagai transaksi valid sampai produk memiliki harga dan data minimum yang diperlukan.

---

# 7. Quantity Representation

Untuk produk berbasis kilogram, quantity disimpan sebagai integer dalam gram.

Contoh:

```text
0,5 kg → 500 gram
1,0 kg → 1000 gram
1,5 kg → 1500 gram
2,0 kg → 2000 gram
```

Untuk produk per unit:

```text
1 pcs → 1
2 pcs → 2
3 pcs → 3
```

Hal ini menghindari masalah floating-point.

## Perhitungan Produk Timbang

```text
subtotal =
(quantity_gram × price_per_kg) / 1000
```

Contoh:

```text
1500 × 30000 / 1000
= 45000
```

---

# 8. Price History

## Table: price_history

```text
history_id
product_id
old_price
new_price
changed_at
changed_by
```

Setiap perubahan harga menghasilkan satu record.

Harga dapat berubah berkali-kali dalam satu hari.

Contoh:

```text
3500 → 4000
08:00
```

Kemudian:

```text
4000 → 4500
14:00
```

---

# 9. Transaction

## Table: transactions

```text
transaction_id
transaction_number
transaction_status
payment_status
payment_method
total_amount
amount_received
change_amount
created_at
completed_at
cancelled_at
created_by
cancelled_by
```

## transaction_status

```text
DRAFT
COMPLETED
CANCELLED
```

## payment_status

```text
UNPAID
PAID
```

---

# 10. Transaction Item

## Table: transaction_items

```text
item_id
transaction_id
product_id
product_name_snapshot
quantity
unit_price
subtotal
selling_unit
```

`product_name_snapshot` menjaga histori transaksi tetap konsisten jika nama produk master berubah.

`unit_price` menyimpan harga pada saat item dimasukkan ke transaksi.

---

# 11. Harga di Keranjang

Ketika produk dimasukkan ke keranjang:

```text
products.current_price
        ↓
cart_item.unit_price
        ↓
transaction_items.unit_price
```

Jika Admin mengubah harga setelah item masuk keranjang:

```text
current_price = harga baru
cart_item.unit_price = harga lama
```

Harga item dalam keranjang tidak berubah otomatis.

---

# 12. Stock Movement

## Table: stock_movements

```text
movement_id
product_id
movement_type
quantity_delta
reference_type
reference_id
reason
created_at
created_by
```

## movement_type

```text
INITIAL_STOCK
STOCK_IN
SALE
SALE_REVERSAL
ADJUSTMENT
```

Semua perubahan stok direpresentasikan sebagai movement.

Contoh:

```text
INITIAL_STOCK   +160
SALE              -3
SALE              -2
STOCK_IN          +40
ADJUSTMENT         -1
SALE_REVERSAL      +2
```

---

# 13. Current Stock

`products.current_stock` tidak menjadi sumber kebenaran utama.

Stok dihitung dari Stock Movement:

```text
Current Stock =
SUM(quantity_delta)
```

Secara konseptual:

```text
Initial Stock
+ Stock In
- Sale
+ Sale Reversal
± Adjustment
```

Perpindahan:

```text
Gudang → Display
Display → Gudang
```

tidak menghasilkan movement.

---

# 14. Stock In

## Table: stock_ins

```text
stock_in_id
product_id
purchase_quantity
purchase_unit
conversion_factor
stock_quantity
note
created_at
created_by
```

Contoh:

```text
Produk:
Indomie Goreng

Pembelian:
4 dus

Isi:
40 pcs/dus

Stock:
160 pcs
```

Sistem menghasilkan:

```text
STOCK_IN
+160 pcs
```

Conversion factor yang digunakan saat pembelian disimpan pada record Stock In sehingga histori tidak bergantung pada konfigurasi produk di kemudian hari.

---

# 15. Initial Stock

Initial Stock dilakukan per produk ketika sistem pertama kali digunakan.

Contoh:

```text
Indomie Goreng
Harga: Rp4.000
Stok awal: 120 pcs
```

Sistem menghasilkan:

```text
INITIAL_STOCK
+120
```

Initial Stock bukan sekadar mengisi angka stok.

Initial Stock menjadi bagian dari Stock Ledger.

Setiap produk fisik harus:

- memiliki stok awal yang valid, atau
- secara eksplisit ditetapkan `0`.

---

# 16. Stock Adjustment / Stock Opname

Admin memasukkan kondisi fisik aktual.

Contoh:

```text
Stok sistem: 157
Stok fisik : 155
```

Sistem menghitung:

```text
155 - 157 = -2
```

Kemudian membuat:

```text
ADJUSTMENT
-2
```

Alasan wajib:

```text
Barang rusak
Barang hilang
Kedaluwarsa
Kesalahan pencatatan
Stock opname
Lainnya
```

Admin tidak memasukkan delta secara manual sebagai mekanisme utama.

---

# 17. Transaction Cancellation

Transaksi selesai tidak dihapus.

Status berubah:

```text
COMPLETED
    ↓
CANCELLED
```

Kemudian dibuat:

```text
SALE_REVERSAL
+quantity
```

Histori transaksi tetap tersimpan.

Pembatalan hanya tersedia untuk Admin.

Alasan pembatalan wajib dicatat.

---

# 18. Digital Transaction

## Table: digital_transactions

```text
digital_transaction_id
transaction_item_id
service_type
customer_number
nominal
provider_reference
status
created_at
```

## status

```text
SUCCESS
FAILED
PENDING
```

Produk digital tidak menghasilkan Stock Movement.

Pada MVP, transaksi dilakukan menggunakan aplikasi agen yang sudah digunakan toko.

Aplikasi toko hanya mencatat transaksi digital.

Integrasi provider langsung ditunda.

---

# 19. User

## Table: users

```text
user_id
display_name
role
pin_hash
is_active
created_at
updated_at
```

## role

```text
ADMIN
CASHIER
```

Meskipun saat ini hanya orang tua yang menjadi operator, struktur data mendukung lebih dari satu user.

---

# 20. Sync Queue

## Table: sync_queue

```text
queue_id
entity_type
entity_id
operation
payload
status
retry_count
last_error
created_at
synced_at
```

## status

```text
PENDING
SYNCING
SYNCED
FAILED
```

Contoh:

```text
TRANSACTION
UUID-TRX-001
CREATE
PENDING
```

---

# 21. UUID

Entity yang disinkronkan menggunakan UUID yang dibuat pada perangkat.

Contoh:

```text
transaction_id =
550e8400-e29b-41d4-a716-446655440000
```

ID yang sama digunakan ketika retry.

Tujuannya mencegah duplikasi data ketika sync diulang.

---

# 22. Google Sheets Mapping

Google Sheets berfungsi sebagai remote repository sementara.

## Categories

```text
category_id
name
created_at
updated_at
is_active
```

## Products

```text
product_id
category_id
name
barcode
product_kind
pricing_method
selling_unit
stock_unit
purchase_unit
purchase_conversion_factor
current_price
minimum_stock
is_active
created_at
updated_at
```

## Transactions

```text
transaction_id
transaction_number
transaction_status
payment_status
payment_method
total_amount
amount_received
change_amount
created_at
completed_at
cancelled_at
created_by
cancelled_by
```

## TransactionItems

```text
item_id
transaction_id
product_id
product_name_snapshot
quantity
unit_price
subtotal
selling_unit
```

## PriceHistory

```text
history_id
product_id
old_price
new_price
changed_at
changed_by
```

## StockMovements

```text
movement_id
product_id
movement_type
quantity_delta
reference_type
reference_id
reason
created_at
created_by
```

## StockIns

```text
stock_in_id
product_id
purchase_quantity
purchase_unit
conversion_factor
stock_quantity
note
created_at
created_by
```

## StockAdjustments

```text
adjustment_id
product_id
system_quantity
physical_quantity
difference
reason
note
created_at
created_by
```

## DigitalTransactions

```text
digital_transaction_id
transaction_item_id
service_type
customer_number
nominal
provider_reference
status
created_at
```

---

# 23. Local Database vs Google Sheets

## Local Database

Menangani:

```text
Search
Barcode
Cart
Price Lookup
Calculation
Payment
Transaction
Stock Calculation
```

## Google Sheets

Menangani:

```text
Remote Storage
Backup
Administration
Data Inspection
Data Recovery
```

Google Sheets tidak menjadi dependency langsung pada alur kasir.

---

# 24. Synchronization Strategy

Untuk MVP:

```text
LOCAL → GOOGLE SHEETS
```

menjadi alur utama.

## Transaction Sync

```text
Create Local
     ↓
Commit
     ↓
Create Sync Queue
     ↓
Upload
     ↓
Mark SYNCED
```

Jika internet tidak tersedia:

```text
Transaction = COMPLETED
Sync = PENDING
```

Transaksi tetap sah.

Ketika internet tersedia, Sync Worker mencoba kembali.

---

# 25. Master Data Synchronization

Data master menggunakan pendekatan upsert berdasarkan ID.

```text
Products
Categories
Settings
```

Data historis bersifat append-oriented:

```text
Transactions
TransactionItems
StockMovements
PriceHistory
StockIns
StockAdjustments
DigitalTransactions
```

Data historis tidak boleh ditimpa sembarangan.

---

# 26. Restore / Recovery

Sistem nantinya dapat melakukan:

```text
Google Sheets
      ↓
Restore
      ↓
Local Database
```

Digunakan ketika:

- HP rusak.
- HP diganti.
- aplikasi di-install ulang.
- database lokal hilang.

Restore bukan bagian dari alur kasir harian.

---

# 27. Data Integrity Rules

Sistem harus memastikan:

```text
Product.barcode unik jika tidak NULL.

Transaction.transaction_id unik.

Transaction.transaction_number unik.

TransactionItem.transaction_id valid.

TransactionItem.product_id valid.

StockMovement.product_id valid.

PriceHistory.product_id valid.

DigitalTransaction.transaction_item_id valid.

Unknown barcode tidak dapat menjadi transaksi final
sebelum produk terdaftar dan memiliki harga.

Sync retry menggunakan ID yang sama.

Produk inactive tidak dapat digunakan untuk transaksi baru.

Produk digital tidak menghasilkan stock movement.
```

---

# 28. Transaction Atomicity

Penyelesaian transaksi harus dilakukan dalam satu database transaction.

```text
BEGIN TRANSACTION

1. Validasi cart
2. Validasi product
3. Validasi stock
4. Validasi payment
5. Create Transaction
6. Create TransactionItems
7. Create SALE StockMovements

COMMIT
```

Jika salah satu langkah gagal:

```text
ROLLBACK
```

Tidak boleh terjadi kondisi:

```text
Transaction = COMPLETED
StockMovement = tidak ada
```

atau:

```text
Stock berkurang
Transaction = tidak tersimpan
```

---

# 29. Acceptance Criteria — Product Registration

### Existing Barcode

```text
Given barcode sudah terdaftar
When cashier scans barcode
Then product ditemukan
And product dapat ditambahkan ke cart.
```

### Unknown Barcode

```text
Given barcode belum terdaftar
When cashier scans barcode
Then system detects UNKNOWN_BARCODE
And displays "Produk belum terdaftar".
```

### Registration

```text
Given barcode belum terdaftar
When user chooses "Daftarkan Produk Baru"
Then system requires Admin authorization.
```

```text
Given Admin authorization berhasil
When product information is completed
And Admin saves the product
Then product is created
And barcode becomes associated with the product.
```

### Price Requirement

```text
Given product baru dibuat
When selling price belum ditentukan
Then product cannot be used untuk transaksi final.
```

### Duplicate Barcode

```text
Given barcode sudah dimiliki product lain
When Admin attempts to save another product with the same barcode
Then system rejects the registration.
```

### Continue Current Transaction

Setelah produk baru berhasil dibuat dan memiliki harga valid:

```text
Unknown Barcode
      ↓
Register Product
      ↓
Admin saves
      ↓
Product available
      ↓
Return to current cart
```

Jika memungkinkan, aplikasi dapat langsung memasukkan produk tersebut ke cart agar kasir tidak perlu melakukan scan ulang.

---

# 30. Acceptance Criteria — Price

```text
Given current price = Rp4.000
When Admin changes price to Rp4.500
Then current price becomes Rp4.500
And PriceHistory is created.
```

Harga dapat diubah berkali-kali dalam satu hari.

---

# 31. Acceptance Criteria — Transaction

```text
Given product price = Rp4.000
When product is added to cart
And Admin changes master price to Rp4.500
Then existing cart item remains Rp4.000.
```

---

# 32. Acceptance Criteria — Stock

```text
Given stock = 160 pcs
When transaction sells 3 pcs
Then SALE movement = -3
And current stock = 157 pcs.
```

---

# 33. Acceptance Criteria — Cancellation

```text
Given completed transaction sold 3 pcs
When Admin cancels transaction
Then transaction remains stored
And status becomes CANCELLED
And SALE_REVERSAL +3 is created.
```

---

# 34. Acceptance Criteria — Stock Adjustment

```text
Given system stock = 157
When physical stock = 155
Then system calculates difference = -2
And ADJUSTMENT -2 is created.
```

---

# 35. Acceptance Criteria — Sync

```text
Given transaction is completed
When internet is unavailable
Then transaction remains locally completed
And sync status becomes PENDING.
```

```text
When internet becomes available
Then the same transaction ID is synced
And duplicate transaction is not created.
```

---

# 36. Prinsip Architecture

```text
Local Database
    ↓
Operational Source

Google Sheets
    ↓
Remote Repository

Stock Movement
    ↓
Source of Stock History

TransactionItem.unit_price
    ↓
Source of Historical Transaction Price
```

Sistem harus menjaga pemisahan antara:

```text
Master Data
Historical Data
Operational State
Synchronization State
```

agar setiap bagian dapat dikembangkan secara independen.