# prompt.md: ordered prompts for Codex

How to use: paste **one** prompt at a time into Codex, review the result, run the app, then move on. Don't skip ahead. After each prompt, commit.

Every prompt implicitly includes this preamble (also put it at the top of your first message):

> Read `AGENTS.md` and `specs.md` first. Implement only this task. Follow the architecture and free-tier constraints. Run `./gradlew assembleDebug testDebugUnitTest lintDebug ktlintCheck` until green. Update docs. **Then build an installable APK with `./scripts/build-apk.sh <pass-name>` and report its path, size and SHA-256.** End with a summary and manual test steps for that APK.

### APK rule (applies to every prompt, including fix/follow-up prompts)
At the end of every pass Codex must produce `dist/pulse-<pass-name>-debug.apk` (e.g. `dist/pulse-P4-debug.apk`) that I can install on a phone and test.
- It must be a **debug-signed, installable** APK (`assembleDebug`), built from the final committed state, after all checks are green.
- It must install over the previous pass's APK (same `applicationId` and signing key; never change `applicationId` or use a random keystore). Never increase `minSdk` after P0 without telling me.
- Features not built yet must be hidden or show a "Coming soon" state, never crash.
- The final message must include: APK path, size, SHA-256, `versionName`/`versionCode`, and a "How to test this APK" checklist (what to tap, what should happen, which accounts/devices are needed).
- Also commit nothing large: `dist/` is git-ignored; CI uploads the APK as a GitHub Actions artifact on every push so I can download it from the run page.
- If the sandbox has no Android SDK and the APK cannot be built, say so explicitly, make sure CI builds it, and give me the exact workflow run/artifact name. Never claim an APK exists when it does not.

---

## P0: Kickoff / scaffolding
```
Create the Android project "Pulse" (package app.pulse) per AGENTS.md module layout.
- Gradle Kotlin DSL + libs.versions.toml with all libraries in AGENTS.md (pin stable versions).
- Hilt, Compose Material 3 theme (light/dark/dynamic color), edge-to-edge, Navigation Compose shell with placeholder routes: auth, chatlist, chat/{id}, settings.
- ktlint or spotless, .editorconfig, .gitignore (local.properties, google-services.json, release keystores; but NOT app/debug.keystore, which is committed on purpose).
- BuildConfig fields from local.properties: SUPABASE_URL, SUPABASE_ANON_KEY, GOOGLE_WEB_CLIENT_ID.
- GitHub Actions ci.yml (assemble, unit tests, lint, ktlint) that also uploads `app-debug.apk` as an artifact named `pulse-debug-${{ github.sha }}` (actions/upload-artifact).
- `scripts/build-apk.sh <pass-name>`: runs `./gradlew assembleDebug`, copies the output to `dist/pulse-<pass-name>-debug.apk`, prints path, size, SHA-256, versionName/versionCode (via `aapt2 dump badging` or `apkanalyzer` if available). Fails loudly if the APK is missing. Add `dist/` to .gitignore.
- A fixed, committed debug keystore config (`app/debug.keystore`, standard debug passwords) so every pass's APK is signed identically and installs as an update.
- Auto-increment `versionCode` from `git rev-list --count HEAD` and set `versionName` to `0.<pass>.<build>`.
- docs/setup.md, docs/decisions.md, docs/manual-test.md, README.md skeleton.
Done when: app launches to a placeholder screen, CI passes, and `dist/pulse-P0-debug.apk` installs and opens.
```

## P1: Supabase backend
```
Implement the backend under supabase/ per specs.md sections 4-5.
- migrations: 0001_init.sql (schema), 0002_rls.sql, 0003_rpc.sql, 0004_storage.sql (buckets + policies), 0005_realtime.sql (publication).
- Edge functions: push-message (FCM HTTP v1 using service-account secret), link-preview (with SSRF guard), cleanup.
- supabase/tests/: SQL tests proving: non-member cannot select/insert messages; non-admin cannot insert into a channel; users cannot edit others' messages; direct chat dedupe works; mark_read is monotonic.
- supabase/config.toml, seed.sql with 3 demo users, a group and a channel.
- Expand docs/setup.md with exact CLI steps (supabase start, db push, functions deploy, secrets, webhook).
Done when: `supabase db reset` runs clean locally and the SQL tests pass. Also build `dist/pulse-P1-debug.apk` (app unchanged; confirm it still installs over P0).
```

## P2: Core data layer
```
Build core/model, core/database, core/network, core/data.
- Domain models, Room entities/DAOs/migrations (export schema), mappers.
- Supabase client module (Auth, Postgrest, Realtime, Storage) with Hilt.
- Repository interfaces in core:domain (or core:data api) + Supabase implementations + fakes for tests.
- SyncEngine skeleton: realtime subscription manager, catch-up by seq, SyncStateEntity.
- OutboxWorker (WorkManager) with unique work per chat, ordered sending, exponential backoff, idempotent insert.
Tests: DAOs (in-memory Room), outbox ordering/retry/duplicate, mappers.
Done when: unit tests green; build `dist/pulse-P2-debug.apk` (still shell UI; add a hidden debug screen showing DB/Supabase connectivity status so the APK proves the wiring).
```

## P3: Auth and profile
```
Implement feature/auth and feature/profile per specs 7.1.
- Email/password sign-up/sign-in, Google sign-in via Credential Manager, validation, error states, loading states.
- Session persistence, splash gating to auth vs chatlist, logout (clears Room, unregisters FCM token).
- Profile edit: display name, username availability check (debounced), avatar pick+crop+upload, bio.
Tests: ViewModels with fake AuthRepository; one Compose UI test for login.
Done when: register -> restart -> still logged in; username conflict handled. Deliver `dist/pulse-P3-debug.apk`.
```

## P4: Chat list + 1:1 chat (thin vertical slice)
```
Implement feature/chatlist, feature/search (users only), feature/chat for text-only direct messaging per 7.2 and 7.3.
- User search by username -> create_direct_chat RPC -> open chat.
- Chat list from Room (pinned first, last_message_at), unread badge, previews, timestamps.
- Chat screen: bubbles, grouping, date separators, input bar, optimistic send via outbox, states pending/sent/delivered/read (ticks), Paging 3 history, scroll position restore, drafts.
- Realtime receive, mark_delivered/mark_read, typing indicator (Broadcast), online/last seen (Presence + profiles.last_seen).
Tests: ChatViewModel (send, offline queue, ordering), chat list VM, Compose test for chat screen.
Done when: two emulators chat in realtime; airplane-mode messages deliver in order after reconnect without duplicates. Deliver `dist/pulse-P4-debug.apk` and a two-device test script in the final message.
```

## P5: Message actions
```
Implement specs 7.4 on top of the chat screen.
- Long-press menu, reply with quote + swipe-to-reply + jump to original, edit (48h), delete for me / for everyone (tombstone), copy, forward (chat picker), pin/unpin with pinned bar, multi-select.
- Reactions (quick bar + picker, chips, toggle) with realtime sync.
- In-chat search (prev/next, highlight) and global search via search_messages RPC.
Tests: VM tests for each action; RLS-backed behavior checked via fakes + documented manual steps.
Deliver `dist/pulse-P5-debug.apk`.
```

## P6: Media
```
Implement feature/media per 7.5.
- Photo Picker + CameraX capture, multi-select, compression (max 1600px, q~80), thumbnails/blurhash, caption.
- Video pick/record + thumbnail, Media3 player screen.
- File attachments (<=50MB), download with progress, open via intent.
- Voice messages: hold-to-record with slide-to-cancel and lock, waveform, playback with scrub and speed, background playback service.
- Upload pipeline in WorkManager (resumable, progress, cancel/retry); attachment row inserted only after upload; signed URL fetching with caching.
- Full-screen media viewer with pinch-zoom + swipe; link previews via the link-preview function.
Tests: upload worker (retry/cancel), compression util, voice recorder state machine.
Deliver `dist/pulse-P6-debug.apk` (declare camera/mic/media permissions correctly; test on a real phone).
```

## P7: Groups and channels
```
Implement feature/groups per 7.6.
- Create group/channel flow, group info screen (members, media/files/links tabs, mute, leave), add/remove members, roles, invite link (deep link pulse://join/{code} and https app link), join_by_invite.
- @mentions autocomplete + highlighting; channel read-only input for non-admins.
- Member list with online status; admin tools (promote/demote/remove).
Tests: VMs; confirm server-side enforcement via supabase/tests additions.
Deliver `dist/pulse-P7-debug.apk`.
```

## P8: Push notifications
```
Implement specs 7.7.
- FirebaseMessagingService: token registration/refresh into devices table; handle data messages.
- Notification channels, MessagingStyle with grouping per chat, avatar, inline reply (RemoteInput -> outbox), mark-as-read action, deep link to chat, suppress when chat open, cancel when read elsewhere.
- Per-chat and global mute respecting muted_until; Android 13 permission flow; battery-optimization hint in settings.
- Verify push-message function end to end; document in docs/manual-test.md.
- Deliver `dist/pulse-P8-debug.apk`. It must include the FCM config; if `google-services.json` is missing, the build must still succeed (push disabled gracefully) and say so.
```

## P9: Settings and extras
```
Implement feature/settings and extras per 7.8.
- Saved Messages, archive, folders/filters, chat wallpapers, theme + accent, text size, notification settings, storage usage + clear cache, data saver, language (en + bn translations for all strings), block user, report, delete account (RPC), Crashlytics consent toggle.
- Emoji picker polish, drafts, link auto-detect.
- Deliver `dist/pulse-P9-debug.apk`.
```

## P10: Hardening and release
```
- Baseline Profile + startup/scroll benchmarks; fix jank on 5k-message chat.
- Accessibility pass (TalkBack, contrast, 200% font).
- R8/ProGuard rules, test release build.
- Audit: RLS tests complete, no secrets in repo, no PII in logs, upload size/type validation.
- release.yml: signed APK/AAB on tag, attach to GitHub Release (keystore via secrets).
- Keep-alive workflow (cron) to ping Supabase and prevent free-tier pause.
- Final docs: README with screenshots placeholders, setup.md, architecture diagram (mermaid), known limitations.
- Deliver both `dist/pulse-P10-debug.apk` (for testing) and a release-signed `dist/pulse-P10-release.apk` (R8 enabled, signed with the keystore from env vars/secrets; if no keystore is available, say so and skip).
```

## P11 (optional): Calls
```
Add 1:1 voice/video calls with WebRTC. Signaling via Supabase Realtime Broadcast, public STUN only (document that calls may fail behind symmetric NAT). Incoming-call full-screen notification via FCM high priority. Keep behind a feature flag. Deliver `dist/pulse-P11-debug.apk`.
```

---

## Debug / follow-up prompt templates

**Fix a bug**
```
Bug: <what happens> | Expected: <what should happen> | Steps: <1,2,3>
Find the root cause, add a failing test first, fix it, run the full check, then build `dist/pulse-fix-<short-description>-debug.apk`.
```

**Review before merge**
```
Review the diff of the last task against AGENTS.md and specs.md. List violations (architecture, offline-first, security, free-tier limits, missing tests) and fix them. Rebuild the APK as `dist/pulse-<pass>-review-debug.apk`.
```

**Schema change**
```
Add <change>. Create a new migration file (do not edit applied ones), update RLS and Room entities + Room migration, update specs.md schema section, add tests. Verify the APK upgrades from the previous one without data loss, then build `dist/pulse-<name>-debug.apk`.
```

---

## Tips
- Always test with two emulators or an emulator + a real phone logged in as different users.
- If Codex drifts, re-paste the preamble and point at the exact spec section.
- Commit after each green prompt so you can roll back cheaply.
- Install the new APK over the old one (`adb install -r dist/pulse-P4-debug.apk`) to also test upgrades. If Codex can't build locally, download the APK from the GitHub Actions run artifacts.
- Human-only steps (Supabase project, Firebase project, OAuth IDs, FCM service account) are listed in `specs.md` section 9; Codex cannot do these for you.
