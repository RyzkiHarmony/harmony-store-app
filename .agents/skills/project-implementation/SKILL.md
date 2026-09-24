---
name: project-implementation
description: Guides implementation of a scoped feature in the Toko Kasir Android POS project using the repository PRD, business rules, architecture, UX specification, and implementation plan. Use when adding a feature, changing behavior, or starting a development milestone.
---
# Project Implementation Skill

## Purpose

Implement one requested milestone or feature without expanding scope unnecessarily.

## Required context

Read:
1. `AGENTS.md`
2. `docs/BUSINESS_RULES.md`
3. `docs/ARCHITECTURE.md`
4. the relevant section of `docs/PRD.md`
5. the relevant section of `docs/UX_SPEC.md`
6. `docs/IMPLEMENTATION_PLAN.md` only for milestone sequencing

Inspect current code before editing.

## Procedure

### 1. Define the change

Identify:
- user-visible outcome
- affected use cases
- affected entities/DAO/repository
- affected screens/ViewModels
- required migration
- required tests

Do not implement unrelated roadmap items.

### 2. Check consistency

Before coding, verify:
- business rules are not violated
- current architecture can support the change
- existing behavior that should remain unchanged is understood
- no document contains a conflicting requirement

If a material conflict exists, stop before changing business logic and report it.

### 3. Implement in dependency order

Prefer:
1. domain models/rules
2. repository contracts
3. local persistence
4. data/repository implementation
5. use case
6. ViewModel/UI state
7. UI/navigation
8. sync integration when required

Keep pure business logic independent of Android UI where practical.

### 4. Verify

Run the narrowest useful checks first, then broader checks:
- compile/build
- unit tests
- DAO/integration tests
- relevant UI tests
- lint/static analysis if configured

Inspect the final diff for accidental edits.

## Output contract

At the end of the task, summarize:
- implemented behavior
- files/layers changed
- tests/checks run
- migration changes
- remaining risks or assumptions

Do not claim verification that was not actually performed.
