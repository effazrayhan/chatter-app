insert into storage.buckets (id, name, public) values
  ('avatars', 'avatars', true), ('media', 'media', false), ('voice', 'voice', false), ('thumbs', 'thumbs', false)
on conflict (id) do nothing;

create policy avatar_read on storage.objects for select to public using (bucket_id = 'avatars');
create policy avatar_write on storage.objects for insert to authenticated with check (bucket_id = 'avatars' and (storage.foldername(name))[1] = auth.uid()::text);
create policy private_media_read on storage.objects for select to authenticated
using (bucket_id in ('media', 'voice', 'thumbs') and public.is_member(((storage.foldername(name))[1])::uuid));
create policy private_media_write on storage.objects for insert to authenticated
with check (bucket_id in ('media', 'voice', 'thumbs') and public.is_member(((storage.foldername(name))[1])::uuid));
