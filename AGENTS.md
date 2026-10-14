# AGENTS.md

Instructions for Codex (and any coding agent) working in this repository.
Read this file, then `specs.md`, before every task.

## Project
**Pulse**: a Telegram-style Android chat app. Native Kotlin, Jetpack Compose, offline-first.
Backend is **Supabase (free tier)**; push via **Firebase Cloud Messaging (free)**. **Everything must run on free tiers. Never add a paid service or a feature that requires a billing account.**

## Source of truth
- `specs.md`: stack, architecture, data model, features, acceptance criteria. Follow it exactly.
- `prompt.md`: ordered task prompts. Work on one task at a time, only the one you were given.
- If the spec is ambiguous or conflicts with reality (e.g. a library API changed), choose the simplest option that keeps the architecture intact, note the decision in `docs/decisions.md`, and continue. Do not stop to ask unless blocked.

## Stack (fixed, do not swap)
Kotlin 2.x, Jetpack Compose + Material 3, Navigation Compose, Hilt, Coroutines/Flow, Room, Paging 3, DataStore, WorkManager, Coil, CameraX, Media3, supabase-kt (auth, postgrest, realtime, storage) with the Ktor Android engine, Firebase Messaging + Crashlytics only (no other Firebase products), JUnit5/JUnit4, MockK, Turbine, Compose UI tests. Gradle Kotlin DSL + `gradle/libs.versions.toml`. minSdk 26, target/compile latest stable.

Adding a dependency requires: free + open license, justification in `docs/decisions.md`.

## Architecture rules
1. **Offline-first.** UI reads only from Room via repositories. Network code writes into Room. ViewModels never call Supabase directly.
2. **Layers per feature:** `ui` (Composables + ViewModel + UiState/UiEvent) → `domain` (use cases, only when logic is non-trivial) → `data` (repository impl, Room DAO, remote data source).
3. **Backend behind interfaces.** `AuthRepository`, `ChatRepository`, `MessageRepository`, `MediaRepository`, `PresenceRepository`, `PushRepository` are interfaces in `core:domain`; Supabase implementations live in `core:data`. Provide fakes for tests.
4. **Unidirectional data flow.** `StateFlow<UiState>` out, `onEvent(UiEvent)` in. One-off effects via `Channel`/`SharedFlow`.
5. **Client-generated UUIDs** for messages (idempotent sends via the outbox).
6. **Server time is truth** for ordering (`seq`), local time only for the optimistic display.
7. No business logic in Composables. No `GlobalScope`. No blocking calls on main. No `!!` outside tests.

## Module / package layout
```
app/                      # Application, MainActivity, nav graph, FCM service
core/common               # utils, Result wrappers, dispatchers
core/model                # pure Kotlin domain models
core/database             # Room entities, DAOs, migrations
core/network              # Supabase client, DTOs, mappers
core/data                 # repository implementations, sync, outbox workers
core/ui                   # theme, design system, shared composables
feature/auth  feature/chatlist  feature/chat  feature/media
feature/groups  feature/profile  feature/settings  feature/search
supabase/                 # migrations/, functions/, seed.sql, config.toml
docs/                     # decisions.md, manual-test.md, setup.md
```
Package root: `app.pulse`. Start with fewer modules if setup cost is high, but keep package boundaries as above.

## Workflow for every task
1. Read `specs.md` sections relevant to the task.
2. Plan briefly (files to touch), then implement in small commits.
3. Write tests alongside code (see Testing).
4. Run and make green, in this order:
   ```
   ./gradlew assembleDebug testDebugUnitTest lintDebug ktlintCheck
   ```
   (Add `ktlint`/`spotless` in Phase 0.) Fix all failures. Do not disable tests or lint rules to pass.
5. **Build the test APK (mandatory every pass):** run `./scripts/build-apk.sh <pass-name>`, which produces `dist/pulse-<pass-name>-debug.apk`. The APK must be debug-signed with the committed debug keystore (so it installs over earlier passes), built from the final committed state, and must not crash on unfinished features (hide them or show "Coming soon"). Report path, size, SHA-256, versionName/versionCode and a "How to test this APK" checklist in the final summary. If no Android SDK is available, say so honestly and rely on the CI artifact; never claim an APK exists when it doesn't.
6. If you changed SQL, add a new numbered file in `supabase/migrations/` (never edit an applied migration) and update `specs.md` schema section.
7. Update `README.md`/`docs/setup.md` if setup steps changed; append manual test steps to `docs/manual-test.md`.
8. Finish with a summary: what changed, how to verify (APK test checklist), known gaps.

## Testing requirements
- Every ViewModel: unit tests with fakes + Turbine.
- Every repository: tests against in-memory Room + fake remote.
- Outbox/sync logic: tests for offline, retry, ordering, duplicate delivery.
- Key screens: at least one Compose UI test (chat list, chat screen, login).
- RLS policies: SQL tests in `supabase/tests/` (pgTAP or plain SQL scripts) proving a non-member cannot read/write a chat.

## Security
- Never commit secrets. Use `local.properties` (git-ignored) for `SUPABASE_URL` and `SUPABASE_ANON_KEY`; expose via `BuildConfig`. The anon key is public by design; the **service role key must never appear in the app or the repo**.
- `google-services.json` is git-ignored; commit `google-services.json.example`.
- Every table has RLS enabled with explicit policies. Storage buckets are private; use signed URLs.
- Validate and sanitize all user input; limit message length (4096) and upload sizes (50 MB).
- Do not log message bodies or tokens.

## Free-tier constraints to respect
- Supabase free: 500 MB DB, 1 GB storage, 50 MB max file size, 2 concurrent-ish realtime limits (200 concurrent connections), project pauses after 1 week of inactivity. Compress images (max 1600 px, JPEG/WebP q≈80) and voice (Opus/AAC) before upload; generate thumbnails client-side.
- Do not poll. Use Realtime subscriptions and push.
- No SMS auth. Auth is email+password and Google sign-in.
- Calls (if built) use WebRTC with public STUN only; document that TURN is absent.

## Code style
- Official Kotlin style, ktlint/spotless enforced. Small functions, descriptive names, KDoc on public APIs.
- Immutable models, `sealed interface` for states/events.
- Strings in `strings.xml` (English + Bangla `values-bn`). No hardcoded UI text.
- Accessibility: content descriptions, 48dp touch targets, dynamic font scaling.

## Definition of done (all tasks)
Builds, tests pass, lint clean, no new warnings of note, works offline where applicable, docs updated, **installable debug APK produced in `dist/`**, summary written.

## Do not
- Do not rewrite unrelated code, rename modules, or reformat the repo.
- Do not use deprecated APIs when a stable replacement exists.
- Do not add analytics/ads/tracking.
- Do not change `applicationId` or the debug keystore after P0 (breaks update-installs).
- Do not finish a task without producing the APK.
- Do not implement features outside the current task.
