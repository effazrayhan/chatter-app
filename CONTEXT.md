# Pulse project context

Use this file as the compact handoff record for the next maintenance pass. Keep every item checked only when it is true in the current working tree.

## Current pass

- [x] Read `AGENTS.md`, `specs.md`, and `prompt.md`.
- [x] Confirm the project lives at `/home/anando/Projects/chatter`.
- [ ] Initialize the standalone Git repository named `chatter-app`.
- [ ] Complete P0 Android scaffolding.
- [ ] Run the required Gradle checks.
- [ ] Build `dist/pulse-P0-debug.apk`.
- [ ] Commit the completed pass.

## Decisions

- [x] Preserve package root `app.pulse` and application ID `app.pulse`.
- [x] Start with one `app` module; add feature/core modules as later prompts require.
- [x] Keep credentials in ignored `local.properties`.

## Next work

- [ ] P1: implement Supabase migrations, RLS, RPCs, storage, realtime, and edge functions.
- [ ] P2: implement the Room, network, repository, sync, and outbox layers.
- [ ] Update this file after each task and commit.
