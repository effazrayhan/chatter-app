# Pulse (chatter-app)

Pulse is a Telegram-style Android chat app built with Kotlin, Jetpack Compose, Room-shaped offline storage, Supabase, and Firebase Cloud Messaging. It targets Android 8.0+ and uses only free-tier services.

## Quick start

Install Java 17, Android SDK platform 35, and build tools 35.0.0. Clone the repository, then create `local.properties` in the repository root:

```properties
sdk.dir=/absolute/path/to/Android/Sdk
SUPABASE_URL=https://your-project.supabase.co
SUPABASE_ANON_KEY=your-public-anon-key
GOOGLE_WEB_CLIENT_ID=your-web-client-id
```

Build and verify the app:

```sh
./gradlew assembleDebug testDebugUnitTest lintDebug ktlintCheck
./scripts/build-apk.sh P10
adb install -r dist/pulse-P10-debug.apk
```

The APK is signed with the committed `app/debug.keystore`, so each pass installs as an update. `local.properties`, Firebase configuration, release keys, and generated build output remain local.

## Supabase setup

Install the Supabase CLI and Docker, then run:

```sh
supabase start
supabase db reset
supabase functions serve
supabase link --project-ref YOUR_PROJECT_REF
supabase db push
supabase secrets set FCM_SERVICE_ACCOUNT_JSON='...'
supabase functions deploy push-message --no-verify-jwt
supabase functions deploy link-preview
supabase functions deploy cleanup --no-verify-jwt
```

Create a Database Webhook for `public.messages` INSERT to the deployed `push-message` function. Create a Firebase Android app with application ID `app.pulse`, download `google-services.json` into `app/`, and configure Google sign-in in Supabase Auth. Supabase free projects pause after inactivity; resume them from the dashboard when needed.

## Project layout

- `app/`: Android application, Compose navigation, features, and push service.
- `supabase/`: migrations, RLS, RPCs, storage policies, realtime publication, and edge functions.
- `docs/`: setup, architecture, decisions, and manual test steps.
- `CONTEXT.md`: checkboxed maintenance handoff and next-pass state.
- `dist/`: debug APKs for each pass, intentionally committed for direct GitHub downloads.

## Passes

P0–P4 provide the app shell, backend foundation, auth/profile forms, cached chat list, and local-first direct chat. P5–P10 add action/media/group/push/settings foundations and release workflows. Supabase credentials and Firebase service configuration are required for real multi-device networking and notifications.
