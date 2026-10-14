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
- [x] Push `master` to `origin` (`effazrayhan/chatter-app`).

## Decisions

- [x] Preserve package root `app.pulse` and application ID `app.pulse`.
- [x] Start with one `app` module; add feature/core modules as later prompts require.
- [x] Keep credentials in ignored `local.properties`.
- [x] Keep the debug signing key at `app/debug.keystore` so APKs remain update-compatible.
- [x] Defer Hilt code generation until the first generated dependency layer; the shell remains buildable.

## Next work

- [x] P1: implement Supabase migrations, RLS, RPCs, storage, realtime, and edge functions.
- [x] P2: implement the Room, network, repository, sync, and outbox layers.
- [x] P3: implement auth and profile.
- [x] P4: implement the direct chat vertical slice.
- [x] P5: implement message actions and reactions.
- [x] P6: implement media and upload foundations.
- [x] P7: implement groups and channels foundations.
- [x] P8: implement push notification foundations.
- [x] P9: implement settings and extras foundations.
- [x] P10: harden, document, and prepare release workflows.
- [ ] Update this file after each task and commit.

## Last verification

- The latest committed pass is verified with `./gradlew assembleDebug testDebugUnitTest lintDebug ktlintCheck` and `./scripts/build-apk.sh P0`.
- Record exact APK metadata in the maintenance handoff or final report for each build.
