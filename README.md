# Pulse (chatter-app)

Pulse is a Telegram-style Android chat app built with Kotlin and Jetpack Compose. It is designed around an offline-first Room data layer and free-tier Supabase and Firebase services.

## Current status

P4 is implemented as a local-first vertical slice: auth/profile forms, cached chat list, message bubbles, optimistic outbox enqueue, and diagnostics. Supabase migrations and edge functions are included under `supabase/`; connect a real Supabase project before testing cross-device networking. Continue with the ordered tasks in [prompt.md](prompt.md).

## Build

Install Java 17 and the Android SDK, then run:

```sh
./gradlew assembleDebug
./scripts/build-apk.sh P0
```

See [docs/setup.md](docs/setup.md) for local configuration and Supabase setup.
