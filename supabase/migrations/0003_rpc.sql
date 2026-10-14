create or replace function public.create_direct_chat(other uuid) returns uuid
language plpgsql security definer set search_path = public as $$
declare key text; chat_id uuid;
begin
  if auth.uid() is null or auth.uid() = other then raise exception 'invalid participant'; end if;
  key := least(auth.uid()::text, other::text) || ':' || greatest(auth.uid()::text, other::text);
  insert into public.chats (type, direct_key, created_by)
  values ('direct', key, auth.uid())
  on conflict (direct_key) do update set direct_key = excluded.direct_key
  returning id into chat_id;
  insert into public.chat_members (chat_id, user_id, role) values (chat_id, auth.uid(), 'owner') on conflict do nothing;
  insert into public.chat_members (chat_id, user_id, role) values (chat_id, other, 'member') on conflict do nothing;
  return chat_id;
end $$;

create or replace function public.create_group(title text, member_ids uuid[], is_channel boolean default false) returns uuid
language plpgsql security definer set search_path = public as $$
declare chat_id uuid; member uuid;
begin
  if auth.uid() is null or nullif(trim(title), '') is null then raise exception 'invalid group'; end if;
  insert into public.chats (type, title, created_by) values (case when is_channel then 'channel' else 'group' end, trim(title), auth.uid()) returning id into chat_id;
  insert into public.chat_members (chat_id, user_id, role) values (chat_id, auth.uid(), 'owner');
  foreach member in array coalesce(member_ids, '{}') loop
    insert into public.chat_members (chat_id, user_id) values (chat_id, member) on conflict do nothing;
  end loop;
  return chat_id;
end $$;

create or replace function public.join_by_invite(code text) returns uuid
language plpgsql security definer set search_path = public as $$
declare chat_id uuid;
begin
  select id into chat_id from public.chats where invite_code = code;
  if chat_id is null then raise exception 'invite not found'; end if;
  insert into public.chat_members (chat_id, user_id) values (chat_id, auth.uid()) on conflict do nothing;
  return chat_id;
end $$;

create or replace function public.mark_read(chat uuid, up_to_seq bigint) returns void
language sql security definer set search_path = public as $$
  update public.chat_members set last_read_seq = greatest(last_read_seq, up_to_seq)
  where chat_id = chat and user_id = auth.uid();
$$;

create or replace function public.mark_delivered(chat uuid, up_to_seq bigint) returns void
language sql security definer set search_path = public as $$
  update public.chat_members set last_delivered_seq = greatest(last_delivered_seq, up_to_seq)
  where chat_id = chat and user_id = auth.uid();
$$;

create or replace function public.search_users(q text) returns setof public.profiles
language sql stable security invoker set search_path = public as $$
  select * from public.profiles where username ilike trim(q) || '%' or display_name ilike trim(q) || '%' order by username limit 20;
$$;

create or replace function public.search_messages(chat uuid default null, q text default '', lim int default 30)
returns setof public.messages language sql stable security invoker set search_path = public as $$
  select m.* from public.messages m where public.is_member(m.chat_id) and (chat is null or m.chat_id = chat)
    and to_tsvector('simple', coalesce(m.body, '')) @@ plainto_tsquery('simple', q)
  order by m.seq desc limit least(greatest(lim, 1), 100);
$$;
