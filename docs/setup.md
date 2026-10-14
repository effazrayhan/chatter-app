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

The Supabase migrations and functions will be added in P1. Free projects pause after a period of inactivity; when that happens, open the project dashboard and resume it before running the app.

## Checks

Run the project checks from the repository root:

```sh
./gradlew assembleDebug testDebugUnitTest lintDebug ktlintCheck
```

Build an installable debug APK with `./scripts/build-apk.sh P0`.
