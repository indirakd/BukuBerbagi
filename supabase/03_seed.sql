-- =====================================================================
-- BukuBerbagi - Data awal kategori
-- Nilai kolom icon dipetakan ke ikon di aplikasi (CategoryIcons.kt):
-- school, science, religion, fiction, child, growth, history, art, language, food, bookmark
-- =====================================================================

insert into public.categories (category_name, description, icon) values
  ('Sekolah',           'Buku paket SD sampai SMA',          'school'),
  ('Kuliah',            'Referensi perguruan tinggi',        'science'),
  ('Agama',             'Buku keislaman dan agama lainnya',  'religion'),
  ('Fiksi',             'Novel, cerpen, dan komik',          'fiction'),
  ('Anak',              'Cerita bergambar dan dongeng',      'child'),
  ('Pengembangan diri', 'Motivasi dan keterampilan',         'growth')
on conflict (category_name) do nothing;

-- =====================================================================
-- MEMBUAT AKUN ADMIN
-- 1. Daftar dulu lewat aplikasi (misalnya admin@bukuberbagi.id).
-- 2. Jalankan perintah di bawah (ganti emailnya), lalu login ulang di aplikasi.
-- =====================================================================
-- update public.profiles set role = 'ADMIN' where email = 'admin@bukuberbagi.id';
