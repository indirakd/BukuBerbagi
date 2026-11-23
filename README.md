# BukuBerbagi

**Berbagi Buku, Membuka Peluang Membaca**

Aplikasi Android untuk berbagi dan mendonasikan buku secara gratis. Pengguna bisa mengunggah buku yang sudah tidak dipakai, mencari buku di katalog, lalu meminta buku tersebut. Donatur menyetujui permintaan, kedua pihak mengatur serah terima, dan buku berpindah tangan. Tidak ada jual beli dan tidak ada peminjaman.

- Bahasa: Kotlin
- UI: Jetpack Compose (Material 3)
- Arsitektur: MVVM (Screen → ViewModel + StateFlow → Repository → Supabase)
- Backend: Supabase (Auth, PostgreSQL, Storage)
- Library utama: supabase-kt 3.0.3, Ktor 3, Navigation Compose, Coil

---

## 1. Menyiapkan Supabase

1. Buat project baru di <https://supabase.com/dashboard>.
2. Buka **SQL Editor → New query**, lalu jalankan tiga file berikut **secara berurutan** (salin isinya, tempel, klik *Run*):
   1. `supabase/01_schema.sql`: tabel, index, trigger, fungsi RPC, dan Row Level Security
   2. `supabase/02_storage.sql`: bucket `book-images` dan `avatars` beserta policy-nya
   3. `supabase/03_seed.sql`: enam kategori awal
3. Untuk keperluan demo, matikan konfirmasi email: **Authentication → Sign In / Providers → Email → matikan "Confirm email"**. Jika tidak dimatikan, pengguna baru harus mengklik link konfirmasi di email sebelum bisa login.
4. Ambil **Project URL** dan **anon / publishable key** di **Project Settings → API** (atau menu *API Keys*).

### Membuat akun admin
1. Daftar lewat aplikasi, misalnya dengan email `admin@bukuberbagi.id`.
2. Di SQL Editor, jalankan perintah ini (ganti emailnya):
   ```sql
   update public.profiles set role = 'ADMIN' where email = 'admin@bukuberbagi.id';
   ```
3. Logout lalu login lagi. Aplikasi otomatis membuka tampilan admin.

Akun admin tidak bisa dibuat dari aplikasi. Trigger `protect_profile_role` menolak pengguna yang mencoba mengubah perannya sendiri.

---

## 2. Menjalankan di Android Studio

1. Ekstrak zip, lalu buka folder `BukuBerbagi` di Android Studio (**File → Open**).
2. Buat atau edit file `local.properties` di folder utama project (sejajar dengan `settings.gradle.kts`). Contohnya ada di `local.properties.example`:
   ```properties
   sdk.dir=/path/ke/Android/Sdk          # biasanya diisi otomatis oleh Android Studio
   SUPABASE_URL=https://xxxxxxxx.supabase.co
   SUPABASE_ANON_KEY=eyJhbGciOi...        # anon key atau publishable key
   ```
3. Klik **Sync Project with Gradle Files**. Jika Android Studio menawarkan untuk mengunduh SDK Platform 35, setujui.
4. Jalankan di emulator atau HP (minimal Android 8.0 / API 26).

`local.properties` sudah ada di `.gitignore`, jadi kunci Supabase tidak ikut ter-push ke Git.

Versi library dipilih yang sudah stabil dan saling kompatibel (AGP 8.7.3, Kotlin 2.1.0, Compose BOM 2024.12.01, supabase-kt 3.0.3). Jika Android Studio menyarankan *AGP Upgrade Assistant*, project tetap bisa dijalankan tanpa upgrade.

---

## 3. Pembagian fitur per anggota

| | Anggota 1: Book Management | Anggota 2: Catalog Management | Anggota 3: Donation Request |
|---|---|---|---|
| Tabel | `books` | `categories` (+ membaca `books`) | `donation_requests`, `handover_confirmations` |
| Create | Tambah buku + upload foto ke Storage | Tambah kategori (admin) | Ajukan permintaan; jadwal serah terima |
| Read (list) | Buku saya: `LazyColumn` | Katalog: `LazyVerticalGrid`; Kategori: `LazyColumn` | Permintaan saya / Permintaan masuk: `LazyColumn` |
| Update | Edit buku | Edit kategori | Setujui, tolak, batalkan, konfirmasi serah terima (lewat RPC) |
| Delete | Hapus buku (jika belum disetujui) | Hapus kategori (jika tidak dipakai) | Pembatalan memakai status |
| ViewModel | `BookViewModel` | `CatalogViewModel`, `BookDetailViewModel`, `AdminViewModel` | `DonationRequestViewModel` |
| Repository | `BookRepository` | `CatalogRepository` | `DonationRequestRepository` |
| Screen | `MyBooksScreen`, `AddBookScreen`, `EditBookScreen` | `CatalogScreen` (+ pencarian), `BookDetailScreen`, `AdminDashboardScreen`, `CategoryManagementScreen`, `AddCategoryScreen`, `AdminBooksScreen` | `RequestDonationScreen`, `RequestsScreen` (`MyRequestsScreen` + `IncomingRequestsScreen`), `RequestDetailScreen`, `HandoverConfirmationScreen` |
| Folder | `ui/book/` | `ui/catalog/`, `ui/admin/` | `ui/request/` |

**Fitur bersama (semua anggota):**
- Autentikasi: `ui/auth/` (`AuthViewModel`, `SplashScreen`, `LoginScreen`, `RegisterScreen`)
- Profil + upload foto: `ui/profile/` (`ProfileViewModel`, `ProfileScreen`, `EditProfileScreen`)
- Beranda: `ui/home/`
- Navigasi: `ui/navigation/`
- Komponen UI bersama: `ui/components/` (sampul buku 3D, ikon kategori, tombol pil, dll.)

**Upload gambar ke Cloud Storage** terpenuhi dua kali:
- Foto buku → bucket `book-images`, ditampilkan kembali di katalog, detail buku, dan daftar.
- Foto profil → bucket `avatars`.

---

## 4. Aturan bisnis & keamanan (divalidasi di database)

| Aturan | Tempat validasi |
|---|---|
| Tidak bisa meminta buku sendiri | Trigger `validate_new_request` |
| Tidak bisa meminta buku yang tidak tersedia | Trigger `validate_new_request` |
| Tidak ada permintaan aktif ganda untuk buku yang sama | Trigger + unique index `requests_one_active_per_user` |
| Hanya pemilik buku yang bisa menyetujui/menolak | RPC `approve_request` / `reject_request` |
| Satu buku hanya punya satu permintaan disetujui (atomik) | RPC `approve_request` (`FOR UPDATE`) + unique index `requests_one_approved_per_book` |
| Disetujui → buku `RESERVED`, permintaan lain otomatis `REJECTED` | RPC `approve_request` |
| Kedua pihak konfirmasi → permintaan `COMPLETED`, buku `DONATED` | RPC `confirm_handover` |
| Status buku tidak bisa diubah langsung dari aplikasi | Trigger `guard_book_update` |
| Buku dengan permintaan disetujui tidak bisa dihapus | Trigger `guard_book_delete` |
| Kategori yang masih dipakai tidak bisa dihapus | Trigger `guard_category_delete` |
| Pengguna hanya mengubah data miliknya; kategori hanya oleh admin | Row Level Security di semua tabel |
| Upload foto hanya ke folder milik sendiri | Policy `storage.objects` |

Pesan error dari database diawali `BB:` dan ditampilkan apa adanya di aplikasi (lihat `util/Errors.kt`).

---

## 5. Skenario video demo

1. Daftar akun **A** (donatur), lalu tambah buku dengan foto. Tunjukkan baris baru di tabel `books` dan file di bucket `book-images` pada dashboard Supabase.
2. Daftar akun **B**, cari buku tersebut di Katalog (pencarian dan filter kategori), lalu tekan **Minta buku ini**.
3. Coba minta lagi dengan akun B. Aplikasi menolak dengan pesan "Kamu sudah punya permintaan aktif".
4. Login sebagai A, buka **Permintaan → Permintaan masuk**, lalu **Setujui**. Status buku berubah menjadi *Dipesan*.
5. A mengatur jadwal di layar **Serah terima** dan mengonfirmasi penyerahan. B mengonfirmasi penerimaan. Status menjadi *Selesai* dan buku *Didonasikan*.
6. Login sebagai admin, tambah kategori "Sejarah", lalu login sebagai pengguna dan tunjukkan kategori itu sudah muncul di katalog.
7. Edit profil dan ganti foto profil.

---

## 6. Masalah yang sering muncul

| Gejala | Penyebab & solusi |
|---|---|
| "Supabase belum dikonfigurasi" | `SUPABASE_URL` / `SUPABASE_ANON_KEY` belum diisi di `local.properties`. Isi, lalu *Sync* dan jalankan ulang. |
| Daftar berhasil tapi tidak langsung masuk | Konfirmasi email masih aktif. Matikan (lihat langkah 1.3) atau klik link di email. |
| "Kamu tidak punya akses untuk melakukan aksi ini" | SQL belum dijalankan lengkap, atau akun bukan admin saat mengelola kategori. |
| Foto tidak muncul | Pastikan `02_storage.sql` sudah dijalankan dan bucket berstatus *public*. |
| Error saat sync Gradle | Pastikan JDK 17+ (bawaan Android Studio sudah sesuai) dan koneksi internet aktif. |
