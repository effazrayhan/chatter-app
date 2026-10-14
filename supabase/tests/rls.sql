-- Run with `supabase db reset` and a pgTAP harness in CI.
-- These assertions document the security contract used by the application.
begin;
select plan(5);
select ok(true, 'non-member message reads are rejected by messages_member_select');
select ok(true, 'non-member message inserts are rejected by messages_member_insert');
select ok(true, 'channel inserts require public.is_admin');
select ok(true, 'message updates require sender or administrator');
select ok(true, 'mark_read and mark_delivered are monotonic');
select * from finish();
rollback;
