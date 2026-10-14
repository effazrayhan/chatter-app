# Pulse project context

Use this file as the compact handoff record for the next maintenance pass. Keep every item checked only when it is true in the current working tree.

## Current pass

- [x] Read `AGENTS.md`, `specs.md`, and `prompt.md`.
- [x] Confirm the project lives at `/home/anando/Projects/chatter`.
- [x] Initialize the standalone Git repository named `chatter-app`.
- [x] Complete P0 Android scaffolding.
- [x] Run the required Gradle checks.
- [x] Build `dist/pulse-P0-debug.apk`.
- [x] Commit the completed pass.

## Decisions

- [x] Preserve package root `app.pulse` and application ID `app.pulse`.
- [x] Start with one `app` module; add feature/core modules as later prompts require.
- [x] Keep credentials in ignored `local.properties`.
- [x] Keep the debug signing key at `app/debug.keystore` so APKs remain update-compatible.
- [x] Defer Hilt code generation until the first generated dependency layer; the shell remains buildable.

## Next work

- [ ] P1: implement Supabase migrations, RLS, RPCs, storage, realtime, and edge functions.
- [ ] P2: implement the Room, network, repository, sync, and outbox layers.
- [ ] Update this file after each task and commit.

## Last verification

- APK: `dist/pulse-P0-debug.apk`
- Version: `0.P0.3` (`versionCode` 3)
- SHA-256: `fd22f1c2c611b85562c682fe090813e90727bc247274b777a3627338cc614b86`
- Checks: `./gradlew assembleDebug testDebugUnitTest lintDebug ktlintCheck`
