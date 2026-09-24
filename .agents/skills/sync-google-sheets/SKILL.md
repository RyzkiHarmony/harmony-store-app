---
name: sync-google-sheets
description: Designs and implements local-first synchronization between the Android Room database and Google Apps Script/Google Sheets, including SyncQueue, retries, idempotency, batching, bootstrap/recovery, and sync status. Use when modifying remote sync or data recovery behavior.
---
# Sync & Google Sheets Skill

## Reference

Read:
- `AGENTS.md`
- `docs/ARCHITECTURE.md`
- relevant `docs/PRD.md`
- relevant `docs/IMPLEMENTATION_PLAN.md`

## Architecture

```text
Android Room
   |
SyncQueue
   |
WorkManager
   |
Apps Script gateway
   |
Google Sheets
```

Recovery/bootstrap is the reverse direction under explicit application control:

```text
Google Sheets
   -> Apps Script
   -> Android local DB
```

Room remains the operational source of truth for cashier execution.

## Sync invariants

- Local transaction completion does not depend on remote sync success.
- A sync failure marks work as pending/failed and remains retryable.
- Retry uses the same stable entity IDs.
- Remote writes must be idempotent.
- Prefer batch sync requests.
- Sync logic must distinguish create/update/cancel semantics.
- Do not duplicate physical stock movements during retries.

## SyncQueue

Track at least:
- queue ID
- entity type
- entity ID
- operation
- payload
- status
- retry count
- last error
- created time
- synced time

Typical status:
- `PENDING`
- `SYNCING`
- `SYNCED`
- `FAILED`

Do not remove failed work merely because a request failed.

## WorkManager

Use WorkManager for background sync.
Prefer network-connected constraints.
Return retry for transient failures.
Do not retry indefinitely without backoff/limits defined by implementation.

## Apps Script boundary

Apps Script is a lightweight transport/gateway layer.
Do not move core business rules into Apps Script when they can remain in Android/domain logic.

Remote data should preserve IDs and timestamps required for reconstruction/audit.

## Bootstrap/recovery

A restore process must not blindly overwrite local state.
Before implementing recovery, define:
- what data is authoritative
- conflict rules
- duplicate detection
- ordering/version semantics
- transaction integrity expectations

When the existing project does not define these details, stop at the safest implementation point and report the missing rule rather than inventing destructive merge behavior.

## Testing focus

Test:
- retry behavior
- duplicate request handling
- stable IDs
- batch payload validation
- queue state transitions
- sync failure resilience
- transaction remains locally completed while sync is pending
- recovery/restore guards
