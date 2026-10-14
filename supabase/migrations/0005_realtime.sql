begin;
  alter publication supabase_realtime add table public.messages;
  alter publication supabase_realtime add table public.reactions;
  alter publication supabase_realtime add table public.chat_members;
  alter publication supabase_realtime add table public.chats;
  alter publication supabase_realtime add table public.chat_pins;
  alter publication supabase_realtime add table public.attachments;
commit;
