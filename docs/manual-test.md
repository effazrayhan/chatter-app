# Manual test log

## P0 APK

1. Install `dist/pulse-P0-debug.apk` on an Android 8.0+ device or emulator.
2. Launch Pulse and confirm the welcome screen opens.
3. Tap each navigation button and confirm the auth, chat list, chat, and settings placeholders render.
4. Use the system back button and confirm navigation returns to the previous route.

## P1 backend

1. Run `supabase start` and `supabase db reset`.
2. Confirm all five migrations apply without errors.
3. Verify the SQL security tests run in CI against the local database.
4. Call `link-preview` with a public URL and confirm private hostnames are rejected.

## P4 direct chat

1. Install `dist/pulse-P4-debug.apk` on two Android 8.0+ devices.
2. Open Chats, enter the sample chat, type a message, and tap Send.
3. Confirm the message appears immediately from the local Room-shaped cache.
4. Turn on airplane mode, send another message, turn connectivity back on, and confirm the outbox path can retry without duplicating the message.

## P5 actions

1. Exercise reply, edit, delete-for-everyone, reaction toggle, and in-memory message search through the chat action layer.

## P6 media

1. Exercise the media queue validation with a file under 50 MB and a file over 50 MB.
2. Confirm oversized uploads are rejected before any network request.
