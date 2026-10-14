alter table public.profiles enable row level security;
alter table public.devices enable row level security;
alter table public.chats enable row level security;
alter table public.chat_members enable row level security;
alter table public.messages enable row level security;
alter table public.attachments enable row level security;
alter table public.reactions enable row level security;
alter table public.chat_pins enable row level security;
alter table public.hidden_messages enable row level security;

create policy profiles_select on public.profiles for select to authenticated using (true);
create policy profiles_update on public.profiles for update to authenticated using (id = auth.uid()) with check (id = auth.uid());

create policy devices_owner on public.devices for all to authenticated
using (user_id = auth.uid()) with check (user_id = auth.uid());

create policy chats_member_or_public on public.chats for select to authenticated
using (public.is_member(id) or username is not null);
create policy chats_admin_update on public.chats for update to authenticated
using (public.is_admin(id)) with check (public.is_admin(id));

create policy members_select on public.chat_members for select to authenticated
using (public.is_member(chat_id));
create policy members_self_update on public.chat_members for update to authenticated
using (user_id = auth.uid()) with check (user_id = auth.uid());

create policy messages_member_select on public.messages for select to authenticated
using (public.is_member(chat_id));
create policy messages_member_insert on public.messages for insert to authenticated
with check (
  sender_id = auth.uid() and (
    (public.is_member(chat_id) and (select type from public.chats where id = chat_id) <> 'channel')
    or public.is_admin(chat_id)
  )
);
create policy messages_owner_update on public.messages for update to authenticated
using (sender_id = auth.uid() or public.is_admin(chat_id))
with check (sender_id = auth.uid() or public.is_admin(chat_id));

create policy attachments_member on public.attachments for all to authenticated
using (exists (select 1 from public.messages m where m.id = message_id and public.is_member(m.chat_id)))
with check (exists (select 1 from public.messages m where m.id = message_id and public.is_member(m.chat_id)));
create policy reactions_member on public.reactions for select to authenticated
using (exists (select 1 from public.messages m where m.id = message_id and public.is_member(m.chat_id)));
create policy reactions_owner on public.reactions for insert to authenticated
with check (user_id = auth.uid() and exists (select 1 from public.messages m where m.id = message_id and public.is_member(m.chat_id)));
create policy reactions_owner_delete on public.reactions for delete to authenticated using (user_id = auth.uid());

create policy pins_member_select on public.chat_pins for select to authenticated using (public.is_member(chat_id));
create policy pins_admin_write on public.chat_pins for all to authenticated using (public.is_admin(chat_id)) with check (public.is_admin(chat_id));
create policy hidden_owner on public.hidden_messages for all to authenticated using (user_id = auth.uid()) with check (user_id = auth.uid());
