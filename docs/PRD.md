# PRD V0.1 — Aplikasi Kasir & Manajemen Harga Toko

## 1. Product Overview

Aplikasi Android untuk membantu toko keluarga melakukan transaksi penjualan dengan cepat, mengetahui harga produk tanpa mengandalkan ingatan, mencatat transaksi, serta membangun pengelolaan stok dasar.

Sistem dirancang **mobile-first** dan menggunakan database lokal pada perangkat sebagai sumber data operasional kasir. Google Sheets digunakan sebagai penyimpanan online sementara dan media sinkronisasi data.

### Masalah Utama

Saat ini:

- Harga produk terutama diketahui melalui ingatan ayah/ibu.
- Kasir berpotensi kesulitan mengetahui harga.
- Perhitungan transaksi dilakukan manual.
- Tidak ada pencatatan setiap transaksi.
- Stok belum dihitung berdasarkan data transaksi.
- Produk memiliki karakteristik berbeda: barcode, non-barcode, produk timbang, produk kemasan, dan produk digital.

### Tujuan Produk

1. Memungkinkan kasir menemukan harga produk dengan cepat.
2. Mengurangi kesalahan input harga.
3. Mempercepat proses transaksi.
4. Mencatat transaksi secara otomatis.
5. Mengurangi stok berdasarkan transaksi yang selesai.
6. Memungkinkan orang tua mengelola produk, harga, barang masuk, dan koreksi stok.
7. Membentuk histori perubahan harga dan pergerakan stok.
8. Menyediakan fondasi yang dapat dikembangkan dari Google Sheets ke database backend sungguhan.

---

# 2. User Roles

## 2.1 Kasir

Saat ini yang menjadi kasir adalah ayah dan ibu secara bergantian.

Hak akses:

- Memulai transaksi.
- Scan barcode.
- Mencari produk.
- Menambahkan produk non-barcode.
- Memilih berat produk timbang.
- Mengubah quantity.
- Menghapus item.
- Melihat harga.
- Memproses pembayaran tunai.
- Mengonfirmasi pembayaran QRIS.
- Menyelesaikan transaksi.
- Melihat transaksi yang diperlukan untuk operasional kasir.

Kasir tidak boleh:

- Mengubah harga master.
- Mengubah data produk.
- Mengubah stok secara manual.
- Mengubah histori harga.

## 2.2 Orang Tua / Admin

Hak akses:

- Mengelola produk.
- Mengubah harga.
- Melihat histori harga.
- Mengelola stok.
- Initial Stock Setup.
- Mencatat barang masuk.
- Melakukan stock opname.
- Melakukan koreksi stok.
- Membatalkan transaksi.
- Melihat histori transaksi.

Akses Admin diamankan menggunakan PIN/password.

---

# 3. Mode Aplikasi

Aplikasi memiliki dua mode utama:

```text
HOME
 ├── MODE KASIR
 └── MODE ORANG TUA
```

Mode Kasir harus dirancang untuk penggunaan cepat.

Mode Orang Tua berfungsi sebagai area administrasi.

---

# 4. Master Product

Setiap produk mempunyai data dasar yang dinormalisasi:

```text
Product
- product_id
- category_id
- product_name
- barcode (nullable)
- product_kind (PHYSICAL | DIGITAL)
- pricing_method (PER_UNIT | PER_KG)
- quantity_type (COUNT | GRAM)
- selling_unit
- stock_unit
- purchase_unit (nullable)
- purchase_conversion_factor (nullable)
- current_price (integer Rupiah / Long)
- minimum_stock (optional)
- is_active (Boolean)
- created_at
- updated_at
```

### Klasifikasi Produk

Produk diklasifikasikan menggunakan atribut yang jelas dan terstandarisasi:

1. **product_kind**:
   - `PHYSICAL`: Produk fisik yang mempengaruhi stok toko.
   - `DIGITAL`: Produk digital (pulsa, kuota, token PLN, PDAM) yang tidak mempengaruhi stok fisik.

2. **pricing_method**:
   - `PER_UNIT`: Harga ditetapkan per satuan jual (pcs, botol, pack, dus).
   - `PER_KG`: Harga ditetapkan per kilogram (untuk produk timbang).

3. **quantity_type**:
   - `COUNT`: Kuantitas berupa bilangan bulat unit (1, 2, 3 pcs).
   - `GRAM`: Kuantitas disimpan dalam integer gram (500, 1000, 1500 gram) untuk menghindari floating-point error.

Barcode bukan jenis produk melainkan atribut opsional. Produk tanpa barcode tetap dapat dicari secara manual.

Contoh:

```text
Indomie Goreng
product_kind = PHYSICAL
pricing_method = PER_UNIT
quantity_type = COUNT
selling_unit = pcs
stock_unit = pcs
barcode = 899...
```

```text
Telur
product_kind = PHYSICAL
pricing_method = PER_KG
quantity_type = GRAM
selling_unit = kg
stock_unit = kg
barcode = NULL
```

```text
Gula 1 kg (kemasan)
product_kind = PHYSICAL
pricing_method = PER_UNIT
quantity_type = COUNT
selling_unit = pack
stock_unit = pack
barcode = NULL
```

Produk digital dipisahkan sepenuhnya dari inventory fisik.
Stok operasional tidak disimpan sebagai angka yang bebas diedit pada produk, melainkan dihitung secara dinamis dari histori Stock Movement.

---

# 5. Initial Stock Setup

Saat aplikasi pertama kali digunakan, Admin melakukan pengisian stok awal.

Initial Stock Setup dilakukan **per produk**, setelah data produk dibuat.

Contoh:

```text
Indomie Goreng
Harga jual: Rp4.000 / pcs
Stok awal: 120 pcs

Aqua 600ml
Harga jual: Rp4.000 / botol
Stok awal: 50 botol

Telur
Harga jual: Rp30.000 / kg
Stok awal: 30 kg

Gula 1 kg
Harga jual: Rp18.000 / pack
Stok awal: 40 pack
```

Data tersebut menghasilkan movement:

```text
INITIAL_STOCK
+120 pcs
```

Initial Stock bukan sekadar mengisi angka `stock` pada produk.

Ia harus masuk ke histori stock movement.

Formula:

```text
Current Stock
=
Initial Stock
+ Stock In
- Sales
± Adjustments
```

Setelah Initial Stock Setup selesai, sistem dianggap memasuki kondisi operasional normal.

---

# 6. Stock Management

Sistem tidak menggunakan model:

```text
stock = angka yang bebas diedit
```

Sebaliknya menggunakan Stock Movement.

Jenis movement:

```text
INITIAL_STOCK
STOCK_IN
SALE
ADJUSTMENT
SALE_REVERSAL
```

Contoh:

```text
Indomie Goreng

INITIAL_STOCK    +160
SALE              -3
SALE              -2
STOCK_IN          +40
ADJUSTMENT         -1
```

Current Stock:

```text
194 pcs
```

Perpindahan:

```text
Gudang → Display
Display → Gudang
```

tidak dicatat.

Sistem hanya memperhatikan jumlah stok total toko.

---

# 7. Stock In

Ketika barang baru datang, Admin mencatat barang masuk.

Contoh:

```text
Produk:
Indomie Goreng

Satuan pembelian:
Dus

Jumlah:
4

Isi per dus:
40 pcs

Total:
160 pcs
```

Sistem menyimpan:

```text
STOCK_IN
+160 pcs
```

Produk yang dibeli dan dijual dalam unit yang sama tidak memerlukan conversion.

---

# 8. Stock Adjustment / Stock Opname

Admin tidak mengubah stok melalui field `stock`.

Admin melakukan:

```text
Stock Opname

Produk:
Indomie Goreng

Stok sistem:
157 pcs

Stok fisik:
155 pcs

Selisih:
-2 pcs

Alasan:
Barang rusak
```

Sistem membuat:

```text
ADJUSTMENT
-2 pcs
```

Pilihan alasan:

- Barang rusak.
- Barang hilang.
- Kedaluwarsa.
- Kesalahan pencatatan.
- Stock opname.
- Lainnya.

---

# 9. Price Management

Harga ditetapkan oleh Admin.

Harga dapat berubah beberapa kali dalam satu hari.

Contoh:

```text
08:00 → Rp4.000
10:30 → Rp4.200
15:00 → Rp4.500
```

Harga terbaru menjadi `current_price`.

Setiap perubahan menghasilkan Price History:

```text
PriceHistory
- history_id
- product_id
- old_price
- new_price
- changed_at
- changed_by
```

### Harga transaksi tidak boleh berubah

Ketika transaksi dilakukan:

```text
Product.current_price
        ↓
TransactionItem.unit_price
```

`unit_price` disimpan di transaksi.

Jika harga produk berubah setelah transaksi selesai, transaksi lama tetap menggunakan harga saat transaksi terjadi.

---

# 10. Cashier Transaction Flow

## Step 1 — Transaksi Baru

Kasir membuka:

```text
TRANSAKSI BARU

[ Scan Barcode ]
[ Cari Produk ]

Keranjang:
Belum ada barang

TOTAL
Rp0

[ BAYAR ]
```

---

## Step 2 — Barcode

Kasir mengarahkan kamera HP ke barcode.

```text
Camera
 ↓
Barcode
 ↓
Cari product_id
 ↓
Product
```

Produk langsung masuk ke keranjang.

Jika produk yang sama di-scan kembali:

```text
Indomie Goreng × 2
```

bukan membuat dua item terpisah.

---

# 11. Manual Product Search

Karena barcode bisa gagal atau produk tidak memiliki barcode, tersedia pencarian manual.

```text
[ 🔍 Cari produk ]

Indomie
------------------
Indomie Goreng
Indomie Soto
Indomie Ayam Bawang
```

Kasir dapat memilih produk secara manual.

Pencarian harus mendukung nama produk dan sebaiknya toleran terhadap sebagian nama.

---

# 12. Weighted Product

Digunakan untuk produk yang benar-benar ditimbang, terutama telur dan beras.

Harga produk disimpan sebagai **harga per kilogram**.

Contoh:

```text
Telur
Harga: Rp30.000 / kg
```

Berat yang dapat dipilih kasir menggunakan interval 0,5 kg:

```text
0,5 kg
1,0 kg
1,5 kg
2,0 kg
2,5 kg
...
```

### Business Rule

Untuk sementara:

```text
Harga transaksi = berat × harga per kg
```

Contoh:

```text
0,5 kg × Rp30.000 = Rp15.000
1,0 kg × Rp30.000 = Rp30.000
1,5 kg × Rp30.000 = Rp45.000
2,0 kg × Rp30.000 = Rp60.000
```

Dengan demikian:

```text
0,5 kg = 50% harga/kg
1,5 kg = 150% harga/kg
2,0 kg = 200% harga/kg
```

### Representasi dan Perhitungan Kuantitas

UI menampilkan kuantitas dalam format kilogram desimal (`0,5 kg`, `1,0 kg`, `1,5 kg`, dst.) untuk kemudahan kasir.

Namun, untuk menjamin akurasi dan menghindari floating-point rounding error (sesuai Business Rules BR-018, BR-019 dan Data Architecture):
- Kuantitas produk timbang disimpan di database dan domain sebagai **integer gram** (`Long`: 500, 1000, 1500 gram).
- Harga produk dan total transaksi disimpan sebagai **integer Rupiah** (`Long`).
- Subtotal dihitung:
  ```text
  subtotal = (quantity_gram × price_per_kg) / 1000
  ```

Contoh:
```text
quantity_gram = 1500
price_per_kg  = 30000
subtotal      = (1500 × 30000) / 1000 = 45000 (Rp45.000)
```

### Batasan

Kasir tidak memasukkan berat secara bebas untuk MVP.

Berat dipilih menggunakan increment:

```text
0,5 kg
```

sehingga transaksi mengikuti pola penjualan toko saat ini.

---

# 13. Packaged Weight Product

Gula, tepung, dan produk sejenis sudah dikemas sebelumnya.

Contoh:

```text
Gula 0,5 kg
Gula 1 kg
Gula 1,5 kg
```

Produk tersebut diperlakukan sebagai unit penjualan tersendiri.

Contoh:

```text
Gula 1 kg
selling_unit = pack
```

Kasir tidak perlu memasukkan berat kembali.

Harga tiap kemasan merupakan harga jual produk tersebut.

---

# 14. Cart

Keranjang menampilkan:

```text
Indomie Goreng
3 × Rp4.000
Rp12.000

Aqua 600ml
2 × Rp4.000
Rp8.000

Telur
1,5 kg × Rp30.000
Rp45.000

-----------------------
TOTAL
Rp65.000
```

Sebelum pembayaran kasir dapat:

- menambah quantity.
- mengurangi quantity.
- memasukkan quantity.
- menghapus item.

---

# 15. Payment

Metode pembayaran:

```text
CASH
QRIS
```

## Cash

```text
Total:
Rp65.000

Uang diterima:
Rp100.000

Kembalian:
Rp35.000
```

Sistem otomatis menghitung kembalian.

## QRIS

```text
Total:
Rp65.000

Menunggu pembayaran...

[ PEMBAYARAN BERHASIL ]
```

Transaksi tidak dianggap selesai hanya karena metode QRIS dipilih.

Kasir harus mengonfirmasi bahwa pembayaran benar-benar berhasil.

---

# 16. Transaction Completion

Setelah pembayaran valid:

```text
Transaction
    ↓
COMPLETED
    ↓
Transaction Items saved
    ↓
Stock movement SALE created
    ↓
Sync queue
    ↓
New Transaction
```

Kemudian aplikasi otomatis kembali ke:

```text
TRANSAKSI BARU
```

agar siap digunakan untuk pelanggan berikutnya.

---

# 17. Transaction Data

## Transaction

```text
Transaction
- transaction_id
- transaction_number
- created_at
- payment_method
- total_amount
- amount_received
- change_amount
- status
- created_by
```

Status minimal:

```text
COMPLETED
CANCELLED
```

## Transaction Item

```text
TransactionItem
- transaction_item_id
- transaction_id
- product_id
- quantity
- unit_price
- subtotal
```

---

# 18. Transaction Cancellation

Transaksi yang sudah selesai tidak dihapus.

Contoh:

```text
TRX-000125
Rp35.000
COMPLETED
```

Setelah dibatalkan:

```text
TRX-000125
Rp35.000
CANCELLED
```

Sistem membuat stock reversal:

```text
SALE_REVERSAL
+quantity
```

Dengan demikian stok kembali.

Histori transaksi tetap tersedia.

Pembatalan hanya dapat dilakukan dalam Mode Orang Tua.

---

# 19. Digital Products

Produk digital seperti:

- pulsa.
- paket data.
- token PLN.
- pembayaran PDAM.

tidak diperlakukan sebagai inventory fisik.

Pada tahap awal aplikasi **tidak melakukan integrasi langsung dengan provider/agen digital**.

Alurnya:

```text
Aplikasi agen
      ↓
Transaksi digital dilakukan
      ↓
Hasil transaksi
      ↓
Dicatat di aplikasi toko
```

Data digital transaction minimal:

```text
DigitalTransaction
- transaction_id
- service_type
- customer_number
- nominal
- status
```

Status:

```text
SUCCESS
FAILED
PENDING
```

Integrasi provider dapat menjadi fase pengembangan berikutnya.

---

# 20. Google Sheets

Google Sheets digunakan sebagai remote storage sementara.

Secara konseptual:

```text
Android App
    │
    ├── Local Database
    │
    └── Sync Manager
             │
             ▼
        Google Sheets
```

### Local Database

Digunakan untuk operasi kasir.

Data transaksi disimpan lokal terlebih dahulu.

### Sync

Setelah transaksi tersimpan:

```text
LOCAL
  ↓
SYNC QUEUE
  ↓
GOOGLE SHEETS
```

Jika sync gagal:

```text
Transaction
SYNC_PENDING
```

Transaksi tidak hilang dan dapat dicoba kembali.

---

# 21. Google Sheets Structure

Sheet sementara:

```text
Products
Transactions
TransactionItems
StockMovements
PriceHistory
DigitalTransactions
Settings
```

Google Sheets bukan bagian dari UX kasir.

Kasir hanya berinteraksi dengan aplikasi.

---

# 22. Prinsip Offline-First

Kasir tidak boleh kehilangan kemampuan transaksi hanya karena koneksi internet bermasalah.

Operasi utama yang harus tetap dapat dilakukan secara lokal:

```text
Search Product
Scan Barcode
Cart
Calculate Total
Cash Payment
QRIS Confirmation
Save Transaction
Update Local Stock
```

Sinkronisasi dapat dilakukan kemudian.

---

# 23. MVP Scope

### Wajib

```text
Product Management
Price Management
Barcode Scan
Manual Search
Weighted Product
Packaged Weight Product
Cart
Cash Payment
QRIS Confirmation
Transaction Recording
Transaction Cancellation
Initial Stock Setup
Stock In
Stock Deduction
Stock Adjustment
Stock Opname
Price History
Transaction History
Local Database
Google Sheets Sync
Mode Kasir
Mode Orang Tua
```

### Ditunda

```text
Provider Digital Integration
Printer Thermal
Customer/Member Management
Debt Management
Supplier Management
Multi-store
Multi-cashier
Accounting
Advanced Analytics
Cloud Backend
```

---

# 24. Core Success Criteria

Sistem dianggap berhasil pada tahap MVP apabila:

1. Kasir dapat menemukan produk tanpa harus bertanya kepada orang tua untuk melihat harga.
2. Kasir dapat menyelesaikan transaksi menggunakan barcode maupun pencarian manual.
3. Telur dan beras dapat dijual berdasarkan berat dengan interval 0,5 kg.
4. Harga produk timbang dihitung otomatis berdasarkan harga per kg.
5. Sistem menghitung total dan kembalian dengan benar.
6. QRIS dapat dicatat setelah kasir mengonfirmasi pembayaran.
7. Transaksi selesai tersimpan.
8. Stok otomatis berkurang dari transaksi.
9. Orang tua dapat mengubah harga kapan saja, termasuk beberapa kali dalam satu hari.
10. Histori transaksi tetap menggunakan harga saat transaksi terjadi.
11. Orang tua dapat melakukan stock opname dan koreksi stok.
12. Perubahan dan transaksi dapat disimpan lokal dan disinkronkan ke Google Sheets.
13. Perpindahan barang gudang ↔ display tidak perlu dicatat.

# 25. Prinsip Desain Utama

Aplikasi harus:

- cepat digunakan.
- membutuhkan sedikit langkah.
- tidak memaksa toko mengubah kebiasaan operasional yang tidak penting.
- mengutamakan akurasi harga dan transaksi.
- menyimpan histori perubahan penting.
- memisahkan data master dari data historis.
- memungkinkan migrasi dari Google Sheets ke database backend di masa depan.
- menggunakan satuan dasar yang konsisten untuk perhitungan stok.
- menggunakan harga per unit dasar sebagai dasar perhitungan transaksi.
- menjaga histori transaksi agar tidak berubah ketika harga produk berubah.