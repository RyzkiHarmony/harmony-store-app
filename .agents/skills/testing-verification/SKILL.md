---
name: testing-verification
description: Verifies Toko Kasir changes through targeted unit, persistence, integration, UI, build, and lint checks, with emphasis on business-critical invariants and honest reporting of what was actually tested. Use after implementing or modifying functionality.
---
# Testing & Verification Skill

## Reference

Read `AGENTS.md` and the relevant feature documentation before defining verification scope.

## Verification order

1. Compile/build affected module.
2. Run focused unit tests for changed business rules.
3. Run Room/DAO/integration tests when persistence changes.
4. Run UI tests when navigation or interaction behavior changes.
5. Run lint/static analysis if configured.
6. Run broader test suite when practical.
7. Inspect final diff.

Use the repository's existing Gradle tasks and conventions rather than inventing commands.

## Business invariants to verify

### Money
- all business money values are integral
- change calculation is exact
- insufficient cash is rejected

### Cart
- draft cart persists
- duplicate scan aggregates as specified
- price snapshot is stable

### Inventory
- stock comes from movement ledger
- sale reduces stock
- reversal restores stock
- adjustment difference is `physical - system`
- negative stock is rejected
- digital items do not alter physical stock

### Transactions
- completion is atomic
- completed records are retained
- cancellation creates reversal movement
- QRIS requires explicit confirmation

### Authorization
- cashier cannot invoke admin-only price/stock/cancellation operations
- authorization is enforced below the UI layer

### Sync
- local completion survives remote failure
- retry uses stable IDs
- duplicate sync attempts do not duplicate records

## Failure analysis

When a test fails:
- distinguish product defect from test/setup/environment defect
- inspect stack traces and affected code
- fix root cause rather than weakening the test
- do not delete a test because it exposes a legitimate business invariant

## Reporting

State exactly:
- commands/tests executed
- passed/failed
- known environment limitations
- changes made after failures

Never claim "all tests pass" unless the relevant tests were actually run.
