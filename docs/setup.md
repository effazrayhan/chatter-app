# Development setup

## Android

1. Install Android Studio with an Android SDK containing API 35 and build tools.
2. Install Java 17 and set `JAVA_HOME`.
3. Copy the required local configuration into `local.properties`:

```properties
sdk.dir=/path/to/Android/Sdk
SUPABASE_URL=
SUPABASE_ANON_KEY=
GOOGLE_WEB_CLIENT_ID=
```

The values are exposed to the app through `BuildConfig`; secrets are ignored by Git.

## Firebase

Firebase setup is added in the notification task. Keep `google-services.json` local; it is intentionally ignored.

## Supabase

Install the Supabase CLI, then run the local stack and migrations:

```sh
supabase start
supabase db reset
supabase functions serve
supabase secrets set FCM_SERVICE_ACCOUNT_JSON='...'
supabase functions deploy push-message --no-verify-jwt
supabase functions deploy link-preview
supabase functions deploy cleanup --no-verify-jwt
```

For a hosted project, link it with `supabase link --project-ref <ref>` and use `supabase db push`. Configure a Database Webhook for `messages` INSERT to `push-message`. Create the Firebase service account secret in Supabase; never commit it.

Free Supabase projects pause after inactivity. Open the project dashboard and resume it before running the app.

## Checks

Run the project checks from the repository root:

```sh
./gradlew assembleDebug testDebugUnitTest lintDebug ktlintCheck
```

Build an installable debug APK with `./scripts/build-apk.sh P0`.
