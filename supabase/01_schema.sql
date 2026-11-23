-- =====================================================================
-- BukuBerbagi - Skema database Supabase
-- Jalankan file ini di Supabase Dashboard > SQL Editor > New query > Run
-- Urutan: 01_schema.sql -> 02_storage.sql -> 03_seed.sql
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. TABEL
-- ---------------------------------------------------------------------

-- Profil pengguna (1:1 dengan auth.users). Baris dibuat otomatis oleh trigger
-- handle_new_user saat pengguna mendaftar.
create table if not exists public.profiles (
  id                uuid primary key references auth.users(id) on delete cascade,
  full_name         text not null,
  email             text,
  profile_image_url text,
  location          text,
  role              text not null default 'USER' check (role in ('USER', 'ADMIN')),
  created_at        timestamptz not null default now(),
  updated_at        timestamptz not null default now()
);

-- Kategori buku (dikelola admin - fitur Anggota 2)
create table if not exists public.categories (
  id            uuid primary key default gen_random_uuid(),
  category_name text not null unique,
  description   text,
  icon          text not null default 'bookmark',
  created_at    timestamptz not null default now()
);
create unique index if not exists categories_name_lower_idx
  on public.categories (lower(category_name));

-- Buku donasi (fitur Anggota 1)
create table if not exists public.books (
  id          uuid primary key default gen_random_uuid(),
  user_id     uuid not null references public.profiles(id) on delete cascade,
  category_id uuid not null references public.categories(id) on delete restrict,
  title       text not null check (length(trim(title)) > 0),
  author      text not null check (length(trim(author)) > 0),
  description text check (description is null or char_length(description) <= 500),
  condition   text not null check (condition in ('BARU', 'SEPERTI_BARU', 'LAYAK_BACA')),
  image_url   text not null,
  location    text,
  status      text not null default 'AVAILABLE' check (status in ('AVAILABLE', 'RESERVED', 'DONATED')),
  created_at  timestamptz not null default now(),
  updated_at  timestamptz not null default now()
);

-- Permintaan donasi (fitur Anggota 3)
create table if not exists public.donation_requests (
  id           uuid primary key default gen_random_uuid(),
  book_id      uuid not null references public.books(id) on delete cascade,
  requester_id uuid not null references public.profiles(id) on delete cascade,
  donor_id     uuid not null references public.profiles(id) on delete cascade,
  reason       text not null check (char_length(trim(reason)) >= 10),
  status       text not null default 'PENDING'
               check (status in ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED', 'COMPLETED')),
  created_at   timestamptz not null default now(),
  updated_at   timestamptz not null default now(),
  constraint donation_requests_not_self check (requester_id <> donor_id)
);

-- Konfirmasi serah terima (fitur Anggota 3). Dibuat otomatis saat permintaan disetujui.
create table if not exists public.handover_confirmations (
  id                  uuid primary key default gen_random_uuid(),
  request_id          uuid not null unique references public.donation_requests(id) on delete cascade,
  donor_confirmed     boolean not null default false,
  requester_confirmed boolean not null default false,
  handover_method     text check (handover_method in ('AMBIL_DI_LOKASI', 'COD', 'DIKIRIM')),
  handover_date       timestamptz,
  notes               text,
  created_at          timestamptz not null default now()
);

-- ---------------------------------------------------------------------
-- 2. INDEX
-- ---------------------------------------------------------------------
create index if not exists books_status_created_idx on public.books (status, created_at desc);
create index if not exists books_user_idx           on public.books (user_id);
create index if not exists books_category_idx       on public.books (category_id);
create index if not exists requests_requester_idx   on public.donation_requests (requester_id);
create index if not exists requests_donor_idx       on public.donation_requests (donor_id);
create index if not exists requests_book_idx        on public.donation_requests (book_id);

-- Satu pengguna hanya boleh punya satu permintaan aktif untuk buku yang sama
create unique index if not exists requests_one_active_per_user
  on public.donation_requests (book_id, requester_id)
  where status in ('PENDING', 'APPROVED');

-- Satu buku hanya boleh punya satu permintaan yang disetujui/selesai
create unique index if not exists requests_one_approved_per_book
  on public.donation_requests (book_id)
  where status in ('APPROVED', 'COMPLETED');

-- ---------------------------------------------------------------------
-- 3. FUNGSI BANTU & TRIGGER
-- Pesan error yang diawali "BB:" akan ditampilkan apa adanya di aplikasi.
-- ---------------------------------------------------------------------

-- updated_at otomatis
create or replace function public.set_updated_at()
returns trigger language plpgsql as $$
begin
  new.updated_at := now();
  return new;
end $$;

drop trigger if exists trg_profiles_updated on public.profiles;
create trigger trg_profiles_updated before update on public.profiles
  for each row execute function public.set_updated_at();

drop trigger if exists trg_books_updated on public.books;
create trigger trg_books_updated before update on public.books
  for each row execute function public.set_updated_at();

drop trigger if exists trg_requests_updated on public.donation_requests;
create trigger trg_requests_updated before update on public.donation_requests
  for each row execute function public.set_updated_at();

-- Cek admin (dipakai di RLS)
create or replace function public.is_admin()
returns boolean language sql stable security definer set search_path = public as $$
  select exists (select 1 from public.profiles where id = auth.uid() and role = 'ADMIN');
$$;

-- Buat profil otomatis saat register (nama diambil dari metadata full_name)
create or replace function public.handle_new_user()
returns trigger language plpgsql security definer set search_path = public as $$
begin
  insert into public.profiles (id, full_name, email)
  values (
    new.id,
    coalesce(nullif(trim(new.raw_user_meta_data ->> 'full_name'), ''), split_part(new.email, '@', 1)),
    new.email
  )
  on conflict (id) do nothing;
  return new;
end $$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created after insert on auth.users
  for each row execute function public.handle_new_user();

-- Pengguna tidak boleh menaikkan role-nya sendiri menjadi ADMIN dari aplikasi.
-- (Perubahan dari SQL Editor tetap diizinkan karena auth.uid() bernilai null di sana.)
create or replace function public.protect_profile_role()
returns trigger language plpgsql as $$
begin
  if new.role is distinct from old.role and auth.uid() is not null and not public.is_admin() then
    raise exception 'BB: Kamu tidak boleh mengubah peran akun';
  end if;
  new.id := old.id;
  return new;
end $$;

drop trigger if exists trg_protect_role on public.profiles;
create trigger trg_protect_role before update on public.profiles
  for each row execute function public.protect_profile_role();

-- Status buku hanya boleh berubah lewat RPC permintaan (bukan diedit langsung)
create or replace function public.guard_book_update()
returns trigger language plpgsql as $$
begin
  if new.user_id is distinct from old.user_id then
    raise exception 'BB: Pemilik buku tidak bisa diubah';
  end if;
  if new.status is distinct from old.status
     and auth.uid() is not null
     and coalesce(current_setting('app.bypass', true), '') <> 'on' then
    raise exception 'BB: Status buku hanya bisa berubah melalui proses permintaan';
  end if;
  return new;
end $$;

drop trigger if exists trg_guard_book_update on public.books;
create trigger trg_guard_book_update before update on public.books
  for each row execute function public.guard_book_update();

-- Buku yang sudah punya permintaan disetujui/selesai tidak boleh dihapus
create or replace function public.guard_book_delete()
returns trigger language plpgsql as $$
begin
  if exists (select 1 from public.donation_requests d
             where d.book_id = old.id and d.status in ('APPROVED', 'COMPLETED')) then
    raise exception 'BB: Buku tidak bisa dihapus karena sudah ada permintaan yang disetujui';
  end if;
  return old;
end $$;

drop trigger if exists trg_guard_book_delete on public.books;
create trigger trg_guard_book_delete before delete on public.books
  for each row execute function public.guard_book_delete();

-- Kategori yang masih dipakai buku tidak boleh dihapus
create or replace function public.guard_category_delete()
returns trigger language plpgsql as $$
begin
  if exists (select 1 from public.books where category_id = old.id) then
    raise exception 'BB: Kategori masih dipakai oleh buku, tidak bisa dihapus';
  end if;
  return old;
end $$;

drop trigger if exists trg_guard_category_delete on public.categories;
create trigger trg_guard_category_delete before delete on public.categories
  for each row execute function public.guard_category_delete();

-- Validasi saat permintaan baru dibuat (aturan bisnis 1, 2, 3)
create or replace function public.validate_new_request()
returns trigger language plpgsql security definer set search_path = public as $$
declare
  b public.books;
begin
  select * into b from public.books where id = new.book_id;
  if not found then
    raise exception 'BB: Buku tidak ditemukan';
  end if;
  if b.user_id = new.requester_id then
    raise exception 'BB: Kamu tidak bisa meminta buku milikmu sendiri';
  end if;
  if b.status <> 'AVAILABLE' then
    raise exception 'BB: Buku ini sudah tidak tersedia';
  end if;
  if exists (select 1 from public.donation_requests
             where book_id = new.book_id and requester_id = new.requester_id
               and status in ('PENDING', 'APPROVED')) then
    raise exception 'BB: Kamu sudah punya permintaan aktif untuk buku ini';
  end if;
  new.donor_id := b.user_id;   -- donatur selalu diambil dari pemilik buku
  new.status   := 'PENDING';
  return new;
end $$;

drop trigger if exists trg_validate_new_request on public.donation_requests;
create trigger trg_validate_new_request before insert on public.donation_requests
  for each row execute function public.validate_new_request();

-- ---------------------------------------------------------------------
-- 4. RPC (dipanggil dari aplikasi lewat supabase.postgrest.rpc)
-- Semua perubahan status divalidasi di sini, bukan di aplikasi.
-- ---------------------------------------------------------------------

-- Setujui permintaan secara atomik (baris buku dikunci FOR UPDATE)
create or replace function public.approve_request(p_request_id uuid)
returns void language plpgsql security definer set search_path = public as $$
declare
  r public.donation_requests;
  b public.books;
begin
  select * into r from public.donation_requests where id = p_request_id for update;
  if not found then raise exception 'BB: Permintaan tidak ditemukan'; end if;
  if r.donor_id <> auth.uid() then raise exception 'BB: Hanya pemilik buku yang bisa menyetujui'; end if;
  if r.status <> 'PENDING' then raise exception 'BB: Permintaan ini sudah diproses'; end if;

  select * into b from public.books where id = r.book_id for update;
  if b.status <> 'AVAILABLE' then raise exception 'BB: Buku sudah dipesan penerima lain'; end if;

  perform set_config('app.bypass', 'on', true);
  update public.donation_requests set status = 'APPROVED' where id = r.id;
  update public.donation_requests set status = 'REJECTED'
    where book_id = r.book_id and id <> r.id and status = 'PENDING';
  update public.books set status = 'RESERVED' where id = b.id;
  insert into public.handover_confirmations (request_id) values (r.id)
    on conflict (request_id) do nothing;
  perform set_config('app.bypass', 'off', true);
end $$;

-- Tolak permintaan (hanya donatur, hanya yang PENDING). Status buku tidak berubah.
create or replace function public.reject_request(p_request_id uuid)
returns void language plpgsql security definer set search_path = public as $$
declare r public.donation_requests;
begin
  select * into r from public.donation_requests where id = p_request_id for update;
  if not found then raise exception 'BB: Permintaan tidak ditemukan'; end if;
  if r.donor_id <> auth.uid() then raise exception 'BB: Hanya pemilik buku yang bisa menolak'; end if;
  if r.status <> 'PENDING' then raise exception 'BB: Permintaan ini sudah diproses'; end if;
  update public.donation_requests set status = 'REJECTED' where id = r.id;
end $$;

-- Batalkan permintaan (hanya peminta, hanya yang PENDING). Status buku tidak berubah.
create or replace function public.cancel_request(p_request_id uuid)
returns void language plpgsql security definer set search_path = public as $$
declare r public.donation_requests;
begin
  select * into r from public.donation_requests where id = p_request_id for update;
  if not found then raise exception 'BB: Permintaan tidak ditemukan'; end if;
  if r.requester_id <> auth.uid() then raise exception 'BB: Hanya peminta yang bisa membatalkan'; end if;
  if r.status <> 'PENDING' then raise exception 'BB: Hanya permintaan yang masih menunggu yang bisa dibatalkan'; end if;
  update public.donation_requests set status = 'CANCELLED' where id = r.id;
end $$;

-- Donatur mengatur jadwal & metode penyerahan
create or replace function public.set_handover_schedule(
  p_request_id uuid, p_method text, p_date timestamptz, p_notes text)
returns void language plpgsql security definer set search_path = public as $$
declare r public.donation_requests;
begin
  select * into r from public.donation_requests where id = p_request_id;
  if not found then raise exception 'BB: Permintaan tidak ditemukan'; end if;
  if r.donor_id <> auth.uid() then raise exception 'BB: Hanya donatur yang bisa mengatur jadwal'; end if;
  if r.status <> 'APPROVED' then raise exception 'BB: Jadwal hanya bisa diatur untuk permintaan yang disetujui'; end if;
  if p_method not in ('AMBIL_DI_LOKASI', 'COD', 'DIKIRIM') then raise exception 'BB: Metode penyerahan tidak valid'; end if;

  insert into public.handover_confirmations (request_id, handover_method, handover_date, notes)
  values (r.id, p_method, p_date, p_notes)
  on conflict (request_id) do update
    set handover_method = excluded.handover_method,
        handover_date   = excluded.handover_date,
        notes           = excluded.notes;
end $$;

-- Konfirmasi serah terima. Jika kedua pihak sudah konfirmasi:
-- permintaan -> COMPLETED, buku -> DONATED. Mengembalikan status permintaan terbaru.
create or replace function public.confirm_handover(p_request_id uuid)
returns text language plpgsql security definer set search_path = public as $$
declare
  r public.donation_requests;
  h public.handover_confirmations;
begin
  select * into r from public.donation_requests where id = p_request_id for update;
  if not found then raise exception 'BB: Permintaan tidak ditemukan'; end if;
  if r.status <> 'APPROVED' then raise exception 'BB: Permintaan belum disetujui atau sudah selesai'; end if;
  if auth.uid() not in (r.donor_id, r.requester_id) then raise exception 'BB: Kamu bukan bagian dari permintaan ini'; end if;

  select * into h from public.handover_confirmations where request_id = r.id for update;
  if not found or h.handover_method is null then
    raise exception 'BB: Donatur belum mengatur jadwal penyerahan';
  end if;

  if auth.uid() = r.donor_id then
    update public.handover_confirmations set donor_confirmed = true where id = h.id returning * into h;
  else
    update public.handover_confirmations set requester_confirmed = true where id = h.id returning * into h;
  end if;

  if h.donor_confirmed and h.requester_confirmed then
    perform set_config('app.bypass', 'on', true);
    update public.donation_requests set status = 'COMPLETED' where id = r.id;
    update public.books set status = 'DONATED' where id = r.book_id;
    perform set_config('app.bypass', 'off', true);
    return 'COMPLETED';
  end if;
  return 'APPROVED';
end $$;

-- Ringkasan untuk dasbor admin
create or replace function public.admin_stats()
returns table (total_users bigint, available_books bigint, active_requests bigint, donated_books bigint)
language plpgsql stable security definer set search_path = public as $$
begin
  if not public.is_admin() then raise exception 'BB: Khusus admin'; end if;
  return query select
    (select count(*) from public.profiles where role = 'USER'),
    (select count(*) from public.books where status = 'AVAILABLE'),
    (select count(*) from public.donation_requests where status in ('PENDING', 'APPROVED')),
    (select count(*) from public.books where status = 'DONATED');
end $$;

revoke execute on function public.approve_request(uuid)       from public, anon;
revoke execute on function public.reject_request(uuid)        from public, anon;
revoke execute on function public.cancel_request(uuid)        from public, anon;
revoke execute on function public.set_handover_schedule(uuid, text, timestamptz, text) from public, anon;
revoke execute on function public.confirm_handover(uuid)      from public, anon;
revoke execute on function public.admin_stats()               from public, anon;
grant execute on function public.approve_request(uuid)        to authenticated;
grant execute on function public.reject_request(uuid)         to authenticated;
grant execute on function public.cancel_request(uuid)         to authenticated;
grant execute on function public.set_handover_schedule(uuid, text, timestamptz, text) to authenticated;
grant execute on function public.confirm_handover(uuid)       to authenticated;
grant execute on function public.admin_stats()                to authenticated;

-- ---------------------------------------------------------------------
-- 5. ROW LEVEL SECURITY
-- ---------------------------------------------------------------------
alter table public.profiles               enable row level security;
alter table public.categories             enable row level security;
alter table public.books                  enable row level security;
alter table public.donation_requests      enable row level security;
alter table public.handover_confirmations enable row level security;

-- profiles
drop policy if exists "profiles_select" on public.profiles;
create policy "profiles_select" on public.profiles
  for select to authenticated using (true);
drop policy if exists "profiles_insert_own" on public.profiles;
create policy "profiles_insert_own" on public.profiles
  for insert to authenticated with check (id = auth.uid());
drop policy if exists "profiles_update_own" on public.profiles;
create policy "profiles_update_own" on public.profiles
  for update to authenticated using (id = auth.uid()) with check (id = auth.uid());

-- categories: semua pengguna login boleh membaca, hanya admin yang boleh mengubah
drop policy if exists "categories_select" on public.categories;
create policy "categories_select" on public.categories
  for select to authenticated using (true);
drop policy if exists "categories_admin_insert" on public.categories;
create policy "categories_admin_insert" on public.categories
  for insert to authenticated with check (public.is_admin());
drop policy if exists "categories_admin_update" on public.categories;
create policy "categories_admin_update" on public.categories
  for update to authenticated using (public.is_admin()) with check (public.is_admin());
drop policy if exists "categories_admin_delete" on public.categories;
create policy "categories_admin_delete" on public.categories
  for delete to authenticated using (public.is_admin());

-- books: katalog dapat dibaca pengguna login; buat/ubah/hapus hanya milik sendiri
drop policy if exists "books_select" on public.books;
create policy "books_select" on public.books
  for select to authenticated using (true);
drop policy if exists "books_insert_own" on public.books;
create policy "books_insert_own" on public.books
  for insert to authenticated with check (user_id = auth.uid() and status = 'AVAILABLE');
drop policy if exists "books_update_own" on public.books;
create policy "books_update_own" on public.books
  for update to authenticated using (user_id = auth.uid()) with check (user_id = auth.uid());
drop policy if exists "books_delete_own" on public.books;
create policy "books_delete_own" on public.books
  for delete to authenticated using (user_id = auth.uid());

-- donation_requests: hanya peminta, donatur, atau admin yang bisa melihat.
-- Insert hanya atas nama sendiri. Tidak ada policy UPDATE: perubahan status wajib lewat RPC.
drop policy if exists "requests_select_party" on public.donation_requests;
create policy "requests_select_party" on public.donation_requests
  for select to authenticated
  using (requester_id = auth.uid() or donor_id = auth.uid() or public.is_admin());
drop policy if exists "requests_insert_own" on public.donation_requests;
create policy "requests_insert_own" on public.donation_requests
  for insert to authenticated with check (requester_id = auth.uid());

-- handover_confirmations: hanya pihak terkait yang bisa melihat. Ubah lewat RPC.
drop policy if exists "handover_select_party" on public.handover_confirmations;
create policy "handover_select_party" on public.handover_confirmations
  for select to authenticated
  using (exists (
    select 1 from public.donation_requests d
    where d.id = request_id
      and (d.requester_id = auth.uid() or d.donor_id = auth.uid() or public.is_admin())
  ));
