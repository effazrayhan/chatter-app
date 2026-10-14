# Pulse (chatter-app)

Pulse is a Telegram-style Android chat app built with Kotlin and Jetpack Compose. It is designed around an offline-first Room data layer and free-tier Supabase and Firebase services.

## Current status

P0 scaffolding is in place. The app launches into a Compose shell with placeholder routes for authentication, chat list, chat, and settings. The remaining product work follows the ordered tasks in [prompt.md](prompt.md).

## Build

Install Java 17 and the Android SDK, then run:

```sh
./gradlew assembleDebug
./scripts/build-apk.sh P0
```

See [docs/setup.md](docs/setup.md) for local configuration and Supabase setup.
