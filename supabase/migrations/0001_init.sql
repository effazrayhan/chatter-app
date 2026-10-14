create extension if not exists pgcrypto;

create table public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  username text unique check (username is null or username ~ '^[a-zA-Z0-9_]{3,32}$'),
  display_name text not null default '',
  avatar_path text,
  bio text not null default '',
  last_seen timestamptz not null default now(),
  created_at timestamptz not null default now()
);

create table public.devices (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references public.profiles(id) on delete cascade,
  fcm_token text not null unique,
  platform text not null default 'android',
  updated_at timestamptz not null default now()
);
create index devices_user_id_idx on public.devices(user_id);

create table public.chats (
  id uuid primary key default gen_random_uuid(),
  type text not null check (type in ('direct', 'group', 'channel', 'saved')),
  title text,
  description text,
  avatar_path text,
  username text unique,
  invite_code text unique default encode(gen_random_bytes(9), 'hex'),
  direct_key text unique,
  created_by uuid references public.profiles(id),
  created_at timestamptz not null default now(),
  last_message_at timestamptz not null default now()
);

create table public.chat_members (
  chat_id uuid references public.chats(id) on delete cascade,
  user_id uuid references public.profiles(id) on delete cascade,
  role text not null default 'member' check (role in ('owner', 'admin', 'member')),
  muted_until timestamptz,
  pinned boolean not null default false,
  archived boolean not null default false,
  folder text,
  last_read_seq bigint not null default 0,
  last_delivered_seq bigint not null default 0,
  joined_at timestamptz not null default now(),
  primary key (chat_id, user_id)
);
create index chat_members_user_id_idx on public.chat_members(user_id);

create table public.messages (
  id uuid primary key,
  seq bigint generated always as identity,
  chat_id uuid not null references public.chats(id) on delete cascade,
  sender_id uuid not null references public.profiles(id),
  body text check (body is null or char_length(body) <= 4096),
  reply_to_id uuid references public.messages(id) on delete set null,
  forwarded_from_user uuid references public.profiles(id),
  edited_at timestamptz,
  deleted_at timestamptz,
  created_at timestamptz not null default now()
);
create index messages_chat_seq_idx on public.messages(chat_id, seq desc);
create index messages_body_search_idx on public.messages using gin (to_tsvector('simple', coalesce(body, '')));

create table public.attachments (
  id uuid primary key default gen_random_uuid(),
  message_id uuid not null references public.messages(id) on delete cascade,
  kind text not null check (kind in ('image', 'video', 'file', 'voice', 'link')),
  storage_path text,
  thumb_path text,
  mime text,
  size_bytes bigint,
  width int,
  height int,
  duration_ms int,
  waveform jsonb,
  link_url text,
  link_title text,
  link_desc text,
  link_image text
);

create table public.reactions (
  message_id uuid references public.messages(id) on delete cascade,
  user_id uuid references public.profiles(id) on delete cascade,
  emoji text not null,
  created_at timestamptz not null default now(),
  primary key (message_id, user_id, emoji)
);

create table public.chat_pins (
  chat_id uuid references public.chats(id) on delete cascade,
  message_id uuid references public.messages(id) on delete cascade,
  pinned_by uuid references public.profiles(id),
  primary key (chat_id, message_id)
);

create table public.hidden_messages (
  message_id uuid references public.messages(id) on delete cascade,
  user_id uuid references public.profiles(id) on delete cascade,
  primary key (message_id, user_id)
);

create or replace function public.is_member(cid uuid) returns boolean
language sql stable security definer set search_path = public as $$
  select exists (select 1 from public.chat_members where chat_id = cid and user_id = auth.uid());
$$;

create or replace function public.is_admin(cid uuid) returns boolean
language sql stable security definer set search_path = public as $$
  select exists (
    select 1 from public.chat_members
    where chat_id = cid and user_id = auth.uid() and role in ('owner', 'admin')
  );
$$;

create or replace function public.handle_new_user() returns trigger
language plpgsql security definer set search_path = public as $$
declare saved_chat uuid;
begin
  insert into public.profiles (id, display_name)
  values (new.id, coalesce(new.raw_user_meta_data->>'name', ''));
  insert into public.chats (type, title, created_by)
  values ('saved', 'Saved Messages', new.id) returning id into saved_chat;
  insert into public.chat_members (chat_id, user_id, role)
  values (saved_chat, new.id, 'owner');
  return new;
end $$;

create trigger on_auth_user_created after insert on auth.users
for each row execute function public.handle_new_user();

create or replace function public.bump_chat() returns trigger
language plpgsql as $$
begin
  update public.chats set last_message_at = now() where id = new.chat_id;
  return new;
end $$;

create trigger trg_bump_chat after insert on public.messages
for each row execute function public.bump_chat();
