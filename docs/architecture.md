# Pulse architecture

```mermaid
flowchart TD
    UI[Compose UI] --> VM[ViewModel]
    VM --> Repo[Repository interface]
    Repo --> Room[Room cache]
    Repo --> Remote[Supabase remote]
    Remote --> Sync[Realtime and catch-up sync]
    Sync --> Room
    Repo --> Outbox[WorkManager outbox]
    Outbox --> Remote
```

The current single application module contains package boundaries for core model, database, network, data, and feature code. They can become Gradle modules as the app grows without changing the public package root.
