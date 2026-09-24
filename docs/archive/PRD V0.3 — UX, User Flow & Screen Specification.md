# PRD V0.3 — UX, User Flow & Screen Specification

## 1. UX Principles

Aplikasi harus mengikuti prinsip berikut:

1. Transaksi harus membutuhkan sesedikit mungkin langkah.
2. Fitur yang paling sering digunakan harus selalu mudah dijangkau.
3. Kasir tidak boleh perlu menghafal harga.
4. Kasir tidak boleh perlu memahami struktur database.
5. Kesalahan sebelum pembayaran harus mudah diperbaiki.
6. Kesalahan setelah transaksi selesai harus dapat ditangani melalui mekanisme pembatalan.
7. Data penting seperti harga, stok, dan histori harus dilindungi dari perubahan tidak sengaja.
8. Koneksi internet tidak boleh menjadi dependency utama transaksi.

---

# 2. Application Entry Flow

Saat aplikasi dibuka:

```text
┌──────────────────────────────┐
│         NAMA TOKO            │
│                              │
│      ┌────────────────┐      │
│      │   MODE KASIR   │      │
│      └────────────────┘      │
│                              │
│      ┌────────────────┐      │
│      │ MODE ORANG TUA │      │
│      └────────────────┘      │
│                              │
└──────────────────────────────┘
```

Mode Kasir tidak membutuhkan autentikasi tambahan.

Mode Orang Tua memerlukan PIN.

---

# 3. Mode Kasir

Halaman utama Mode Kasir:

```text
┌────────────────────────────────┐
│ ←  TRANSAKSI BARU              │
├────────────────────────────────┤
│                                │
│ [ 🔍 Cari produk ] [ 📷 Scan ] │
│                                │
│ ────────────────────────────── │
│ Keranjang                      │
│                                │
│ Belum ada barang               │
│                                │
│                                │
│ ────────────────────────────── │
│ TOTAL                          │
│ Rp0                            │
│                                │
│ [ PROSES PEMBAYARAN ]          │
└────────────────────────────────┘
```

Fungsi utama:

- Search Product.
- Scan Barcode.
- Lihat Cart.
- Proses Payment.

---

# 4. Barcode Scanning

Ketika kasir menekan Scan:

```text
┌──────────────────────────────┐
│ ← Scan Barcode               │
│                              │
│      ┌──────────────┐        │
│      │              │        │
│      │  SCAN AREA   │        │
│      │              │        │
│      └──────────────┘        │
│                              │
│ Arahkan barcode ke area      │
│ scanning                     │
└──────────────────────────────┘
```

Setelah barcode berhasil dibaca:

```text
Barcode
   ↓
Cari Product berdasarkan barcode
```

## Barcode ditemukan

Produk langsung dimasukkan ke cart.

Jika produk sudah terdapat di cart:

```text
quantity += 1
```

Contoh:

```text
Indomie × 2
```

bukan:

```text
Indomie × 1
Indomie × 1
```

---

# 5. Unknown Barcode

Jika barcode tidak ditemukan:

```text
┌──────────────────────────────┐
│ Produk belum terdaftar       │
│                              │
│ Barcode:                     │
│ 899xxxxxxxxxxx               │
│                              │
│ [ DAFTARKAN PRODUK BARU ]    │
│ [ BATAL ]                    │
└──────────────────────────────┘
```

Ketika memilih `Daftarkan Produk Baru`:

```text
Unknown Barcode
       ↓
Admin Authentication
       ↓
Product Registration
```

Keranjang transaksi yang sedang berjalan harus tetap tersimpan.

Setelah produk berhasil didaftarkan:

```text
Product Created
       ↓
Return to Existing Cart
       ↓
Add New Product Automatically
```

Kasir tidak perlu memulai transaksi dari awal.

---

# 6. Product Registration

Form:

```text
┌──────────────────────────────┐
│ PRODUK BARU                  │
├──────────────────────────────┤
│ Barcode                      │
│ [899xxxxxxxxxxx]             │
│                              │
│ Nama Produk                  │
│ [.........................]  │
│                              │
│ Kategori                     │
│ [ Pilih kategori ▼ ]         │
│                              │
│ Jenis                        │
│ [ Fisik ▼ ]                  │
│                              │
│ Metode Harga                 │
│ [ Per Unit ▼ ]               │
│                              │
│ Satuan Penjualan             │
│ [ pcs ▼ ]                    │
│                              │
│ Harga                        │
│ [ Rp ................. ]     │
│                              │
│ Stok Awal                    │
│ [ ................. ]        │
│                              │
│ [ SIMPAN PRODUK ]            │
└──────────────────────────────┘
```

Untuk barcode yang sudah ditemukan dari scanner:

```text
Barcode
```

diisi otomatis dan tidak perlu diketik ulang.

---

# 7. Manual Search

Selain barcode, kasir dapat memilih:

```text
[ 🔍 Cari produk ]
```

Search screen:

```text
┌──────────────────────────────┐
│ ← Cari Produk                │
├──────────────────────────────┤
│ [ Indomie______________ ]    │
├──────────────────────────────┤
│                              │
│ Indomie Goreng               │
│ Rp4.000 / pcs                │
│                              │
│ Indomie Soto                 │
│ Rp4.000 / pcs                │
│                              │
│ Indomie Ayam Bawang          │
│ Rp4.000 / pcs                │
│                              │
└──────────────────────────────┘
```

Search harus mendukung:

- pencocokan sebagian nama.
- case insensitive.
- hasil cepat dari Local Database.

Search tidak bergantung pada internet.

---

# 8. Weighted Product Flow

Untuk Telur/Beras:

```text
Search
   ↓
Telur
   ↓
Pilih Berat
```

Screen:

```text
┌──────────────────────────────┐
│ TELUR                        │
│ Rp30.000 / kg                │
├──────────────────────────────┤
│                              │
│ Pilih berat                  │
│                              │
│ [ 0,5 kg ]   [ 1,0 kg ]      │
│                              │
│ [ 1,5 kg ]   [ 2,0 kg ]      │
│                              │
│ [ 2,5 kg ]   [ 3,0 kg ]      │
│                              │
└──────────────────────────────┘
```

Setelah berat dipilih:

```text
1,5 kg × Rp30.000
= Rp45.000
```

Item langsung masuk ke cart.

---

# 9. Quantity Editing

Setelah produk berada di cart:

```text
Indomie Goreng
[-]  3  [+]
Rp12.000
```

Kasir dapat:

- menambah quantity.
- mengurangi quantity.
- mengetik quantity.
- menghapus item.

Untuk produk timbang:

```text
Telur
1,5 kg
```

Kasir dapat memilih ulang berat.

MVP tidak menyediakan input berat bebas seperti `1,37 kg`.

---

# 10. Cart Screen

```text
┌────────────────────────────────┐
│ TRANSAKSI BARU                 │
├────────────────────────────────┤
│                                │
│ Indomie Goreng                 │
│ 3 × Rp4.000          Rp12.000  │
│ [-] 3 [+]        [Hapus]       │
│                                │
│ Aqua 600ml                     │
│ 2 × Rp4.000          Rp8.000   │
│ [-] 2 [+]        [Hapus]       │
│                                │
│ Telur                          │
│ 1,5 kg × Rp30.000    Rp45.000  │
│ [Ubah Berat]      [Hapus]      │
│                                │
├────────────────────────────────┤
│ TOTAL               Rp65.000   │
│                                │
│ [ PROSES PEMBAYARAN ]          │
└────────────────────────────────┘
```

---

# 11. Empty Cart

Jika belum ada item:

```text
Belum ada barang.

[ SCAN BARCODE ]
[ CARI PRODUK ]
```

Tombol pembayaran disabled ketika cart kosong.

---

# 12. Payment Screen

Setelah kasir memilih `Proses Pembayaran`:

```text
┌──────────────────────────────┐
│ PEMBAYARAN                   │
├──────────────────────────────┤
│ Total                        │
│ Rp65.000                     │
│                              │
│ Metode Pembayaran            │
│                              │
│ [ TUNAI ]     [ QRIS ]       │
└──────────────────────────────┘
```

---

# 13. Cash Payment

```text
┌──────────────────────────────┐
│ PEMBAYARAN TUNAI             │
├──────────────────────────────┤
│ Total                        │
│ Rp65.000                     │
│                              │
│ Uang diterima                │
│ [ Rp100.000 ]                │
│                              │
│ Kembalian                    │
│ Rp35.000                     │
│                              │
│ [ SELESAIKAN TRANSAKSI ]     │
└──────────────────────────────┘
```

Jika uang:

```text
< total
```

tombol penyelesaian disabled.

Contoh:

```text
Total = 65.000
Uang = 60.000
```

Aplikasi menampilkan:

```text
Uang pembayaran kurang
Rp5.000
```

---

# 14. QRIS Payment

```text
┌──────────────────────────────┐
│ PEMBAYARAN QRIS              │
├──────────────────────────────┤
│ Total                        │
│ Rp65.000                     │
│                              │
│ Silakan pelanggan melakukan  │
│ pembayaran melalui QRIS.    │
│                              │
│ [ PEMBAYARAN BERHASIL ]      │
│                              │
│ [ KEMBALI ]                  │
└──────────────────────────────┘
```

Aplikasi tidak menghubungkan dirinya ke provider QRIS.

Kasir bertugas memastikan pembayaran berhasil.

---

# 15. Completing Payment

Saat pembayaran berhasil:

```text
Validate Payment
       ↓
Create Transaction
       ↓
Create Transaction Items
       ↓
Create Stock Movements
       ↓
Commit Local DB
       ↓
Create Sync Queue
       ↓
Transaction Success
```

Semua operasi utama harus berhasil sebagai satu atomic operation.

---

# 16. Transaction Success

Tidak perlu membuat halaman panjang.

Cukup:

```text
┌──────────────────────────────┐
│ ✓ TRANSAKSI BERHASIL         │
│                              │
│ TRX-000123                   │
│                              │
│ Total                        │
│ Rp65.000                     │
│                              │
│ Kembalian                    │
│ Rp35.000                     │
│                              │
│ [ TRANSAKSI BARU ]           │
└──────────────────────────────┘
```

Setelah beberapa saat atau ketika tombol ditekan:

```text
TRANSAKSI BARU
```

---

# 17. Active Cart Protection

Aplikasi harus mempertahankan cart ketika:

- Scanner ditutup.
- Search dibuka dan ditutup.
- Product Registration dilakukan.
- Admin Mode dibuka sementara.

Cart tidak boleh hilang karena navigasi internal.

---

# 18. Price Change During Active Transaction

Contoh:

```text
Cart:
Indomie = Rp4.000
```

Admin kemudian mengubah:

```text
Rp4.000 → Rp4.500
```

Cart tetap:

```text
Indomie = Rp4.000
```

Pada saat payment, sistem dapat menampilkan:

```text
Harga produk telah berubah.

Harga di keranjang:
Rp4.000

Harga saat ini:
Rp4.500

[ TETAPKAN HARGA KERANJANG ]
[ KEMBALI KE KERANJANG ]
```

MVP tetap menggunakan harga yang sudah masuk ke cart.

---

# 19. Transaction History

Mode Orang Tua memiliki:

```text
Riwayat Transaksi
```

Tampilan:

```text
24 Sep 2026

TRX-000125
Rp65.000
Tunai
COMPLETED

TRX-000124
Rp32.000
QRIS
COMPLETED

TRX-000123
Rp45.000
Tunai
CANCELLED
```

Filter sederhana dapat tersedia:

```text
Tanggal
Status
Metode pembayaran
```

Fitur laporan kompleks belum diperlukan.

---

# 20. Transaction Detail

Ketika transaksi dibuka:

```text
TRX-000125
24 Sep 2026 14:32

Indomie Goreng
3 × Rp4.000
Rp12.000

Aqua 600ml
2 × Rp4.000
Rp8.000

Telur
1,5 kg × Rp30.000
Rp45.000

-----------------
Total Rp65.000

Pembayaran:
Tunai

Status:
COMPLETED
```

Jika transaksi sudah dibatalkan:

```text
Status:
CANCELLED

Dibatalkan:
24 Sep 2026 15:10

Oleh:
ADMIN

Alasan:
Kesalahan transaksi
```

---

# 21. Cancel Transaction Flow

Mode Orang Tua:

```text
Transaction History
        ↓
Transaction Detail
        ↓
[ BATALKAN TRANSAKSI ]
        ↓
Confirmation
        ↓
Reason
        ↓
Confirm
        ↓
CANCELLED
        ↓
SALE_REVERSAL
```

Confirmation screen:

```text
Batalkan transaksi?

TRX-000125
Total Rp65.000

[ YA, BATALKAN ]
[ KEMBALI ]
```

Setelah konfirmasi:

```text
Reason:
[ Kesalahan transaksi ▼ ]

[ KONFIRMASI PEMBATALAN ]
```

---

# 22. Mode Orang Tua

Dashboard:

```text
┌──────────────────────────────┐
│ MODE ORANG TUA               │
├──────────────────────────────┤
│                              │
│ Produk                       │
│ Harga                        │
│ Stok                         │
│ Transaksi                    │
│                              │
│ Initial Stock Setup          │
│ Barang Masuk                 │
│ Stock Opname                 │
│                              │
│ Sinkronisasi                 │
└──────────────────────────────┘
```

Fungsi admin tidak diletakkan di halaman kasir agar tidak mengganggu operasional.

---

# 23. Product Management

```text
Produk
├── Cari
├── Tambah
├── Edit
└── Nonaktifkan
```

Daftar:

```text
Indomie Goreng
Rp4.000
157 pcs
ACTIVE

Aqua 600ml
Rp4.000
48 botol
ACTIVE

Produk Lama
Rp3.500
0 pcs
INACTIVE
```

---

# 24. Price Management

Admin membuka produk:

```text
Indomie Goreng
Current Price

Rp4.000

[ UBAH HARGA ]
```

Input:

```text
Harga baru:
Rp4.500

[ SIMPAN ]
```

Sistem:

```text
current_price = 4500
```

dan:

```text
PriceHistory:
4000 → 4500
```

---

# 25. Initial Stock Setup

Untuk produk baru:

```text
Produk
Harga
Stok Awal
```

Contoh:

```text
Indomie Goreng

Harga jual:
Rp4.000 / pcs

Stok awal:
120 pcs

[ SIMPAN ]
```

Sistem menghasilkan:

```text
INITIAL_STOCK +120
```

Produk langsung siap dipakai kasir.

---

# 26. Stock In

Admin membuka:

```text
Stok
→ Barang Masuk
→ Tambah
```

Contoh:

```text
Produk:
Indomie Goreng

Satuan beli:
Dus

Jumlah:
4

Isi:
40 pcs

Total:
160 pcs

Catatan:
Pembelian supplier

[ SIMPAN ]
```

---

# 27. Stock Opname

Admin:

```text
Stok
→ Stock Opname
→ Pilih Produk
```

Contoh:

```text
Indomie Goreng

Stok sistem:
157 pcs

Stok fisik:
[155]

Selisih:
-2 pcs

Alasan:
[Barang rusak]

[ SIMPAN KOREKSI ]
```

Selisih dihitung otomatis.

---

# 28. Sync Status

Admin dapat melihat status sinkronisasi:

```text
Sinkronisasi

Status:
✓ Tersinkron

Terakhir sync:
24 Sep 2026 15:31

Pending:
0
```

Jika ada masalah:

```text
⚠ 3 data belum tersinkron

[ SINKRONKAN SEKARANG ]
```

Kasir tidak perlu berinteraksi dengan sync secara normal.

---

# 29. Admin Session

Setelah Admin memasukkan PIN:

```text
ADMIN SESSION
```

tetap aktif selama periode tertentu.

Jika aplikasi tidak digunakan dalam periode tertentu atau user keluar:

```text
Admin Session
    ↓
Locked
```

PIN kembali diperlukan.

Tujuannya agar orang tua tidak harus memasukkan PIN untuk setiap perubahan kecil, tetapi Mode Orang Tua tetap terlindungi.

---

# 30. Navigation Structure

```text
APP
│
├── Home
│   ├── Mode Kasir
│   └── Mode Orang Tua
│
├── Cashier
│   ├── New Transaction
│   │   ├── Scan
│   │   ├── Search
│   │   ├── Weighted Input
│   │   ├── Cart
│   │   └── Payment
│   │
│   └── Success
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

# 31. Error Handling UX

Error harus menjelaskan **apa yang terjadi dan apa yang harus dilakukan**, bukan sekadar:

```text
Error
```

Contoh:

### Barcode tidak ditemukan

```text
Produk belum terdaftar.

Barcode:
899xxxxxxxxxxx

[ DAFTARKAN PRODUK ]
```

### Stok tidak cukup

```text
Stok tidak mencukupi.

Stok tersedia: 2 pcs
Jumlah diminta: 5 pcs
```

### Pembayaran kurang

```text
Pembayaran belum cukup.

Kurang Rp5.000.
```

### Sync gagal

```text
Transaksi berhasil disimpan di perangkat.

Sinkronisasi akan dicoba kembali
ketika koneksi tersedia.
```

---

# 32. UX Rules

### UX-001

Scan barcode adalah metode utama untuk produk ber-barcode.

### UX-002

Manual search selalu tersedia sebagai fallback.

### UX-003

Unknown barcode menyediakan jalur Product Registration.

### UX-004

Current cart tidak boleh hilang ketika user berpindah halaman secara internal.

### UX-005

Product Registration yang dimulai dari transaksi harus kembali ke transaksi yang sama setelah selesai.

### UX-006

Produk baru yang berhasil didaftarkan dapat langsung ditambahkan ke cart.

### UX-007

Kasir tidak melihat menu administrasi harga dan stok.

### UX-008

Payment screen tidak dapat diselesaikan jika validasi payment gagal.

### UX-009

Setelah transaksi sukses, aplikasi kembali ke New Transaction.

### UX-010

Semua pencarian produk berasal dari Local Database.

---

# 33. UX Success Criteria

Aplikasi dianggap memiliki UX yang sesuai apabila:

1. Kasir dapat menemukan produk dengan barcode atau nama.
2. Kasir dapat menambah produk dengan sedikit interaksi.
3. Kasir dapat memperbaiki cart sebelum pembayaran.
4. Kasir dapat menyelesaikan pembayaran tunai dengan perhitungan kembalian otomatis.
5. Kasir dapat mengonfirmasi pembayaran QRIS.
6. Unknown barcode tidak mematikan transaksi yang sedang berlangsung.
7. Produk baru dapat didaftarkan tanpa kehilangan cart.
8. Transaksi baru siap digunakan segera setelah transaksi sebelumnya selesai.
9. Admin dapat mengelola data tanpa mengganggu alur kasir.