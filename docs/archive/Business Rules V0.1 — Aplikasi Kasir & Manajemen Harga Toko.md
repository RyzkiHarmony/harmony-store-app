# Business Rules V0.1

## A. Aturan Produk

### BR-001 — Setiap produk memiliki identitas unik

Setiap produk harus memiliki `product_id` unik yang dibuat oleh sistem.

Barcode boleh kosong karena tidak semua produk memiliki barcode.

### BR-002 — Barcode, jika tersedia, harus unik

Satu barcode hanya boleh dimiliki oleh satu produk aktif.

Produk tanpa barcode tetap dapat digunakan melalui pencarian manual.

### BR-003 — Produk yang tidak lagi dijual tidak dihapus secara fisik

Produk yang sudah tidak dijual harus diubah menjadi `INACTIVE`.

Data produk tetap dipertahankan karena mungkin masih direferensikan oleh transaksi lama.

### BR-004 — Produk fisik dan digital diperlakukan berbeda

Produk fisik dapat mempengaruhi stok.

Produk digital tidak mempengaruhi stok fisik.

---

# B. Aturan Satuan dan Stok

### BR-005 — Setiap produk memiliki satu satuan stok utama

Contoh:

```text
Indomie → pcs
Telur → kg
Gula 1 kg → pack
```

Seluruh perhitungan stok menggunakan satuan tersebut.

### BR-006 — Satuan pembelian boleh berbeda dari satuan stok

Contoh:

```text
Dibeli: 4 dus
1 dus: 40 pcs
Stok bertambah: 160 pcs
```

Sistem harus melakukan konversi berdasarkan `conversion_factor`.

### BR-007 — Perpindahan gudang dan display tidak mempengaruhi stok

Perpindahan internal seperti:

```text
Gudang → Display
Display → Gudang
```

tidak dicatat sebagai stock movement.

Sistem hanya mengelola total stok toko.

### BR-008 — Stok tidak boleh diedit langsung

Perubahan stok hanya dapat terjadi melalui:

```text
INITIAL_STOCK
STOCK_IN
SALE
SALE_REVERSAL
ADJUSTMENT
```

### BR-009 — Stok awal wajib ditetapkan sebelum operasi inventory normal

Setiap produk fisik yang akan dikelola stoknya harus memiliki initial stock atau secara eksplisit ditetapkan memiliki stok awal `0`.

### BR-010 — Stock opname menggunakan jumlah fisik aktual

Admin memasukkan:

```text
System Stock
Physical Stock
```

Sistem menghitung:

```text
Adjustment = Physical Stock - System Stock
```

Admin tidak perlu menghitung selisih secara manual.

### BR-011 — Setiap adjustment wajib memiliki alasan

Contoh:

```text
Barang rusak
Barang hilang
Kedaluwarsa
Kesalahan pencatatan
Stock opname
Lainnya
```

### BR-012 — Penjualan tidak boleh membuat stok fisik menjadi negatif

Untuk produk fisik:

```text
quantity yang dijual <= current stock
```

Jika stok tidak mencukupi, transaksi tidak dapat diselesaikan sampai quantity diperbaiki atau stok dikoreksi oleh Admin.

---

# C. Aturan Harga

### BR-013 — Hanya Admin yang dapat mengubah harga master

Kasir hanya dapat melihat harga.

### BR-014 — Harga dapat berubah berkali-kali dalam satu hari

Tidak ada batas jumlah perubahan harga dalam satu hari.

Setiap perubahan harus menghasilkan Price History.

### BR-015 — Harga terbaru menjadi harga aktif

Setelah Admin mengubah harga dan menyimpannya:

```text
current_price = new_price
```

Harga tersebut digunakan untuk item baru yang dimasukkan ke transaksi berikutnya.

### BR-016 — Harga item dikunci ketika produk masuk ke keranjang

Ketika kasir memasukkan produk ke keranjang:

```text
Product.current_price
        ↓
TransactionItem.unit_price
```

Harga tersebut menjadi snapshot harga untuk item tersebut.

Jika Admin mengubah harga setelah item masuk keranjang, harga item yang sudah berada di keranjang tidak otomatis berubah.

### BR-017 — Harga transaksi lama tidak pernah berubah

Perubahan harga produk tidak boleh mengubah harga pada transaksi yang telah dibuat sebelumnya.

### BR-018 — Harga produk timbang disimpan per kilogram

Untuk produk seperti telur dan beras:

```text
current_price = harga per kg
```

Total dihitung:

```text
subtotal = quantity × price_per_kg
```

### BR-019 — Berat produk timbang harus kelipatan 0,5 kg

Nilai yang valid:

```text
0,5
1,0
1,5
2,0
2,5
...
```

Input berat di luar interval tersebut tidak diperbolehkan pada MVP.

### BR-020 — Harga produk timbang tidak memiliki harga khusus per berat

Untuk sementara:

```text
0,5 kg = 50% harga/kg
1,5 kg = 150% harga/kg
2,0 kg = 200% harga/kg
```

Contoh:

```text
Rp30.000/kg × 1,5 kg
= Rp45.000
```

---

# D. Aturan Keranjang

### BR-021 — Produk yang sama digabungkan

Jika kasir memasukkan produk yang sama dua kali:

```text
Indomie × 2
```

sistem memperbarui quantity, bukan membuat dua baris item terpisah.

### BR-022 — Kasir dapat mengubah quantity sebelum pembayaran

Quantity dapat:

```text
naik
turun
diubah langsung
```

Selama transaksi belum selesai.

### BR-023 — Kasir dapat menghapus item sebelum pembayaran

Item yang dihapus dari keranjang belum menghasilkan stock movement.

### BR-024 — Total transaksi selalu dihitung oleh sistem

Kasir tidak memasukkan total secara manual.

Formula:

```text
item subtotal
= quantity × unit_price

transaction total
= SUM(all item subtotal)
```

---

# E. Aturan Pembayaran

### BR-025 — Metode pembayaran MVP hanya CASH dan QRIS

Tidak ada metode lain pada MVP.

### BR-026 — Pembayaran tunai harus memiliki nominal uang diterima

Contoh:

```text
Total       = Rp65.000
Diterima    = Rp100.000
Kembalian   = Rp35.000
```

### BR-027 — Pembayaran tunai yang kurang tidak dapat diselesaikan

Contoh:

```text
Total       = Rp65.000
Diterima    = Rp60.000
```

Sistem menolak penyelesaian transaksi sampai uang diterima minimal Rp65.000.

### BR-028 — Kembalian dihitung otomatis

```text
change = amount_received - total_amount
```

Kasir tidak menghitung kembalian secara manual melalui sistem.

### BR-029 — QRIS memerlukan konfirmasi kasir

Pemilihan `QRIS` saja tidak membuat transaksi berhasil.

Kasir harus menekan:

```text
PEMBAYARAN BERHASIL
```

setelah memastikan pembayaran benar-benar diterima.

### BR-030 — Transaksi QRIS yang belum dikonfirmasi belum menjadi transaksi selesai

Transaksi baru berstatus `COMPLETED` setelah konfirmasi pembayaran.

---

# F. Aturan Penyelesaian Transaksi

### BR-031 — Transaksi disimpan sebagai satu kesatuan

Penyelesaian transaksi harus menghasilkan secara konsisten:

```text
Transaction
+
Transaction Items
+
Stock Movements
```

Tidak boleh terjadi kondisi transaksi selesai tetapi stock movement tidak dibuat.

### BR-032 — Stock movement SALE hanya dibuat untuk transaksi COMPLETED

Transaksi yang belum selesai tidak mengurangi stok.

### BR-033 — Produk digital tidak menghasilkan stock movement

Transaksi pulsa, kuota, PLN, dan PDAM tidak mengurangi stok fisik.

### BR-034 — Setelah transaksi berhasil, aplikasi kembali ke transaksi baru

Flow:

```text
Payment Success
      ↓
Save Transaction
      ↓
Update Local Stock
      ↓
Queue Sync
      ↓
New Transaction
```

---

# G. Aturan Pembatalan Transaksi

### BR-035 — Transaksi selesai tidak boleh dihapus

Transaksi harus tetap tersimpan untuk histori.

### BR-036 — Pembatalan mengubah status transaksi menjadi CANCELLED

Contoh:

```text
COMPLETED
   ↓
CANCELLED
```

### BR-037 — Pembatalan transaksi menghasilkan stock reversal

Jika transaksi menjual:

```text
Indomie × 3
```

dan dibatalkan:

```text
SALE_REVERSAL
+3
```

Stok kembali.

### BR-038 — Hanya Admin yang dapat membatalkan transaksi

Kasir tidak memiliki akses pembatalan.

### BR-039 — Pembatalan tidak mengubah histori harga

Pembatalan transaksi hanya mempengaruhi status transaksi dan inventory.

---

# H. Aturan Produk Digital

### BR-040 — Produk digital tidak menggunakan inventory fisik

Tidak ada:

```text
stock pulsa
stock kuota
stock token
```

dalam inventory fisik toko.

### BR-041 — Integrasi provider digital bukan bagian MVP

Transaksi digital tetap dilakukan menggunakan aplikasi agen yang sekarang digunakan toko.

Aplikasi toko hanya mencatat transaksi digital apabila diperlukan.

### BR-042 — Transaksi digital memiliki status sendiri

Minimal:

```text
SUCCESS
FAILED
PENDING
```

Transaksi `FAILED` tidak dianggap sebagai penjualan berhasil.

---

# I. Aturan Sinkronisasi

### BR-043 — Database lokal adalah sumber operasional kasir

Kasir tidak bergantung pada Google Sheets untuk mencari produk, menghitung total, atau menyelesaikan transaksi.

### BR-044 — Transaksi disimpan lokal sebelum sinkronisasi

Urutannya:

```text
Save Local
   ↓
Transaction Valid
   ↓
Sync Queue
   ↓
Google Sheets
```

### BR-045 — Kegagalan sinkronisasi tidak membatalkan transaksi

Contoh:

```text
TRANSACTION = COMPLETED
SYNC = PENDING
```

Transaksi tetap valid.

### BR-046 — Sinkronisasi harus idempotent

Transaksi yang sama tidak boleh muncul dua kali di Google Sheets hanya karena proses sync diulang.

Setiap entity memiliki ID unik.

### BR-047 — Google Sheets tidak menjadi tempat input operasional utama

Perubahan produk, harga, stok, dan transaksi dilakukan melalui aplikasi.

Google Sheets digunakan sebagai remote repository sementara dan sarana administrasi/backup.

---

# J. Aturan Mode dan Akses

### BR-048 — Mode Kasir tidak memiliki hak administratif

Mode Kasir hanya dapat melakukan operasi penjualan.

### BR-049 — Mode Orang Tua memerlukan autentikasi

Mode Orang Tua dilindungi PIN/password.

### BR-050 — Perubahan penting harus menyimpan siapa yang melakukan tindakan

Untuk perubahan seperti:

```text
price change
stock adjustment
transaction cancellation
```

sistem menyimpan identitas pengguna dan waktu tindakan.

---

# K. Aturan yang Belum Masuk MVP

Fitur berikut sengaja belum memiliki business rule karena belum diperlukan:

```text
Retur barang
Diskon
Member
Utang pelanggan
Supplier
Purchase Order
Multi-cabang
Multi-device
Integrasi provider pulsa
Printer struk
Akuntansi
```

Fitur tersebut tidak boleh diam-diam ditambahkan ke MVP hanya karena secara teknis memungkinkan.