---
name: android-architecture
description: Applies the Toko Kasir Android architecture rules for Compose, MVVM, Clean Architecture, Room, repositories, dependency injection, navigation, and schema migrations. Use when designing or modifying Android layers, data flow, dependencies, or database structure.
---
# Android Architecture Skill

## Reference

Read `AGENTS.md` and `docs/ARCHITECTURE.md` before making architectural changes.

## Layer boundaries

Preferred flow:

```text
Compose UI
  -> ViewModel
  -> Use Case
  -> Repository interface
  -> Repository implementation
  -> Local/Remote data source
```

Rules:
- ViewModel does not call DAO directly.
- UI does not implement business rules.
- Use cases own business validation and authorization.
- Repository abstracts data-source details.
- Keep domain logic testable without Compose where possible.

## Room rules

- Room is the local operational source of truth.
- Schema changes require explicit migrations or a documented migration strategy.
- Never change historical transactions by joining them to mutable master data at read time.
- Preserve snapshot fields needed for historical correctness.
- Stock is derived from `StockMovement`, not a manually edited stock field.

## Money and quantities

- Money: `Long` integer Rupiah.
- Unit quantity: `Long` count.
- Weight quantity: `Long` grams.
- No `Float`/`Double` for business calculations involving money or persisted weight.

## IDs and state

- Use stable IDs for local/remote synchronization.
- Do not rely on list position or timestamps alone for identity.
- Persistent draft-cart state belongs in Room.
- UI state should expose immutable, explicit state models.

## Dependencies

Do not introduce a new library when platform/Jetpack functionality already solves the problem adequately.

For version choices, verify current official Android/Google documentation before adding or upgrading dependencies.

## Architecture change checklist

Before finalizing an architecture change, verify:
- dependency direction remains valid
- no DAO leaks into UI
- migrations are handled
- tests cover the changed rule/data path
- no unnecessary module/library complexity was introduced
- sync can still use the domain/repository abstraction
