# specs.md: Pulse (Telegram-style Android chat app)

## 1. Goals
A working, installable Android chat app with Telegram-like UX: 1:1 chats, groups, channels, media, voice messages, reactions, replies, search, push notifications, offline support. **100% free-tier infrastructure.**

Non-goals (v1): end-to-end encryption, bots, voice/video calls, stickers marketplace, phone-number login, iOS/web clients.

## 2. Free stack

| Concern | Choice | Free-tier note |
|---|---|---|
| Client | Kotlin, Compose, Material 3, Hilt, Room, Paging 3, WorkManager, Coil, CameraX, Media3 | all open source |
| Auth | Supabase Auth: email+password, Google sign-in | no SMS cost |
| Database | Supabase Postgres + RLS | 500 MB |
| Realtime | Supabase Realtime: Postgres Changes (messages), Broadcast (typing), Presence (online) | 200 concurrent connections |
| File storage | Supabase Storage (private buckets, signed URLs) | 1 GB, 50 MB/file |
| Server logic | Postgres functions (RPC) + Supabase Edge Functions (Deno) | 500k invocations/mo |
| Push | FCM HTTP v1 sent from an Edge Function | FCM is free; Spark plan is enough (no Cloud Functions/Storage used) |
| Crash reports | Firebase Crashlytics | free |
| CI/CD | GitHub Actions | free for public repos; 2000 min/mo private |
| Distribution | GitHub Releases (signed APK) | free |
| Link previews | Edge Function fetching OpenGraph tags | free |

**Important:** Supabase free projects pause after ~7 days of inactivity. Document "how to unpause" in `docs/setup.md`. Optionally add a GitHub Actions cron that pings the REST endpoint daily.

Fallback if Supabase limits hurt: the repository interfaces allow swapping to a self-hosted backend later. Do not build that now.

## 3. Architecture

```
Compose UI → ViewModel → Repository (interface)
                              │
                 ┌────────────┴─────────────┐
               Room (truth)           RemoteDataSource (supabase-kt)
                 ▲                          │
                 └──── SyncEngine ◄─────────┘  (Realtime + paged fetch)
                         ▲
                  OutboxWorker (WorkManager) → pushes pending writes
```

- **Reads:** UI observes Room `Flow`s. Chat history via Paging 3 `RemoteMediator` or Room `PagingSource` with a sync engine filling gaps.
- **Writes:** insert locally with `state = PENDING`, enqueue unique `OutboxWorker`; worker sends in `created_local` order per chat, marks `SENT` on ack; exponential backoff; idempotent via client UUID (`INSERT ... ON CONFLICT (id) DO NOTHING`).
- **Realtime sync:** on app foreground and after auth, subscribe to `messages`, `chat_members`, `chats`, `reactions` changes (RLS-filtered). On reconnect, run catch-up: fetch rows with `seq > lastSyncedSeq` per chat.
- **Typing/online:** Realtime Broadcast on channel `chat:{id}` (event `typing`, throttled to 1 per 3 s); Presence on channel `online` for global presence; persist `profiles.last_seen` on background.
- **Message states:** `PENDING → SENT → DELIVERED → READ` (or `FAILED`). Delivered/read derived from `chat_members.last_delivered_seq` / `last_read_seq` (no per-message receipt rows).
- **Ordering:** `messages.seq` (identity column) is the server order. Pending local messages sort after the last synced one by local time.

## 4. Database schema (Postgres) — implement in `supabase/migrations/0001_init.sql`

```sql
create extension if not exists pgcrypto;

-- PROFILES
create table public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  username text unique check (username ~ '^[a-zA-Z0-9_]{3,32}$'),
  display_name text not null default '',
  avatar_path text,
  bio text default '',
  last_seen timestamptz default now(),
  created_at timestamptz default now()
);

-- DEVICES (FCM tokens)
create table public.devices (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references public.profiles(id) on delete cascade,
  fcm_token text not null unique,
  platform text default 'android',
  updated_at timestamptz default now()
);

-- CHATS
create table public.chats (
  id uuid primary key default gen_random_uuid(),
  type text not null check (type in ('direct','group','channel','saved')),
  title text,
  description text,
  avatar_path text,
  username text unique,                 -- public handle for channels/groups (optional)
  invite_code text unique default encode(gen_random_bytes(9),'hex'),
  direct_key text unique,               -- sorted "uidA:uidB" to dedupe direct chats
  created_by uuid references public.profiles(id),
  created_at timestamptz default now(),
  last_message_at timestamptz default now()
);

-- MEMBERS
create table public.chat_members (
  chat_id uuid references public.chats(id) on delete cascade,
  user_id uuid references public.profiles(id) on delete cascade,
  role text not null default 'member' check (role in ('owner','admin','member')),
  muted_until timestamptz,
  pinned boolean default false,
  archived boolean default false,
  folder text,
  last_read_seq bigint default 0,
  last_delivered_seq bigint default 0,
  joined_at timestamptz default now(),
  primary key (chat_id, user_id)
);
create index on public.chat_members(user_id);

-- MESSAGES
create table public.messages (
  id uuid primary key,                           -- client generated
  seq bigint generated always as identity,
  chat_id uuid not null references public.chats(id) on delete cascade,
  sender_id uuid not null references public.profiles(id),
  body text check (char_length(body) <= 4096),
  reply_to_id uuid references public.messages(id) on delete set null,
  forwarded_from_user uuid references public.profiles(id),
  edited_at timestamptz,
  deleted_at timestamptz,                        -- "delete for everyone" tombstone
  created_at timestamptz default now()
);
create index on public.messages(chat_id, seq desc);
create index on public.messages using gin (to_tsvector('simple', coalesce(body,'')));

-- ATTACHMENTS
create table public.attachments (
  id uuid primary key default gen_random_uuid(),
  message_id uuid not null references public.messages(id) on delete cascade,
  kind text not null check (kind in ('image','video','file','voice','link')),
  storage_path text,
  thumb_path text,
  mime text, size_bytes bigint, width int, height int,
  duration_ms int, waveform jsonb,
  link_url text, link_title text, link_desc text, link_image text
);

-- REACTIONS
create table public.reactions (
  message_id uuid references public.messages(id) on delete cascade,
  user_id uuid references public.profiles(id) on delete cascade,
  emoji text not null,
  created_at timestamptz default now(),
  primary key (message_id, user_id, emoji)
);

-- PINS & HIDDEN (delete for me)
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

-- HELPERS
create or replace function public.is_member(cid uuid) returns boolean
language sql stable security definer set search_path = public as $$
  select exists (select 1 from chat_members where chat_id = cid and user_id = auth.uid());
$$;
create or replace function public.is_admin(cid uuid) returns boolean
language sql stable security definer set search_path = public as $$
  select exists (select 1 from chat_members
    where chat_id = cid and user_id = auth.uid() and role in ('owner','admin'));
$$;

-- Auto-create profile on signup
create or replace function public.handle_new_user() returns trigger
language plpgsql security definer set search_path = public as $$
begin
  insert into profiles (id, display_name) values (new.id, coalesce(new.raw_user_meta_data->>'name',''));
  insert into chats (type, title, created_by) values ('saved','Saved Messages', new.id);
  insert into chat_members (chat_id, user_id, role)
    select id, new.id, 'owner' from chats where type='saved' and created_by=new.id
    order by created_at desc limit 1;
  return new;
end $$;
create trigger on_auth_user_created after insert on auth.users
  for each row execute function public.handle_new_user();

-- bump chat.last_message_at
create or replace function public.bump_chat() returns trigger language plpgsql as $$
begin update chats set last_message_at = now() where id = new.chat_id; return new; end $$;
create trigger trg_bump_chat after insert on public.messages
  for each row execute function public.bump_chat();
```

### RLS (in `0002_rls.sql`)
Enable RLS on **every** table. Policies:
- `profiles`: select for any authenticated user; update only own row.
- `devices`: all operations only where `user_id = auth.uid()`.
- `chats`: select if `is_member(id)` or `username is not null` (public discovery, limited columns via a view if needed); update if `is_admin(id)`; insert via RPC only.
- `chat_members`: select if `is_member(chat_id)`; update own row for personal flags (`muted_until, pinned, archived, folder, last_read_seq, last_delivered_seq`); role changes/removal via admin RPCs.
- `messages`: select if `is_member(chat_id)`; insert if `is_member(chat_id) and sender_id = auth.uid()` and (chat type <> 'channel' or `is_admin(chat_id)`); update (edit) only own messages, only `body` and `edited_at`, enforced by a `before update` trigger; delete-for-everyone sets `deleted_at` (sender or admin) and nulls `body` via trigger.
- `attachments`, `reactions`, `chat_pins`: scoped through `is_member` of the parent message's chat; reactions only own rows; pins admin-only in groups/channels.
- `hidden_messages`: own rows only.
- Storage: buckets `avatars` (public read OK), `media`, `voice`, `thumbs` (private). Policy: path prefix `{chat_id}/...`; read if `is_member(chat_id)`, write if member. Use signed URLs (1 h) client-side and cache with Coil.

### RPC functions (security definer, in `0003_rpc.sql`)
- `create_direct_chat(other uuid) returns uuid`: builds sorted `direct_key`, upserts chat + 2 members.
- `create_group(title text, member_ids uuid[], is_channel boolean) returns uuid`.
- `join_by_invite(code text) returns uuid`.
- `add_member(chat uuid, user uuid)`, `remove_member`, `set_role` (admin-only checks).
- `mark_read(chat uuid, up_to_seq bigint)`, `mark_delivered(...)`: monotonic updates.
- `search_messages(chat uuid default null, q text, lim int default 30)`: full text search restricted to chats the caller is in.
- `search_users(q text)`: prefix match on username/display_name, limit 20.
- `delete_for_everyone(message uuid)`.
- Enable Realtime publication for: `messages, reactions, chat_members, chats, chat_pins, attachments`.

## 5. Edge Functions (`supabase/functions/`)
1. **`push-message`**: triggered by a Database Webhook on `messages` INSERT. Loads chat members except sender, skips muted (`muted_until > now()`), loads `devices.fcm_token`s, sends FCM HTTP v1 *data* messages (`chatId, messageId, senderName, preview`). Deletes tokens FCM reports as `UNREGISTERED`. Secret: `FCM_SERVICE_ACCOUNT_JSON` (Supabase secrets, never in repo).
2. **`link-preview`**: given URL, fetch with 3 s timeout and 1 MB cap, parse OpenGraph, return JSON; block private IP ranges (SSRF protection). Client calls it, then inserts the `attachments` link row.
3. **`cleanup`** (scheduled via pg_cron): remove stale devices (> 60 days), orphaned storage objects.

## 6. Client data model (Room)
Entities: `UserEntity`, `ChatEntity`, `ChatMemberEntity`, `MessageEntity` (adds `localState`, `localCreatedAt`, `retryCount`), `AttachmentEntity` (adds `localUri`, `uploadProgress`), `ReactionEntity`, `PinEntity`, `DraftEntity(chatId, text, updatedAt)`, `SyncStateEntity(chatId, lastSeq)`, `OutboxEntity`.
Use Room migrations from v1; export schema JSON and commit it.

## 7. Features and acceptance criteria

### 7.1 Auth and profile
- Email/password + Google sign-in (Credential Manager). Session persisted (supabase-kt session storage + DataStore). Logout clears Room and unregisters FCM token.
- Profile: display name, unique username (live availability check), avatar (crop + upload), bio.
- ✔ Register, kill app, reopen: still logged in. Username conflict shows an inline error.

### 7.2 Chat list
- Sorted by pinned, then `last_message_at`. Shows avatar, title, last message preview (with sender for groups, "Photo"/"Voice message" etc.), time, unread badge, mute icon, delivery tick for own messages, draft preview in red.
- Swipe actions: pin, mute, archive. Archive screen. Folder tabs (All + user-defined).
- ✔ Works fully offline from cache; updates live.

### 7.3 Chat screen
- Bubbles (own right/others left), grouped by sender, date separators, day-relative timestamps, tails, link auto-detection.
- Input bar: text, emoji panel, attach menu, mic hold-to-record (slide to cancel, lock), send button morphing.
- Typing indicator ("X is typing…", multiple users in groups), online / last seen in the top bar.
- Unread divider and "jump to bottom" FAB with unread count; scroll restores position; drafts saved per chat.
- ✔ Two devices chat in near-realtime; airplane mode → messages queue with clock icon, deliver in order on reconnect with no duplicates.

### 7.4 Message actions
Long-press menu: reply, copy, forward (multi-select chat picker), edit (own, 48 h), delete (for me / for everyone), pin, react, select multiple, share.
- Swipe-to-reply with haptic. Tap quoted message to scroll-to and highlight.
- Reactions: quick bar of 6 emoji + full picker; chips under bubble with counts; tap to toggle.
- In-chat search with highlighted results and prev/next; global search (chats, users, messages).
- ✔ Edit shows "edited"; delete-for-everyone leaves "This message was deleted" tombstone.

### 7.5 Media
- Images: gallery picker (Photo Picker) + CameraX capture, multi-select up to 10, caption, client compression, blurhash/thumbnail placeholder, full-screen viewer with pinch-zoom and swipe between media.
- Video: pick/record, thumbnail generation, inline preview, Media3 player (fullscreen, seek).
- Files: any type ≤ 50 MB, name/size/type icon, download with progress, open via intent.
- Voice: record (AAC/Opus), waveform capture, playback with scrubbing and 1x/1.5x/2x speed, background playback, "played" state.
- Link previews via `link-preview` function.
- Uploads: resumable via WorkManager, progress UI, cancel/retry, and attachment rows only inserted after upload success.
- ✔ Airplane-mode upload resumes when online. Cached media viewable offline.

### 7.6 Groups and channels
- Create group (name, avatar, members), group info screen (members, shared media/files/links tabs, mute, leave), add/remove, roles, invite link `pulse://join/{code}` + https app link, @mentions with autocomplete and highlight, mention notifications bypass nothing but are flagged in preview.
- Channels: only admins post; subscribers read, react; view-only input shown as "Mute/Unmute" bar.
- ✔ Non-admin cannot post to a channel (verified by RLS test, not only UI).

### 7.7 Notifications
- FCM data message → local notification built on device (MessagingStyle, per-chat grouping, avatar, inline reply `RemoteInput`, "Mark as read" action), channels (messages, groups, channels), respects mute and DND, deep link to chat, suppressed when that chat is open in foreground, cancel notifications when read on another device.
- POST_NOTIFICATIONS runtime permission flow (Android 13+).
- ✔ Receiving a message with the app killed shows a notification; reply from notification sends the message.

### 7.8 Settings and extras
- Saved Messages, contacts discovery by username search (no phone/contacts upload), archive, folders, chat wallpapers, light/dark/system theme + accent colors, text size, notification settings, storage usage + clear cache, data saver (no auto media download on mobile), language (English, Bangla), block user (client + server-enforced for directs), report (stores to `reports` table), delete account (RPC + cascade).

### 7.9 Non-functional
- Cold start < 2 s to chat list on mid-range device (add Baseline Profile).
- Scroll at 60 fps on 5k-message chats (Paging + stable keys, `contentType`).
- Crash-free, strict mode clean in debug, R8 release build works.
- Accessibility: TalkBack labels, contrast AA, font scale 200 % usable.
- Privacy: no analytics; Crashlytics only with consent toggle in settings.

## 8. Build, run and CI
- `local.properties`: `SUPABASE_URL=`, `SUPABASE_ANON_KEY=`, `GOOGLE_WEB_CLIENT_ID=`.
- Supabase CLI: `supabase start` for local dev (Docker), `supabase db push` to apply migrations to the hosted project, `supabase functions deploy`.
- GitHub Actions: `ci.yml` (assemble, unit tests, lint, ktlint on PR), `release.yml` (on tag: build signed APK/AAB using repository secrets for keystore, attach to GitHub Release).

## 9. Manual setup the human must do (document in `docs/setup.md`)
1. Create a free Supabase project; copy URL + anon key.
2. Run migrations (`supabase db push`).
3. Create a free Firebase project (Spark), add an Android app (`app.pulse`), download `google-services.json`, create a service account key for FCM v1 and store it as the Supabase secret `FCM_SERVICE_ACCOUNT_JSON`.
4. Enable Google sign-in: Google Cloud OAuth client IDs (Android + Web), add to Supabase Auth providers.
5. Create a Database Webhook: `messages` INSERT → `push-message`.
6. Create storage buckets and apply storage policies (migration `0004_storage.sql`).

## 10. Risks and mitigations
| Risk | Mitigation |
|---|---|
| Realtime limits on free tier | Single multiplexed subscription per user; Broadcast for ephemeral events |
| Project auto-pause | Daily keep-alive cron in GitHub Actions; docs |
| Storage 1 GB | Aggressive compression, thumbnails, optional per-user quota check in RPC |
| supabase-kt API drift | Pin versions in the catalog; isolate in `core:network` |
| Message loss while offline | Outbox + catch-up by `seq` + idempotent inserts |
