# Pulse (chatter-app)

Pulse is a Telegram-style Android chat app built with Kotlin, Jetpack Compose, Room-shaped offline storage, Supabase, and Firebase Cloud Messaging. It targets Android 8.0+ and uses only free-tier services.

## Quick start without Docker

Pulse can be built and opened directly in Android Studio. Docker and a local Supabase server are optional and are not needed for the Android app or the committed debug APKs.

Install Android Studio, Java 17, Android SDK platform 35, and build tools 35.0.0. Clone the repository, then create `local.properties` in the repository root:

```properties
sdk.dir=/absolute/path/to/Android/Sdk
SUPABASE_URL=https://your-project.supabase.co
SUPABASE_ANON_KEY=your-public-anon-key
GOOGLE_WEB_CLIENT_ID=your-web-client-id
```

Build and verify the app:

```sh
./gradlew assembleDebug
./scripts/build-apk.sh P10
adb install -r dist/pulse-P10-debug.apk
```

The APK is signed with the committed `app/debug.keystore`, so each pass installs as an update. `local.properties`, Firebase configuration, release keys, and generated build output remain local.

## Use the app directly on Android

1. Open the repository folder in Android Studio.
2. Let Gradle sync finish. If Android Studio asks for an SDK path, select your Android SDK directory.
3. Create or edit `local.properties` with the values shown above. Leave Supabase values empty to explore the local shell and offline chat UI.
4. Start an Android 8.0+ emulator or connect a phone with USB debugging enabled.
5. Select the `app` run configuration and press Run. The app ID is `app.pulse`.
6. From the welcome screen, open Auth, Chats, Diagnostics, Profile, and the sample chat. Messages use the local cache/outbox foundation in this build.

To install a downloaded APK directly on a phone, enable installation from the source you used to download it, then run:

```sh
adb install -r dist/pulse-P10-debug.apk
```

The committed debug keystore keeps every pass update-compatible. You can also tap the APK in the phone's Downloads app.

## Hosted Supabase setup without Docker

Create a free Supabase project from the Supabase dashboard. The hosted project is enough; do not run `supabase start`.

Install the Supabase CLI only if you want to apply migrations and deploy edge functions from your machine, then run:

```sh
supabase link --project-ref YOUR_PROJECT_REF
supabase db push
supabase secrets set FCM_SERVICE_ACCOUNT_JSON='...'
supabase functions deploy push-message --no-verify-jwt
supabase functions deploy link-preview
supabase functions deploy cleanup --no-verify-jwt
```

Create a Database Webhook for `public.messages` INSERT to the deployed `push-message` function. Create a Firebase Android app with application ID `app.pulse`, download `google-services.json` into `app/`, and configure Google sign-in in Supabase Auth. Supabase free projects pause after inactivity; resume them from the dashboard when needed.

If you do not want to install the Supabase CLI either, apply the SQL files in `supabase/migrations/` through the Supabase SQL Editor in numeric order. Deploy edge functions from the dashboard or leave push/link previews disabled while using the local app shell.

## Project layout

- `app/`: Android application, Compose navigation, features, and push service.
- `supabase/`: migrations, RLS, RPCs, storage policies, realtime publication, and edge functions.
- `docs/`: setup, architecture, decisions, and manual test steps.
- `CONTEXT.md`: checkboxed maintenance handoff and next-pass state.
- `dist/`: debug APKs for each pass, intentionally committed for direct GitHub downloads.
- `ui/`: generated visual references for the chat list, chat screen, and profile/settings experiences.

## Passes

P0–P4 provide the app shell, backend foundation, auth/profile forms, cached chat list, and local-first direct chat. P5–P10 add action/media/group/push/settings foundations and release workflows. Supabase credentials and Firebase service configuration are required for real multi-device networking and notifications.
