-- =====================================================================
-- BukuBerbagi - Supabase Storage
-- Bucket publik untuk foto buku dan foto profil.
-- Setiap pengguna hanya boleh mengunggah ke folder bernama user id-nya:
--   book-images/<user_id>/<nama_file>.jpg
--   avatars/<user_id>/avatar_<waktu>.jpg
-- =====================================================================

insert into storage.buckets (id, name, public)
values ('book-images', 'book-images', true),
       ('avatars', 'avatars', true)
on conflict (id) do update set public = excluded.public;

drop policy if exists "bb_public_read" on storage.objects;
create policy "bb_public_read" on storage.objects
  for select using (bucket_id in ('book-images', 'avatars'));

drop policy if exists "bb_upload_own_folder" on storage.objects;
create policy "bb_upload_own_folder" on storage.objects
  for insert to authenticated
  with check (
    bucket_id in ('book-images', 'avatars')
    and (storage.foldername(name))[1] = auth.uid()::text
  );

drop policy if exists "bb_update_own_folder" on storage.objects;
create policy "bb_update_own_folder" on storage.objects
  for update to authenticated
  using (
    bucket_id in ('book-images', 'avatars')
    and (storage.foldername(name))[1] = auth.uid()::text
  );

drop policy if exists "bb_delete_own_folder" on storage.objects;
create policy "bb_delete_own_folder" on storage.objects
  for delete to authenticated
  using (
    bucket_id in ('book-images', 'avatars')
    and (storage.foldername(name))[1] = auth.uid()::text
  );
